package com.nhlstenden.appdev.features.task.models

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

@Parcelize
data class MultipleChoiceOption(
    val id: String,
    val text: String,
    val isCorrect: Boolean
) : Parcelable 