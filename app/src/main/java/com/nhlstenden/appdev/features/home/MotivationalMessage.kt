package com.nhlstenden.appdev.features.home

data class MotivationalMessage(
    val text: String,
    val profilePicture: String? = null,
    val profileMask: String = "circle"
) 