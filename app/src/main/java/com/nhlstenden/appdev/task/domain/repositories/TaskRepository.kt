package com.nhlstenden.appdev.task.domain.repositories

import com.nhlstenden.appdev.task.domain.models.Question

interface TaskRepository {
    suspend fun getQuestionsForTopic(topicId: String): List<Question>
    suspend fun submitAnswer(questionId: String, answer: String): Boolean
    suspend fun getTaskProgress(topicId: String): Int
} 