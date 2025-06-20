package com.nhlstenden.appdev.features.course.models

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

@Parcelize
data class Task(
    val id: String,
    val title: String,
    val description: String,
    val difficulty: Int, // Star rating difficulty from 1 (easy) to 5 (very hard)
    val index: Int, // The order of the task in the course
    val questionCount: Int = 0 // Number of questions in this task
) : Parcelable 