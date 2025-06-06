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
        return taskParser.loadAllCoursesOfTask(courseId)
    }

    override suspend fun getTotalTaskOfCourse(courseId: String): Int {
        return getTasks(courseId).size
    }

    override suspend fun getQuestions(taskId: String): List<Question> {
        return questionParser.loadQuestionsForTask(taskId)
    }

    override suspend fun updateTaskProgress(user: User, taskId: String, progress: Int) {
        Log.d("CourseRepo", "updateTaskProgress called for userId=${user.id}, taskId=$taskId, progress=$progress")
        val courseId = taskId.substringBefore("_")
        val tasks = getTasks(courseId)
        val task = tasks.find { it.id == taskId } ?: run {
            Log.d("CourseRepo", "Task not found for id=$taskId in courseId=$courseId")
            return
        }
        val userProgresses = try {
            supabaseClient.getUserProgress(user.id, user.authToken)
        } catch (e: Exception) {
            Log.d("CourseRepo", "Exception in getUserProgress: ${e.message}")
            return
        }
        var currentProgress: Int? = null
        for(i in 0 until userProgresses.length()) {
            val JsonObject = userProgresses.getJSONObject(i)
            if (JsonObject.getString("course_id") == courseId) {
                currentProgress = JsonObject.getInt("progress")
                break
            }
        }
        Log.d("CourseRepo", "currentProgress=$currentProgress, task.index=${task.index}")
        if (currentProgress == null) {
            Log.d("CourseRepo", "Calling insertUserProgressRPC for userId=${user.id}, courseId=$courseId")
            val response = supabaseClient.insertUserProgressRPC(user.id, courseId, 1, user.authToken)
            Log.d("CourseRepo", "insertUserProgressRPC response: code=${response.code}, body=${response.body?.string()}")
        } else if (task.index >= currentProgress) {
            Log.d("CourseRepo", "Updating progress for userId=${user.id}, courseId=$courseId to ${currentProgress + 1}")
            supabaseClient.updateUserProgress(user.id, taskId, currentProgress + 1, user.authToken)
        } else {
            Log.d("CourseRepo", "No progress update: task.index=${task.index}, currentProgress=$currentProgress")
        }
    }
} 