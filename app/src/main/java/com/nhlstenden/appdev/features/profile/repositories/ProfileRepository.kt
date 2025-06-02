package com.nhlstenden.appdev.profile.domain.repositories

import com.nhlstenden.appdev.profile.domain.models.Profile

interface ProfileRepository {
    suspend fun getProfile(): Profile
    suspend fun updateProfile(displayName: String, bio: String?, profilePicture: String?): Profile
    suspend fun updateProfilePicture(imagePath: String): Profile
    suspend fun logout()
} 