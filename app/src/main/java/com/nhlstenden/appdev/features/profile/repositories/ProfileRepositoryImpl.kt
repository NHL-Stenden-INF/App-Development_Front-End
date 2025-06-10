package com.nhlstenden.appdev.features.profile.repositories

import android.util.Log
import com.nhlstenden.appdev.R
import com.nhlstenden.appdev.core.models.Profile
import com.nhlstenden.appdev.core.repositories.ProfileRepository
import com.nhlstenden.appdev.supabase.SupabaseClient
import com.nhlstenden.appdev.utils.LevelCalculator
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ProfileRepositoryImpl @Inject constructor(
    private val supabaseClient: SupabaseClient,
    private val authRepository: com.nhlstenden.appdev.core.repositories.AuthRepository
) : ProfileRepository {

    private val TAG = "ProfileRepositoryImpl"

    private suspend fun isJWTExpired(response: okhttp3.Response): Boolean {
        if (response.code == 401) {
            val body = response.body?.string()
            if (body?.contains("JWT expired") == true) {
                Log.w(TAG, "JWT expired detected, clearing session")
                authRepository.handleJWTExpiration()
                return true
            }
        }
        return false
    }

    private suspend fun handleSupabaseCall(call: suspend () -> okhttp3.Response): Result<okhttp3.Response> {
        return try {
            val response = call()
            if (response.isSuccessful) {
                Result.success(response)
            } else {
                if (isJWTExpired(response)) {
                    Result.failure(Exception("Session expired. Please login again."))
                } else {
                    Result.failure(Exception("Request failed: ${response.code}"))
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Supabase call failed", e)
            Result.failure(e)
        }
    }

    override suspend fun getProfile(): Result<Profile> {
        return try {
            val currentUser = authRepository.getCurrentUserSync()
                ?: return Result.failure(Exception("User not logged in"))
            
            // Use the base SupabaseClient methods with auth token instead of UserManager-dependent methods
            val profileJson: JSONObject
            val userAttributes: JSONObject
            
            try {
                profileJson = supabaseClient.fetchProfile(currentUser.authToken)
                userAttributes = supabaseClient.fetchUserAttributes(currentUser.authToken)
            } catch (e: RuntimeException) {
                // Check if this is a JWT expiration error
                if (e.message?.contains("JWT expired") == true) {
                    Log.w(TAG, "JWT expired during profile fetch")
                    authRepository.handleJWTExpiration()
                    return Result.failure(Exception("Session expired. Please login again."))
                } else {
                    throw e
                }
            }
            
            // Fetch unlocked rewards
            val unlockedRewardsResponse = withContext(Dispatchers.IO) {
                supabaseClient.getUserUnlockedRewards(currentUser.id, currentUser.authToken)
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
            } else {
                // Check for JWT expiration on rewards fetch
                if (isJWTExpired(unlockedRewardsResponse)) {
                    return Result.failure(Exception("Session expired. Please login again."))
                }
            }
            
            val xp = userAttributes.optLong("xp", 0L)
            val level = LevelCalculator.calculateLevelFromXp(xp)
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
            val currentUser = authRepository.getCurrentUserSync()
                ?: return Result.failure(Exception("User not logged in"))
            
            // Update the profile using the base method with auth token
            supabaseClient.updateProfile(currentUser.authToken, displayName, bio, profilePicture)
            
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
            val currentUser = authRepository.getCurrentUserSync()
                ?: return Result.failure(Exception("User not logged in"))
            
            // Update the profile picture using the base method with auth token
            supabaseClient.updateProfile(currentUser.authToken, profilePicture = imagePath)
            
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
            val currentUser = authRepository.getCurrentUserSync()
                ?: return Result.failure(Exception("User not logged in"))
            
            // Update the bio using the base method with auth token
            supabaseClient.updateProfile(currentUser.authToken, bio = bio)
            
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
            val currentUser = authRepository.getCurrentUserSync()
                ?: return Result.failure(Exception("User not logged in"))
            
            // Update the display name using the base method with auth token
            supabaseClient.updateProfile(currentUser.authToken, displayName = displayName)
            
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
            // Use AuthRepository instead of UserManager
            authRepository.logout()
            Log.d(TAG, "User logged out successfully")
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "Error during logout", e)
            Result.failure(e)
        }
    }
} 