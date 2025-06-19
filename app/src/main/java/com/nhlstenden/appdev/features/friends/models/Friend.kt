package com.nhlstenden.appdev.features.friends.models

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

@Parcelize
data class Friend(
    val id: String,
    val username: String,
    val profilePicture: String?,
    val bio: String? = null,
    val progress: Int = 0,
    val level: Int = 1,
    val currentLevelProgress: Int = 0,
    val currentLevelMax: Int = 100,
    val lastActive: Long = System.currentTimeMillis(),
    val profileMask: Int = 0,
) : Parcelable 