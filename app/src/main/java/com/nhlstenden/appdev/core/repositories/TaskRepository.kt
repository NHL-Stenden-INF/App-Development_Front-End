package com.nhlstenden.appdev.core.repositories

import com.nhlstenden.appdev.features.task.models.Question

interface TaskRepository {
    suspend fun getQuestionsForTask(taskId: String): List<Question>
    suspend fun submitAnswer(questionId: String, answer: String): Boolean
    suspend fun getTaskProgress(taskId: String): Int
} 