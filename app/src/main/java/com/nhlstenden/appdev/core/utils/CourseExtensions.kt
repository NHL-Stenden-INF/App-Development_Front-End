package com.nhlstenden.appdev.core.utils

import com.nhlstenden.appdev.features.courses.model.Course
import com.nhlstenden.appdev.features.progress.models.CourseProgress
import com.nhlstenden.appdev.core.adapters.CourseItem

fun Course.toCourseItem(showDescription: Boolean = true): CourseItem {
    return CourseItem(
        id = this.id,
        title = this.title,
        description = this.description,
        progressText = "${this.progress}/${this.totalTasks}",
        progressPercentage = ProgressCalculator.calculatePercentage(this.progress, this.totalTasks),
        imageResId = this.imageResId,
        showDescription = showDescription,
        showDifficultyStars = true,
        difficulty = this.difficulty
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
        showDifficultyStars = false
    )
} 