package com.nhlstenden.appdev.task.listener

import com.nhlstenden.appdev.task.domain.models.Question

interface TaskCompleteListener {
    fun onTaskCompleted(question: Question)
    fun onTaskComplete(isCorrect: Boolean)
} 