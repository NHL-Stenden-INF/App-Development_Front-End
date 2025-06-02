package com.nhlstenden.appdev.features.courses

import com.nhlstenden.appdev.features.courses.model.Course
import com.nhlstenden.appdev.features.courses.model.Topic
import com.nhlstenden.appdev.features.task.models.Question

interface CourseRepository {
    suspend fun getCourses(): List<Course>
    suspend fun getTopicById(courseTitle: String, topicTitle: String): Topic?
    suspend fun updateTopicProgress(courseTitle: String, topicTitle: String, progress: Int)
    suspend fun getTopics(courseId: String): List<Topic>
    suspend fun getQuestions(topicId: String): List<Question>
} 