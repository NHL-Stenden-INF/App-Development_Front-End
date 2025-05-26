package com.nhlstenden.appdev.profile.data.repositories

import com.nhlstenden.appdev.R
import com.nhlstenden.appdev.profile.domain.models.Profile
import com.nhlstenden.appdev.profile.domain.repositories.ProfileRepository
import com.nhlstenden.appdev.supabase.User
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ProfileRepositoryImpl @Inject constructor(
    private val supabaseClient: com.nhlstenden.appdev.supabase.SupabaseClient
) : ProfileRepository {
    private val defaultProfilePicture = R.drawable.ic_profile_placeholder
    private var userData: User? = null

    override suspend fun getProfile(): Profile {
        val token = userData?.authToken ?: throw IllegalStateException("No auth token available")
        val profileJson = supabaseClient.fetchProfile(token)
        return Profile(
            displayName = profileJson.optString("display_name", ""),
            email = profileJson.optString("email", ""),
            bio = profileJson.optString("bio", null),
            profilePicture = profileJson.optString("profile_picture", null),
            level = 1,
            experience = 0
        )
    }

    override suspend fun updateProfile(displayName: String, bio: String?, profilePicture: String?): Profile {
        val token = userData?.authToken ?: throw IllegalStateException("No auth token available")
        val updated = supabaseClient.updateProfile(token, displayName = displayName, bio = bio, profilePicture = profilePicture)
        return Profile(
            displayName = updated.optString("display_name", displayName),
            email = updated.optString("email", ""),
            bio = updated.optString("bio", bio),
            profilePicture = updated.optString("profile_picture", profilePicture),
            level = 1,
            experience = 0
        )
    }

    override suspend fun updateProfilePicture(imagePath: String): Profile {
        val token = userData?.authToken ?: throw IllegalStateException("No auth token available")
        val updatedProfile = supabaseClient.updateProfile(token, profilePicture = imagePath)
        return Profile(
            displayName = updatedProfile.optString("display_name", ""),
            email = updatedProfile.optString("email", ""),
            bio = updatedProfile.optString("bio", null),
            profilePicture = updatedProfile.optString("profile_picture", imagePath),
            level = 1,
            experience = 0
        )
    }

    override suspend fun logout() {
        userData = null
    }

    fun setUserData(user: User?) {
        userData = user
    }
} 