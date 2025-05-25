package com.nhlstenden.appdev.courses.data.repositories

import com.nhlstenden.appdev.courses.domain.models.Topic
import com.nhlstenden.appdev.courses.domain.repositories.CourseRepository
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class CourseRepositoryImpl @Inject constructor() : CourseRepository {
    override suspend fun getTopics(): List<Topic> {
        // TODO: Implement actual data fetching from Supabase
        return listOf(
            Topic(
                id = "1",
                title = "HTML Basics",
                description = "Learn the fundamentals of HTML markup",
                difficulty = "Beginner",
                progress = 0
            ),
            Topic(
                id = "2",
                title = "CSS Styling",
                description = "Master CSS styling and layout",
                difficulty = "Intermediate",
                progress = 0
            ),
            Topic(
                id = "3",
                title = "SQL Fundamentals",
                description = "Learn database management and queries",
                difficulty = "Beginner",
                progress = 0
            )
        )
    }

    override suspend fun getTopicById(topicId: String): Topic? {
        return getTopics().find { it.id == topicId }
    }

    override suspend fun updateTopicProgress(topicId: String, progress: Int) {
        // TODO: Implement actual progress update in Supabase
    }
} 