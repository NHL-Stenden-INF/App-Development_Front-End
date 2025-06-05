package com.nhlstenden.appdev.features.courses.repositories

import android.app.Application
import com.nhlstenden.appdev.core.models.User
import com.nhlstenden.appdev.features.courses.model.Task
import com.nhlstenden.appdev.features.courses.CourseParser
import com.nhlstenden.appdev.features.courses.CourseRepository
import com.nhlstenden.appdev.features.courses.TaskParser
import com.nhlstenden.appdev.features.courses.model.Course
import com.nhlstenden.appdev.features.courses.QuestionParser
import com.nhlstenden.appdev.features.courses.model.TaskProgress
import com.nhlstenden.appdev.features.task.models.Question
import com.nhlstenden.appdev.supabase.SupabaseClient
import org.json.JSONObject
import javax.inject.Inject
import javax.inject.Singleton

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
            return null
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
            course.progress = userProgressMap[course.id] ?: 0
        }

        return courses
    }

    override suspend fun getTaskById(courseTitle: String, taskTitle: String): Task? {
        return getTasks(courseTitle).find { it.title == taskTitle }
    }

    override suspend fun updateTaskProgress(user: User, taskId: String, progress: Int) {
        supabaseClient.updateUserProgress(user.id, taskId, progress, user.authToken)
    }

    override suspend fun getTasks(courseId: String): List<Task> {
        return taskParser.loadAllCoursesOfTask(courseId)
    }

    override suspend fun getTotalTaskOfCourse(courseId: String): Int {
        return getTasks(courseId).size
    }

    override suspend fun getQuestions(taskId: String): List<Question> {
        return questionParser.loadQuestionsForTask(taskId)
    }
} 