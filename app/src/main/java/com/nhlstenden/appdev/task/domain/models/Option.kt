package com.nhlstenden.appdev.task.domain.models

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

@Parcelize
data class Option(
    val id: String,
    val text: String,
    val isCorrect: Boolean
) : Parcelable 