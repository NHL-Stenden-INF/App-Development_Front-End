package com.nhlstenden.appdev.profile.domain.models

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

data class Achievement(
    val id: String,
    val title: String,
    val description: String,
    val iconResId: Int,
    val unlocked: Boolean = false
) 