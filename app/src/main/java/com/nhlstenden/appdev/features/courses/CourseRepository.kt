package com.nhlstenden.appdev.features.courses.repositories

import com.nhlstenden.appdev.features.courses.model.Course
import com.nhlstenden.appdev.features.courses.model.Topic

interface CourseRepository {
    suspend fun getCourses(): List<Course>
    suspend fun getTopicById(courseTitle: String, topicTitle: String): Topic?
    suspend fun updateTopicProgress(courseTitle: String, topicTitle: String, progress: Int)
    suspend fun getTopics(courseId: String): List<Topic>
} 