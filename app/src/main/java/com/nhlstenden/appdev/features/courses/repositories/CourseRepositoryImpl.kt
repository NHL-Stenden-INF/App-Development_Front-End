package com.nhlstenden.appdev.features.courses.repositories

import android.app.Application
import com.nhlstenden.appdev.core.models.User
import com.nhlstenden.appdev.features.courses.model.Task
import com.nhlstenden.appdev.features.course.utils.CourseParser
import com.nhlstenden.appdev.features.course.repositories.CourseRepository
import com.nhlstenden.appdev.features.course.utils.TaskParser
import com.nhlstenden.appdev.features.courses.model.Course
import com.nhlstenden.appdev.features.course.utils.QuestionParser
import com.nhlstenden.appdev.features.task.models.Question
import javax.inject.Inject
import javax.inject.Singleton
import android.util.Log
import com.nhlstenden.appdev.core.repositories.AuthRepository
import com.nhlstenden.appdev.core.repositories.BaseRepository
import com.nhlstenden.appdev.core.utils.TaskToCourseMapper
import com.nhlstenden.appdev.supabase.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray

@Singleton
class CourseRepositoryImpl @Inject constructor(
    private val application: Application,
    private val authRepository: AuthRepository,
    private val courseParser: com.nhlstenden.appdev.core.parsers.CourseParser,
    private val taskParser: com.nhlstenden.appdev.core.parsers.TaskParser,
    private val questionParser: QuestionParser,
    private val supabaseClient: SupabaseClient
) : BaseRepository(), CourseRepository {

    private val TAG = "CourseRepositoryImpl"

    private suspend fun getUserProgressSafely(userId: String, authToken: String): JSONArray? {
        return try {
            val result = withContext(Dispatchers.IO) {
                supabaseClient.getUserProgressResponse(userId, authToken)
            }

            if (result.isFailure) {
                Log.e(TAG, "Error getting user progress", result.exceptionOrNull())
                return null
            }

            val response = result.getOrNull() ?: return null
            var resultArray: JSONArray? = null
            
            handleApiResponse(
                response = response,
                authRepository = authRepository,
                onSuccess = { body ->
                    parseJsonArraySafely(
                        jsonString = body,
                        onSuccess = { jsonArray -> resultArray = jsonArray }
                    )
                }
            )
            
            resultArray
        } catch (e: Exception) {
            Log.e(TAG, "Error getting user progress", e)
            if (e.message?.contains("Session expired") == true) {
                throw e
            }
            null
        }
    }

    override suspend fun getCourses(user: User): List<Course>? {
        val userProgresses = try {
            getUserProgressSafely(user.id, user.authToken)
        } catch (e: Exception) {
            // If JWT expired, let the exception propagate to trigger logout
            if (e.message?.contains("Session expired") == true) {
                throw e
            }
            // Return courses with 0 progress if there's an error
            var courses = courseParser.loadAllCourses()
            courses.forEach { course ->
                course.totalTasks = this.getTotalTaskOfCourse(course.id)
                course.progress = 0
            }
            return courses
        }
        
        val userProgressMap = HashMap<String, Int>()
        userProgresses?.let { progressArray ->
            for(i in 0 until progressArray.length()) {
                val JsonObject = progressArray.getJSONObject(i)
                userProgressMap.put(
                    JsonObject.getString("course_id"),
                    JsonObject.getInt("progress")
                )
            }
        }
        
        var courses = courseParser.loadAllCourses()

        courses.forEach { course ->
            course.totalTasks = this.getTotalTaskOfCourse(course.id)
            // Ensure progress is 0 if no database entry exists
            course.progress = userProgressMap[course.id] ?: 0
        }

        return courses
    }

    override suspend fun getCoursesWithoutProgress(): List<Course> {
        var courses = courseParser.loadAllCourses()

        courses.forEach { course ->
            course.totalTasks = this.getTotalTaskOfCourse(course.id)
        }

        return courses
    }

    override suspend fun getTaskById(courseTitle: String, taskTitle: String): Task? {
        return getTasks(courseTitle).find { it.title == taskTitle }
    }

    override suspend fun getTasks(courseId: String): List<Task> {
        // Get current user
        val currentUser = authRepository.getCurrentUserSync() ?: return taskParser.loadAllTasksOfCourse(courseId)
        
        try {
            withContext(Dispatchers.IO) {
                // Try to get user progress for this course
                val userProgresses = getUserProgressSafely(currentUser.id, currentUser.authToken)
                var hasProgress = false
                
                // Check if user has progress for this course
                userProgresses?.let { progressArray ->
                    for(i in 0 until progressArray.length()) {
                        val JsonObject = progressArray.getJSONObject(i)
                        if (JsonObject.getString("course_id") == courseId) {
                            hasProgress = true
                            break
                        }
                    }
                }
                
                // If no progress exists, create it
                if (!hasProgress) {
                    val result = supabaseClient.createUserProgress(currentUser.id, courseId, 0, currentUser.authToken)
                    val response = result.getOrNull()

                    if (response != null && !response.isSuccessful && response.code == 401) {
                        val body = response.body?.string()
                        if (body?.contains("JWT expired") == true) {
                            Log.w(TAG, "JWT expired during createUserProgress")
                            authRepository.handleJWTExpiration()
                            throw Exception("Session expired. Please login again.")
                        }
                    }
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error handling user progress", e)
            // Re-throw JWT expiration exceptions
            if (e.message?.contains("Session expired") == true) {
                throw e
            }
        }
        
        return taskParser.loadAllTasksOfCourse(courseId)
    }

    override suspend fun getTotalTaskOfCourse(courseId: String): Int {
        return getTasks(courseId).size
    }

    override suspend fun getQuestions(taskId: String): List<Question> {
        return questionParser.loadQuestionsForTask(taskId)
    }

    override suspend fun updateTaskProgress(userId: String, taskId: String, progress: Int): Boolean {
        Log.d(TAG, "updateTaskProgress ENTRY: userId=$userId, taskId=$taskId, progress=$progress")
        try {
            val currentUser = authRepository.getCurrentUserSync()

            if (currentUser == null) {
                Log.e(TAG, "No current user found")
                return false
            }

            Log.d(TAG, "updateTaskProgress: Current user found, getting progress...")
            // Get current course progress first
            val userProgresses = getUserProgressSafely(currentUser.id, currentUser.authToken)
            var currentProgress = 0
            
            // Map task to course to find current progress
            val courseId = TaskToCourseMapper.mapTaskIdToCourseId(taskId)
            Log.d(TAG, "updateTaskProgress: taskId='$taskId' mapped to courseId='$courseId'")
            
            if (userProgresses == null) {
                Log.w(TAG, "updateTaskProgress: userProgresses is null")
            } else {
                Log.d(TAG, "updateTaskProgress: userProgresses array length=${userProgresses.length()}")
            }
            
            userProgresses?.let { progressArray ->
                for(i in 0 until progressArray.length()) {
                    val JsonObject = progressArray.getJSONObject(i)
                    val progressCourseId = JsonObject.getString("course_id")
                    val progressValue = JsonObject.getInt("progress")
                    Log.d(TAG, "updateTaskProgress: Found progress entry - courseId='$progressCourseId', progress=$progressValue")
                    if (progressCourseId == courseId) {
                        currentProgress = progressValue
                        Log.d(TAG, "updateTaskProgress: Matched course - setting currentProgress=$currentProgress")
                        break
                    }
                }
            }
            
            // Increment progress by the amount specified (usually 1)
            val newProgress = currentProgress + progress
            Log.d(TAG, "updateTaskProgress: courseId='$courseId', currentProgress=$currentProgress, incrementBy=$progress, newProgress=$newProgress")

            val result = supabaseClient.updateUserProgress(userId, taskId, newProgress, currentUser.authToken)
            val response = result.getOrNull() ?: return false
            
            if (!response.isSuccessful && response.code == 401) {
                val body = response.body?.string()
                if (body?.contains("JWT expired") == true) {
                    Log.w(TAG, "JWT expired during updateTaskProgress")
                    authRepository.handleJWTExpiration()
                    throw Exception("Session expired. Please login again.")
                }
            }
            
            return response.code == 200 || response.code == 204
        } catch (e: Exception) {
            Log.e(TAG, "Error updating task progress: ${e.message}")
            // Re-throw JWT expiration exceptions
            if (e.message?.contains("Session expired") == true) {
                throw e
            }
            return false
        }
    }
} 