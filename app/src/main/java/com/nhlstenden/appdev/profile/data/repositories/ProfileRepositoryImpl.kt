package com.nhlstenden.appdev.profile.data.repositories

import com.nhlstenden.appdev.R
import com.nhlstenden.appdev.profile.domain.models.Profile
import com.nhlstenden.appdev.profile.domain.repositories.ProfileRepository
import com.nhlstenden.appdev.supabase.User
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ProfileRepositoryImpl @Inject constructor() : ProfileRepository {
    private val defaultProfilePicture = R.drawable.ic_profile_placeholder
    private var userData: User? = null

    override suspend fun getProfile(): Profile {
        // TODO: Implement actual API call
        return Profile(
            username = userData?.username ?: "John Doe",
            email = userData?.email ?: "john.doe@example.com",
            level = 1,
            experience = 0,
            profilePictureResId = defaultProfilePicture
        )
    }

    override suspend fun updateProfile(username: String, email: String): Profile {
        // TODO: Implement actual API call
        return Profile(
            username = username,
            email = email,
            level = 1,
            experience = 0,
            profilePictureResId = defaultProfilePicture
        )
    }

    override suspend fun updateProfilePicture(imagePath: String): Profile {
        // TODO: Implement actual API call
        return Profile(
            username = userData?.username ?: "John Doe",
            email = userData?.email ?: "john.doe@example.com",
            level = 1,
            experience = 0,
            profilePictureResId = defaultProfilePicture
        )
    }

    override suspend fun logout() {
        // TODO: Implement actual logout logic
        userData = null
    }

    fun setUserData(user: User?) {
        userData = user
    }
} 