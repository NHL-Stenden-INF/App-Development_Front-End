package com.nhlstenden.appdev

// For Registration (POST /user/)
data class RegisterRequest(
    val username: String,
    val email: String,
    val password: String
)

// For receiving user data (GET /user/)
data class UserResponse(
    val id: Int,
    val username: String,
    val email: String,
    val points: Int
)