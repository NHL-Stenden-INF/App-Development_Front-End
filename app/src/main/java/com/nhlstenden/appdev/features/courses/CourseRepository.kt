package com.nhlstenden.appdev.features.courses.repositories

import com.nhlstenden.appdev.features.courses.model.Topic

interface CourseRepository {
    suspend fun getTopics(): List<Topic>
    suspend fun getTopicById(topicId: String): Topic?
    suspend fun updateTopicProgress(topicId: String, progress: Int)
    suspend fun getTopics(courseId: String): List<Topic>
} 