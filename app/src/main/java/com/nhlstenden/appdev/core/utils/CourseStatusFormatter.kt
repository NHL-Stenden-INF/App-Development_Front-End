package com.nhlstenden.appdev.core.utils

object CourseStatusFormatter {
    
    fun formatProgressText(progress: Int, totalTasks: Int): String {
        return when {
            progress >= totalTasks && totalTasks > 0 -> "✓ Completed"
            progress == 0 -> "Start course"
            else -> "$progress/$totalTasks tasks"
        }
    }
    
    fun formatProgressPercentageText(progressPercentage: Int): String {
        return when {
            progressPercentage >= 100 -> "✓ 100% Complete"
            progressPercentage == 0 -> "Not started"
            else -> "$progressPercentage% complete"
        }
    }
    
    fun getCompletionDescription(isCompleted: Boolean): String {
        return if (isCompleted) "Course completed" else "Course in progress"
    }
} 