package com.nhlstenden.appdev.features.courses.repositories

import android.app.Application
import com.nhlstenden.appdev.core.models.User
import com.nhlstenden.appdev.features.courses.model.Course
import com.nhlstenden.appdev.features.course.utils.CourseParser as CourseParserImpl
import com.nhlstenden.appdev.features.course.utils.TaskParser as TaskParserImpl
import com.nhlstenden.appdev.core.utils.JwtHandler
import javax.inject.Inject
import javax.inject.Singleton
import android.util.Log
import com.nhlstenden.appdev.supabase.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray

@Singleton
class CoursesRepositoryImpl @Inject constructor(
    private val application: Application,
    private val courseParser: CourseParserImpl,
    private val taskParser: TaskParserImpl,
    private val supabaseClient: SupabaseClient,
    private val jwtHandler: JwtHandler
) : CoursesRepository {

    private val TAG = "CoursesRepositoryImpl"

    private suspend fun getUserProgressSafely(userId: String, authToken: String): JSONArray? {
        return try {
            val result = withContext(Dispatchers.IO) {
                supabaseClient.getUserProgressResponse(userId, authToken)
            }

            if (result.isFailure) {
                Log.e(TAG, "Error getting user progress", result.exceptionOrNull())
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
                if (jwtHandler.handleJwtResponse(response)) {
                    throw jwtHandler.createSessionExpiredException()
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

    private suspend fun getTotalTaskOfCourse(courseId: String): Int {
        return taskParser.loadAllTasksOfCourse(courseId).size
    }
} 