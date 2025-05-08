package com.nhlstenden.appdev

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

// For Registration (POST /user/)
data class RegisterRequest(
    val username: String,
    val email: String,
    val password: String
)

// For receiving user data (GET /user/)
@Parcelize
data class UserResponse(
    val id: Int,
    val username: String,
    val email: String,
    val points: Int
) : Parcelable