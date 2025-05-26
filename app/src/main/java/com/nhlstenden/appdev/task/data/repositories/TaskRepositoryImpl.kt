package com.nhlstenden.appdev.task.data.repositories

import android.app.Application
import com.nhlstenden.appdev.courses.parser.CourseParser
import com.nhlstenden.appdev.courses.parser.QuestionParser
import com.nhlstenden.appdev.task.domain.models.Question
import com.nhlstenden.appdev.task.domain.models.QuestionType
import com.nhlstenden.appdev.task.domain.repositories.TaskRepository
import javax.inject.Inject

class TaskRepositoryImpl @Inject constructor(
    private val application: Application
) : TaskRepository {
    override suspend fun getQuestionsForTopic(topicId: String): List<Question> {
        // Get the topic title and difficulty from the XML using CourseParser
        val courseParser = CourseParser(application.applicationContext)
        val allCourses = courseParser.loadAllCourses()
        val topic = allCourses.flatMap { it.topics }.find { it.id == topicId }
        if (topic != null) {
            val questionParser = QuestionParser(application.applicationContext)
            return questionParser.loadQuestionsForTopic(topic.title)
        }
        return emptyList()
    }

    override suspend fun submitAnswer(questionId: String, answer: String): Boolean {
        // TODO: Replace with actual API call
        return true
    }

    override suspend fun getTaskProgress(topicId: String): Int {
        // TODO: Replace with actual API call
        return 0
    }
} 