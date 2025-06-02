package com.nhlstenden.appdev.core.models

import java.util.UUID
import android.os.Parcelable
import kotlinx.parcelize.Parcelize

@Parcelize
data class User(
    val id: String,
    val email: String,
    val username: String,
    val profilePicture: String? = null,
    val friends: MutableList<UUID> = mutableListOf(),
    val authToken: String = ""
) : Parcelable 