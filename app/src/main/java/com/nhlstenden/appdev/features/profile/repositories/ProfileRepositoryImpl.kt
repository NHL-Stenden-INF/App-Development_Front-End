package com.nhlstenden.appdev.features.profile.repositories

import com.nhlstenden.appdev.R
import com.nhlstenden.appdev.core.models.Profile
import com.nhlstenden.appdev.core.repositories.ProfileRepository
import com.nhlstenden.appdev.core.models.User as CoreUser
import com.nhlstenden.appdev.supabase.User
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

@Singleton
class ProfileRepositoryImpl @Inject constructor(
    private val supabaseClient: com.nhlstenden.appdev.supabase.SupabaseClient
) : ProfileRepository {
    private val defaultProfilePicture = R.drawable.ic_profile_placeholder
    private var userData: User? = null

    override suspend fun getProfile(): Profile {
        val token = userData?.authToken ?: throw IllegalStateException("No auth token available")
        val profileJson = supabaseClient.fetchProfile(token)
        // Fetch unlocked rewards
        val userId = userData?.id?.toString() ?: throw IllegalStateException("No user ID available")
        val unlockedRewardsResponse = withContext(Dispatchers.IO) {
            supabaseClient.getUserUnlockedRewards(userId, token)
        }
        val unlockedRewardIds = mutableListOf<Int>()
        if (unlockedRewardsResponse.isSuccessful) {
            val body = unlockedRewardsResponse.body?.string()
            if (!body.isNullOrEmpty()) {
                val arr = org.json.JSONArray(body)
                for (i in 0 until arr.length()) {
                    val obj = arr.getJSONObject(i)
                    unlockedRewardIds.add(obj.optInt("reward_id"))
                }
            }
        }
        return Profile(
            displayName = profileJson.optString("display_name", ""),
            email = profileJson.optString("email", ""),
            bio = profileJson.optString("bio", null),
            profilePicture = profileJson.optString("profile_picture", null),
            level = 1,
            experience = 0,
            unlockedRewardIds = unlockedRewardIds
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

    fun setUserData(user: CoreUser?) {
        if (user == null) {
            userData = null
        } else {
            userData = com.nhlstenden.appdev.supabase.User(
                authToken = user.authToken,
                id = try { java.util.UUID.fromString(user.id) } catch (e: Exception) { java.util.UUID(0, 0) },
                username = user.username,
                email = user.email,
                points = 0,
                friends = ArrayList(user.friends),
                achievements = ArrayList(),
                profilePicture = user.profilePicture ?: ""
            )
        }
    }
} 