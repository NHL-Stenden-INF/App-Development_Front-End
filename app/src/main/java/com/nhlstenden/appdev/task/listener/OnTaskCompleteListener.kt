package com.nhlstenden.appdev.task.listener

import com.nhlstenden.appdev.task.ui.TaskActivity

interface OnTaskCompleteListener {
    fun onTaskCompleted (question: TaskActivity.Question, hasSucceeded: Boolean)
}