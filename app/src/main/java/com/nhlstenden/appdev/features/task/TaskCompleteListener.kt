package com.nhlstenden.appdev.features.task

import com.nhlstenden.appdev.features.task.models.Question

interface TaskCompleteListener {
    fun onTaskCompleted(question: Question)
    fun onQuestionCompleted(isCorrect: Boolean)
} 