package com.nhlstenden.appdev.core.utils

import com.nhlstenden.appdev.features.courses.model.Course
import com.nhlstenden.appdev.features.progress.models.CourseProgress
import com.nhlstenden.appdev.core.adapters.CourseItem

fun Course.toCourseItem(showDescription: Boolean = true): CourseItem {
    val progressPercentage = ProgressCalculator.calculatePercentage(this.progress, this.totalTasks)
    val isCompleted = CourseItem.isProgressComplete(progressPercentage)
    
    return CourseItem(
        id = this.id,
        title = this.title,
        description = this.description,
        progressText = CourseStatusFormatter.formatProgressText(this.progress, this.totalTasks),
        progressPercentage = progressPercentage,
        imageResId = this.imageResId,
        showDescription = showDescription,
        showDifficultyStars = true,
        difficulty = this.difficulty,
        isCompleted = isCompleted,
        isLocked = false // Courses are generally not locked, but can be customized
    )
}

fun CourseProgress.toCourseItem(): CourseItem {
    return CourseItem(
        id = this.id,
        title = this.title,
        description = "",
        progressText = this.completionStatus,
        progressPercentage = this.progressPercentage,
        imageResId = this.imageResId,
        showDescription = false,
        showDifficultyStars = false,
        isCompleted = CourseItem.isProgressComplete(this.progressPercentage),
        isLocked = false
    )
} 