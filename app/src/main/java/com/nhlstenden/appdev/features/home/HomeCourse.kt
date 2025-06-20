package com.nhlstenden.appdev.features.home

data class HomeCourse(
    val id: String,
    val title: String,
    val progressText: String,
    val progressPercent: Int,
    val iconResId: Int,
    val accentColor: Int
) 