package com.nhlstenden.appdev.features.profile.repositories

import android.util.Log
import com.nhlstenden.appdev.R
import com.nhlstenden.appdev.core.models.Profile
import com.nhlstenden.appdev.core.repositories.ProfileRepository
import com.nhlstenden.appdev.supabase.SupabaseClient
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ProfileRepositoryImpl @Inject constructor(
    private val supabaseClient: SupabaseClient
) : ProfileRepository {

    private val TAG = "ProfileRepositoryImpl"

    override suspend fun getProfile(): Result<Profile> {
        return try {
            val profileJson = supabaseClient.getProfileForCurrentUser()
            val userAttributes = supabaseClient.getUserAttributesForCurrentUser()
            
            // Fetch unlocked rewards
            val unlockedRewardsResponse = withContext(Dispatchers.IO) {
                supabaseClient.getUserUnlockedRewardsForCurrentUser()
            }
            
            val unlockedRewardIds = mutableListOf<Int>()
            if (unlockedRewardsResponse.isSuccessful) {
                val body = unlockedRewardsResponse.body?.string()
                if (!body.isNullOrEmpty()) {
                    val arr = JSONArray(body)
                    for (i in 0 until arr.length()) {
                        val obj = arr.getJSONObject(i)
                        unlockedRewardIds.add(obj.optInt("reward_id"))
                    }
                }
            }
            
            val xp = userAttributes.optLong("xp", 0L)
            val level = supabaseClient.calculateLevelFromXp(xp)
            val bellPeppers = userAttributes.optInt("bell_peppers", 0)
            
            val profile = Profile(
                displayName = profileJson.optString("display_name", ""),
                email = profileJson.optString("email", ""),
                bio = profileJson.optString("bio", null),
                profilePicture = profileJson.optString("profile_picture", null),
                level = level,
                experience = xp.toInt(),
                unlockedRewardIds = unlockedRewardIds,
                bellPeppers = bellPeppers
            )
            
            Log.d(TAG, "Profile loaded successfully for ${profile.displayName}")
            Result.success(profile)
        } catch (e: Exception) {
            Log.e(TAG, "Error loading profile", e)
            Result.failure(e)
        }
    }

    override suspend fun updateProfile(displayName: String, bio: String?, profilePicture: String?): Result<Profile> {
        return try {
            // Update the profile
            supabaseClient.updateProfileForCurrentUser(
                displayName = displayName,
                bio = bio,
                profilePicture = profilePicture
            )
            
            // Fetch the complete profile data after update to ensure we have all current data
            val profileResult = getProfile()
            if (profileResult.isSuccess) {
                Log.d(TAG, "Profile updated successfully")
                profileResult
            } else {
                Log.e(TAG, "Failed to fetch profile after profile update")
                profileResult
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error updating profile", e)
            Result.failure(e)
        }
    }

    override suspend fun updateProfilePicture(imagePath: String): Result<Profile> {
        return try {
            // Update the profile picture
            supabaseClient.updateProfileForCurrentUser(profilePicture = imagePath)
            
            // Fetch the complete profile data after update to ensure we have all current data
            val profileResult = getProfile()
            if (profileResult.isSuccess) {
                Log.d(TAG, "Profile picture updated successfully")
                profileResult
            } else {
                Log.e(TAG, "Failed to fetch profile after profile picture update")
                profileResult
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error updating profile picture", e)
            Result.failure(e)
        }
    }

    override suspend fun updateBio(bio: String): Result<Profile> {
        return try {
            // Update the bio
            supabaseClient.updateProfileForCurrentUser(bio = bio)
            
            // Fetch the complete profile data after update to ensure we have all current data
            val profileResult = getProfile()
            if (profileResult.isSuccess) {
                Log.d(TAG, "Bio updated successfully")
                profileResult
            } else {
                Log.e(TAG, "Failed to fetch profile after bio update")
                profileResult
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error updating bio", e)
            Result.failure(e)
        }
    }

    override suspend fun updateDisplayName(displayName: String): Result<Profile> {
        return try {
            // Update the display name
            supabaseClient.updateProfileForCurrentUser(displayName = displayName)
            
            // Fetch the complete profile data after update to ensure we have all current data
            val profileResult = getProfile()
            if (profileResult.isSuccess) {
                Log.d(TAG, "Display name updated successfully")
                profileResult
            } else {
                Log.e(TAG, "Failed to fetch profile after display name update")
                profileResult
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error updating display name", e)
            Result.failure(e)
        }
    }

    override suspend fun logout(): Result<Unit> {
        return try {
            // Clear UserManager
            com.nhlstenden.appdev.core.utils.UserManager.logout()
            Log.d(TAG, "User logged out successfully")
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "Error during logout", e)
            Result.failure(e)
        }
    }
} 