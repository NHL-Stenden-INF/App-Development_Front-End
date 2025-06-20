package com.nhlstenden.appdev.features.courses.repositories

import android.app.Application
import com.nhlstenden.appdev.core.models.User
import com.nhlstenden.appdev.features.courses.model.Course
import com.nhlstenden.appdev.features.courses.repositories.CoursesRepository
import javax.inject.Inject
import javax.inject.Singleton
import android.util.Log
import com.nhlstenden.appdev.core.repositories.AuthRepository
import com.nhlstenden.appdev.core.repositories.BaseRepository
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
    private val supabaseClient: SupabaseClient
) : BaseRepository(), CoursesRepository {

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

    // Helper method for getting total tasks count
    private suspend fun getTotalTaskOfCourse(courseId: String): Int {
        return taskParser.loadAllTasksOfCourse(courseId).size
    }
} 