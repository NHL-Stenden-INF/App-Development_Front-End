package com.nhlstenden.appdev.features.course.repositories

import com.nhlstenden.appdev.core.models.User
import com.nhlstenden.appdev.features.courses.model.Task
import com.nhlstenden.appdev.features.courses.model.Course
import com.nhlstenden.appdev.features.course.utils.TaskParser
import com.nhlstenden.appdev.features.course.utils.QuestionParser
import com.nhlstenden.appdev.features.task.models.Question
import javax.inject.Inject
import javax.inject.Singleton
import android.util.Log
import com.nhlstenden.appdev.core.repositories.AuthRepository
import com.nhlstenden.appdev.core.utils.TaskToCourseMapper
import com.nhlstenden.appdev.supabase.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray

@Singleton
class CourseRepositoryImpl @Inject constructor(
    private val taskParser: TaskParser,
    private val questionParser: QuestionParser,
    private val supabaseClient: SupabaseClient,
    private val authRepository: AuthRepository
) : CourseRepository {

    private val TAG = "CourseRepositoryImpl"



    override suspend fun getTaskById(courseTitle: String, taskTitle: String): Task? {
        return taskParser.loadAllTasksOfCourse(courseTitle).find { 
            it.title.equals(taskTitle, ignoreCase = true) 
        }
    }

    override suspend fun getTotalTaskOfCourse(courseId: String): Int {
        return taskParser.loadAllTasksOfCourse(courseId).size
    }

    private suspend fun getUserProgressSafely(userId: String, authToken: String): JSONArray? {
        return try {
            val result = withContext(Dispatchers.IO) {
                supabaseClient.getUserProgressResponse(userId, authToken)
            }

            val response = result.getOrNull() ?: return null
            
            if (response.isSuccessful) {
                val body = response.body?.string()
                if (!body.isNullOrEmpty()) {
                    JSONArray(body)
                } else {
                    null
                }
            } else {
                Log.e(TAG, "Failed to get user progress: ${response.code}")
                null
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error getting user progress", e)
            null
        }
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
                    if (result.isFailure) {
                        Log.e(TAG, "Failed to create user progress", result.exceptionOrNull())
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

    override suspend fun getCourseProgress(userId: String, courseId: String): Map<String, Int> {
        return try {
            withContext(Dispatchers.IO) {
                val currentUser = authRepository.getCurrentUserSync()
                if (currentUser != null) {
                    // Get user progress for this specific course
                    val userProgresses = getUserProgressSafely(currentUser.id, currentUser.authToken)
                    val progressMap = mutableMapOf<String, Int>()
                    
                    // Find the progress for this course
                    var courseProgress = 0
                    userProgresses?.let { progressArray ->
                        for(i in 0 until progressArray.length()) {
                            val JsonObject = progressArray.getJSONObject(i)
                            if (JsonObject.getString("course_id") == courseId) {
                                courseProgress = JsonObject.getInt("progress")
                                break
                            }
                        }
                    }
                    
                    // Get all tasks for this course and map their progress
                    val tasks = taskParser.loadAllTasksOfCourse(courseId)
                    tasks.forEachIndexed { index, task ->
                        when {
                            index < courseProgress -> {
                                // Task is completed - set to full question count
                                progressMap[task.id] = task.questionCount
                            }
                            index == courseProgress || index == 0 -> {
                                // Current task or first task (unlocked but not completed)
                                // Use -1 as a special marker for "unlocked but not completed"
                                progressMap[task.id] = -1
                            }
                            else -> {
                                // Task is locked
                                progressMap[task.id] = 0
                            }
                        }
                    }
                    
                    progressMap
                } else {
                    emptyMap()
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error getting course progress", e)
            emptyMap()
        }
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
            return response.code == 200 || response.code == 204
        } catch (e: Exception) {
            Log.e(TAG, "Error updating task progress: ${e.message}")
            // BaseRepository already handles JWT expiration
            return false
        }
    }
} 