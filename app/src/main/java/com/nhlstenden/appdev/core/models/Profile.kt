package com.nhlstenden.appdev.core.models

data class Profile(
    val displayName: String,
    val email: String,
    val bio: String?,
    val profilePicture: String?,
    val level: Int,
    val experience: Int,
    val friendMask: String = "circle",
    val unlockedRewardIds: List<Int>? = null,
    val bellPeppers: Int = 0
) 