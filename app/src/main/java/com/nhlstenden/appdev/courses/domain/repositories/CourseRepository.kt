package com.nhlstenden.appdev.courses.domain.repositories

import com.nhlstenden.appdev.courses.domain.models.Topic

interface CourseRepository {
    suspend fun getTopics(): List<Topic>
    suspend fun getTopicById(topicId: String): Topic?
    suspend fun updateTopicProgress(topicId: String, progress: Int)
} 