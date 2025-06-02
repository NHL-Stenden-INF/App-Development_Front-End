package com.nhlstenden.appdev.features.task.repositories

import android.app.Application
import com.nhlstenden.appdev.features.task.models.Question
import com.nhlstenden.appdev.core.repositories.TaskRepository
import com.nhlstenden.appdev.features.courses.CourseParser
import com.nhlstenden.appdev.features.courses.parser.QuestionParser
import com.nhlstenden.appdev.features.courses.repositories.CourseRepositoryImpl
import javax.inject.Inject

class TaskRepositoryImpl @Inject constructor(
    private val application: Application
) : TaskRepository {
    override suspend fun getQuestionsForTopic(topicId: String): List<Question> {
        // Get the topic title and difficulty from the XML using CourseParser
        val courseRepository = CourseRepositoryImpl(application)
        return courseRepository.getQuestions(topicId)
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