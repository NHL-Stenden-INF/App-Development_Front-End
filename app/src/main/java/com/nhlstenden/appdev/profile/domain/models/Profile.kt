package com.nhlstenden.appdev.profile.domain.models

data class Profile(
    val username: String,
    val email: String,
    val level: Int,
    val experience: Int,
    val profilePictureResId: Int
) 