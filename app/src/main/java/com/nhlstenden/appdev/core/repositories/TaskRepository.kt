package com.nhlstenden.appdev.core.repositories

import com.nhlstenden.appdev.features.task.models.Question

interface TaskRepository {
    suspend fun getQuestionsForTopic(topicId: String): List<Question>
    suspend fun submitAnswer(questionId: String, answer: String): Boolean
    suspend fun getTaskProgress(topicId: String): Int
} 