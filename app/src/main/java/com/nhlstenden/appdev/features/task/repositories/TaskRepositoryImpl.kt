package com.nhlstenden.appdev.features.task.repositories

import android.app.Application
import com.nhlstenden.appdev.features.task.models.Question
import com.nhlstenden.appdev.core.repositories.TaskRepository
import com.nhlstenden.appdev.features.course.repositories.CourseRepository
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class TaskRepositoryImpl @Inject constructor(
    private val application: Application,
    private val courseRepository: CourseRepository
) : TaskRepository {
    override suspend fun getQuestionsForTask(taskId: String): List<Question> {
        return courseRepository.getQuestions(taskId)
    }

    override suspend fun submitAnswer(questionId: String, answer: String): Boolean {
        // TODO: Replace with actual API call
        return true
    }

    override suspend fun getTaskProgress(taskId: String): Int {
        // TODO: Replace with actual API call
        return 0
    }
} 