package com.nhlstenden.appdev.features.task.models

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

@Parcelize
data class Task(
    val id: String,
    val title: String,
    val description: String,
    val questions: List<Question>
) : Parcelable 