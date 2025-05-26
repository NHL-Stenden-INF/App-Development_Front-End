package com.nhlstenden.appdev.courses.domain.models

data class Course(
    val id: String,
    val title: String,
    val description: String,
    val difficulty: String,
    val imageResId: Int,
    val topics: List<Topic>
) 