package com.nhlstenden.appdev.features.task.models

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

@Parcelize
sealed class Question : Parcelable {
    abstract val id: String
    abstract val question: String
    abstract val explanation: String?

    @Parcelize
    data class MultipleChoiceQuestion(
        override val id: String,
        override val question: String,
        override val explanation: String? = null,

        val options: List<MultipleChoiceOption> = emptyList(),
    ) : Question()

    @Parcelize
    data class FlipCardQuestion(
        override val id: String,
        override val question: String,
        override val explanation: String? = null,

        val front: String,
        val back: String
    ) : Question()

    @Parcelize
    data class PressMistakeQuestion(
        override val id: String,
        override val question: String,
        override val explanation: String? = null,

        val displayedText: String,
        val mistakes: List<String>, // This will be the words or sentences that are wrong
    ) : Question()

    @Parcelize
    data class EditTextQuestion(
        override val id: String,
        override val question: String,
        override val explanation: String? = null,

        val displayedText: String,
        val correctText: String
    ) : Question()
}