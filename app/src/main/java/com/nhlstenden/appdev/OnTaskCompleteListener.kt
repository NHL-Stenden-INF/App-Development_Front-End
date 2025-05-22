package com.nhlstenden.appdev

interface OnTaskCompleteListener {
    fun onTaskCompleted (question: TaskActivity.Question, hasSucceeded: Boolean)
}