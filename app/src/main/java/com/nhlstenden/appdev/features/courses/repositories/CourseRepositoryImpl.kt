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
import com.nhlstenden.appdev.core.utils.UserManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

@Singleton
class CourseRepositoryImpl @Inject constructor(
    application: Application
) : CourseRepository {
    val courseParser = CourseParser(application.applicationContext)
    val taskParser = TaskParser(application.applicationContext)
    val questionParser = QuestionParser(application.applicationContext)
    val supabaseClient = SupabaseClient()

    override suspend fun getCourses(user: User): List<Course>? {
        val userProgresses = try {
            supabaseClient.getUserProgress(user.id, user.authToken)
        } catch (e: Exception) {
            // Return courses with 0 progress if there's an error
            var courses = courseParser.loadAllCourses()
            courses.forEach { course ->
                course.totalTasks = this.getTotalTaskOfCourse(course.id)
                course.progress = 0
            }
            return courses
        }
        
        val userProgressMap = HashMap<String, Int>(userProgresses.length())
        for(i in 0 until userProgresses.length()) {
            val JsonObject = userProgresses.getJSONObject(i)
            userProgressMap.put(
                JsonObject.getString("course_id"),
                JsonObject.getInt("progress")
            )
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
        val currentUser = UserManager.getCurrentUser() ?: return taskParser.loadAllCoursesOfTask(courseId)
        
        try {
            withContext(Dispatchers.IO) {
                // Try to get user progress for this course
                val userProgresses = supabaseClient.getUserProgress(currentUser.id, currentUser.authToken)
                var hasProgress = false
                
                // Check if user has progress for this course
                for(i in 0 until userProgresses.length()) {
                    val JsonObject = userProgresses.getJSONObject(i)
                    if (JsonObject.getString("course_id") == courseId) {
                        hasProgress = true
                        break
                    }
                }
                
                // If no progress exists, create it
                if (!hasProgress) {
                    supabaseClient.createUserProgress(currentUser.id, courseId, 0, currentUser.authToken)
                }
            }
        } catch (e: Exception) {
            Log.e("CourseRepositoryImpl", "Error handling user progress", e)
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
            val currentUser = UserManager.getCurrentUser()
            if (currentUser == null) {
                Log.e("CourseRepositoryImpl", "No current user found")
                return false
            }
            val response = supabaseClient.updateUserProgress(userId, taskId, progress, currentUser.authToken)
            return response.code == 200 || response.code == 204
        } catch (e: Exception) {
            Log.e("CourseRepositoryImpl", "Error updating task progress: ${e.message}")
            return false
        }
    }
} 