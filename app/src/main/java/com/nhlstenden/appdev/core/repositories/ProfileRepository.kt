package com.nhlstenden.appdev.core.repositories

import com.nhlstenden.appdev.core.models.Profile

interface ProfileRepository {
    suspend fun getProfile(): Result<Profile>
    suspend fun updateProfile(displayName: String, bio: String?, profilePicture: String?): Result<Profile>
    suspend fun updateProfilePicture(imagePath: String): Result<Profile>
    suspend fun updateBio(bio: String): Result<Profile>
    suspend fun updateDisplayName(displayName: String): Result<Profile>
    suspend fun logout(): Result<Unit>
} 