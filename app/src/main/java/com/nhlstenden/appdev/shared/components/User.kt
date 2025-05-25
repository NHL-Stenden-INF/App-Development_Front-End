package com.nhlstenden.appdev.shared.components

data class User(
    val id: String,
    val email: String,
    val username: String,
    val profilePicture: String? = null
) 