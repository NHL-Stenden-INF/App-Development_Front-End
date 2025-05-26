package com.nhlstenden.appdev.task.data.repositories

import com.nhlstenden.appdev.task.domain.models.Question
import com.nhlstenden.appdev.task.domain.models.QuestionType
import com.nhlstenden.appdev.task.domain.repositories.TaskRepository
import javax.inject.Inject

class TaskRepositoryImpl @Inject constructor() : TaskRepository {
    override suspend fun getQuestionsForTopic(topicId: String): List<Question> {
        // TODO: Replace with actual API call
        return listOf(
            Question(
                id = "1",
                type = QuestionType.MULTIPLE_CHOICE,
                text = "What is the capital of France?",
                options = listOf(
                    Question.Option("1", "London", false),
                    Question.Option("2", "Paris", true),
                    Question.Option("3", "Berlin", false),
                    Question.Option("4", "Madrid", false)
                ),
                correctOptionId = "2"
            ),
            Question(
                id = "2",
                type = QuestionType.TRUE_FALSE,
                text = "The Earth is flat.",
                options = listOf(
                    Question.Option("1", "True", false),
                    Question.Option("2", "False", true)
                ),
                correctOptionId = "2"
            ),
            Question(
                id = "3",
                type = QuestionType.OPEN_ENDED,
                text = "What is the main purpose of a constructor in object-oriented programming?",
                correctAnswer = "To initialize the object's state"
            )
        )
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