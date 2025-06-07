package com.nhlstenden.appdev.features.courses

import com.nhlstenden.appdev.core.models.User
import com.nhlstenden.appdev.features.courses.model.Course
import com.nhlstenden.appdev.features.courses.model.Task
import com.nhlstenden.appdev.features.task.models.Question

interface CourseRepository {
    suspend fun getCourses(user: User): List<Course>?
    suspend fun getCoursesWithoutProgress(): List<Course>
    suspend fun getTaskById(courseTitle: String, taskTitle: String): Task?
    suspend fun getTasks(courseId: String): List<Task>
    suspend fun getTotalTaskOfCourse(courseId: String): Int
    suspend fun getQuestions(taskId: String): List<Question>
    suspend fun updateTaskProgress(userId: String, taskId: String, progress: Int): Boolean
} 