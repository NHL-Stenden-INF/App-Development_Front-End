package com.nhlstenden.appdev.features.courses.repositories

import android.app.Application
import com.nhlstenden.appdev.features.courses.model.Topic
import com.nhlstenden.appdev.features.courses.CourseParser
import com.nhlstenden.appdev.features.courses.TaskParser
import com.nhlstenden.appdev.features.courses.model.Course
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class CourseRepositoryImpl @Inject constructor(
    application: Application
) : CourseRepository {
    val courseParser = CourseParser(application.applicationContext)
    val taskParser = TaskParser(application.applicationContext)

    override suspend fun getCourses(): List<Course> {
        return courseParser.loadAllCourses()
    }

    override suspend fun getTopicById(courseTitle: String, topicTitle: String): Topic? {
        return getTopics(courseTitle).find { it.title == topicTitle }
    }

    override suspend fun updateTopicProgress(courseTitle: String, topicTitle: String, progress: Int) {
        // TODO: Implement actual progress update in Supabase
    }

    override suspend fun getTopics(coursetitle: String): List<Topic> {
        return taskParser.loadAllCoursesOfTopic(coursetitle)
    }
} 