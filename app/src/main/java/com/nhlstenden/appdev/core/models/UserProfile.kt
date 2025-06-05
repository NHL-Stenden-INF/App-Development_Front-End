package com.nhlstenden.appdev.core.models

import com.nhlstenden.appdev.core.models.Achievement

data class UserProfile(
    val id: String,
    val username: String,
    val email: String,
    val profilePicture: String?,
    val level: Int = 1,
    val experience: Int = 0,
    val totalExperience: Int = 1000,
    val achievements: List<Achievement> = emptyList()
) 