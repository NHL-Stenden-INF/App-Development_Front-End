package com.nhlstenden.appdev.features.courses.model

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

@Parcelize
data class Course(
    val id: String,
    val title: String,
    val description: String,
    val difficulty: Int, // 1-5 stars, consider using CourseDifficulty enum
    val imageResId: Int,
    var progress: Int,
    var totalTasks: Int,
) : Parcelable 