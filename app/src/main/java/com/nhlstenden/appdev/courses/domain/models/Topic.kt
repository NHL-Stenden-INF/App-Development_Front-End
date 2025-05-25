package com.nhlstenden.appdev.courses.domain.models

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

@Parcelize
data class Topic(
    val id: String,
    val title: String,
    val description: String,
    val difficulty: String,
    val progress: Int
) : Parcelable 