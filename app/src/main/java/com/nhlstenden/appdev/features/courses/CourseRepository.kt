package com.nhlstenden.appdev.features.courses

import com.nhlstenden.appdev.core.models.User
import com.nhlstenden.appdev.features.courses.model.Course
import com.nhlstenden.appdev.features.courses.model.Task
import com.nhlstenden.appdev.features.task.models.Question

interface CourseRepository {
    suspend fun getCourses(): List<Course>
    suspend fun getTaskById(courseTitle: String, taskTitle: String): Task?
    suspend fun updateTaskProgress(user: User, taskId: String, progress: Int)
    suspend fun getTasks(courseId: String): List<Task>
    suspend fun getQuestions(taskId: String): List<Question>
} 