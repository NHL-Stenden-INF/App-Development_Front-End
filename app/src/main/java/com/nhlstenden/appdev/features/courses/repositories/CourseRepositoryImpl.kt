package com.nhlstenden.appdev.features.courses.repositories

import android.app.Application
import com.nhlstenden.appdev.features.courses.model.Task
import com.nhlstenden.appdev.features.courses.CourseParser
import com.nhlstenden.appdev.features.courses.CourseRepository
import com.nhlstenden.appdev.features.courses.TaskParser
import com.nhlstenden.appdev.features.courses.model.Course
import com.nhlstenden.appdev.features.courses.QuestionParser
import com.nhlstenden.appdev.features.task.models.Question
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class CourseRepositoryImpl @Inject constructor(
    application: Application
) : CourseRepository {
    val courseParser = CourseParser(application.applicationContext)
    val taskParser = TaskParser(application.applicationContext)
    val questionParser = QuestionParser(application.applicationContext)

    override suspend fun getCourses(): List<Course> {
        return courseParser.loadAllCourses()
    }

    override suspend fun getTaskById(courseTitle: String, taskTitle: String): Task? {
        return getTasks(courseTitle).find { it.title == taskTitle }
    }

    override suspend fun updateTaskProgress(courseTitle: String, taskTitle: String, progress: Int) {
        // TODO: Implement actual progress update in Supabase
    }

    override suspend fun getTasks(courseId: String): List<Task> {
        return taskParser.loadAllCoursesOfTask(courseId)
    }

    override suspend fun getQuestions(taskId: String): List<Question> {
        return questionParser.loadQuestionsForTask(taskId)
    }
} 