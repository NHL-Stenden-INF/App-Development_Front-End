package com.nhlstenden.appdev.features.task.models

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

@Parcelize
enum class QuestionType : Parcelable {
    MULTIPLE_CHOICE,
    FLIP_CARD,
    PRESS_MISTAKES,
    EDIT_TEXT,
    OPEN_ENDED,
    TRUE_FALSE
} 