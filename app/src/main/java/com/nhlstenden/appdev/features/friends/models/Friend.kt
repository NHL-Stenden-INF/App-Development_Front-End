package com.nhlstenden.appdev.friends.domain.models

data class Friend(
    val id: String,
    val username: String,
    val profilePicture: String?,
    val bio: String? = null,
    val progress: Int = 0,
    val level: Int = 1,
    val currentLevelProgress: Int = 0,
    val currentLevelMax: Int = 100,
    val lastActive: Long = System.currentTimeMillis()
) 