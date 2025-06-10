package com.nhlstenden.appdev.features.courses.repositories

import android.app.Application
import com.nhlstenden.appdev.core.models.User
import com.nhlstenden.appdev.features.courses.model.Task
import com.nhlstenden.appdev.features.courses.CourseParser
import com.nhlstenden.appdev.features.courses.CourseRepository
import com.nhlstenden.appdev.features.courses.TaskParser
import com.nhlstenden.appdev.features.courses.model.Course
import com.nhlstenden.appdev.features.courses.QuestionParser
import com.nhlstenden.appdev.features.task.models.Question
import com.nhlstenden.appdev.supabase.SupabaseClient
import javax.inject.Inject
import javax.inject.Singleton
import android.util.Log
import com.nhlstenden.appdev.core.repositories.AuthRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray

@Singleton
class CourseRepositoryImpl @Inject constructor(
    private val application: Application,
    private val authRepository: AuthRepository
) : CourseRepository {
    val courseParser = CourseParser(application.applicationContext)
    val taskParser = TaskParser(application.applicationContext)
    val questionParser = QuestionParser(application.applicationContext)
    val supabaseClient = SupabaseClient()

    private val TAG = "CourseRepositoryImpl"

    private suspend fun isJWTExpired(response: okhttp3.Response): Boolean {
        if (response.code == 401) {
            val body = response.body?.string()
            if (body?.contains("JWT expired") == true) {
                Log.w(TAG, "JWT expired detected, clearing session")
                authRepository.handleJWTExpiration()
                return true
            }
        }
        return false
    }

    private suspend fun getUserProgressSafely(userId: String, authToken: String): JSONArray? {
        return try {
            val response = withContext(Dispatchers.IO) {
                supabaseClient.getUserProgressResponse(userId, authToken)
            }
            
            if (response.isSuccessful) {
                val body = response.body?.string()
                if (!body.isNullOrEmpty()) {
                    JSONArray(body)
                } else {
                    null
                }
            } else {
                if (isJWTExpired(response)) {
                    throw Exception("Session expired. Please login again.")
                } else {
                    Log.e(TAG, "Failed to get user progress: ${response.code}")
                    null
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error getting user progress", e)
            // Re-throw JWT expiration exceptions
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
        val currentUser = authRepository.getCurrentUserSync() ?: return taskParser.loadAllCoursesOfTask(courseId)
        
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
                    val response = supabaseClient.createUserProgress(currentUser.id, courseId, 0, currentUser.authToken)
                    if (!response.isSuccessful && response.code == 401) {
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
        
        return taskParser.loadAllCoursesOfTask(courseId)
    }

    override suspend fun getTotalTaskOfCourse(courseId: String): Int {
        return getTasks(courseId).size
    }

    override suspend fun getQuestions(taskId: String): List<Question> {
        return questionParser.loadQuestionsForTask(taskId)
    }

    override suspend fun updateTaskProgress(userId: String, taskId: String, progress: Int): Boolean {
        try {
            val currentUser = authRepository.getCurrentUserSync()
            if (currentUser == null) {
                Log.e(TAG, "No current user found")
                return false
            }
            val response = supabaseClient.updateUserProgress(userId, taskId, progress, currentUser.authToken)
            
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