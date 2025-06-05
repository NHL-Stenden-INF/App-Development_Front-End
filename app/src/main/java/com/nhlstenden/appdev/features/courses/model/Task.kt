package com.nhlstenden.appdev.features.courses.model

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

@Parcelize
data class Task(
    val id: String,
    val title: String,
    val description: String,
    val difficulty: String,
    val progress: Int
) : Parcelable 