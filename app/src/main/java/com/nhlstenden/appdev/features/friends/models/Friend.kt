package com.nhlstenden.appdev.friends.domain.models

data class Friend(
    val id: String,
    val username: String,
    val profilePicture: String?,
    val bio: String? = null,
    val progress: Int = 0,
    val lastActive: Long = System.currentTimeMillis()
) 