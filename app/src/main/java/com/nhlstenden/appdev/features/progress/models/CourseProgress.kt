package com.nhlstenden.appdev.features.progress.models

data class CourseProgress(
    val id: String,
    val title: String,
    val completionStatus: String,
    val progressPercentage: Int,
    val imageResId: Int
) 