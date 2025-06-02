package com.nhlstenden.appdev.features.task.models

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

@Parcelize
data class Question(
    val id: String,
    val type: QuestionType,
    val text: String,
    val options: List<Option> = emptyList(),
    val correctOptionId: String? = null,
    val correctAnswer: String? = null,
    val explanation: String? = null,
    val front: String? = null,
    val back: String? = null,
    val mistakes: Int? = null,
    val correctText: String? = null,
    val isCompleted: Boolean = false
) : Parcelable {
    @Parcelize
    data class Option(
        val id: String,
        val text: String,
        val isCorrect: Boolean = false
    ) : Parcelable
}