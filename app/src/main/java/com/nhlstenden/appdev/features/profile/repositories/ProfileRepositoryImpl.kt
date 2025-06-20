package com.nhlstenden.appdev.features.profile.repositories

import android.util.Log
import com.nhlstenden.appdev.R
import com.nhlstenden.appdev.core.models.Profile
import com.nhlstenden.appdev.core.repositories.ProfileRepository
import com.nhlstenden.appdev.supabase.*
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

            val profileResult = supabaseClient.fetchProfileOrCreate(currentUser.authToken, "", currentUser.email)

            if (profileResult.isFailure) {
                val ex = profileResult.exceptionOrNull()

                if (ex?.message?.contains("JWT expired") == true) {
                    Log.w(TAG, "JWT expired during profile fetch")
                    authRepository.handleJWTExpiration()
                    return Result.failure(Exception("Session expired. Login again please"))
                }

                return Result.failure(ex ?: Exception("Failed to fetch profile"))
            }

            profileJson = profileResult.getOrThrow()

            val attributeResult = supabaseClient.fetchUserAttributesOrCreate(currentUser.authToken)

            if (attributeResult.isFailure) {
                return Result.failure(attributeResult.exceptionOrNull() ?: Exception("Failed to fetch user attributes"))
            }

            userAttributes = attributeResult.getOrThrow()
            val unlockedRewardsResult = withContext(Dispatchers.IO) { supabaseClient.getUserUnlockedRewards(currentUser.id, currentUser.authToken) }

            if (unlockedRewardsResult.isFailure) {
                Log.e(TAG, "Error fetching unlocked rewards", unlockedRewardsResult.exceptionOrNull())
            }
            
            // Fetch unlocked rewards
            val unlockedRewardsResponse = unlockedRewardsResult.getOrThrow()
            
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
            
            val displayName = profileJson.optString("display_name", "")
            val email = profileJson.optString("email", "")
            
            // Use email as fallback if display name is empty (common after fresh login)
            val finalDisplayName = if (displayName.isNotEmpty()) {
                displayName
            } else if (email.isNotEmpty()) {
                // Extract the part before @ as a fallback
                email.substringBefore("@").takeIf { it.isNotEmpty() } ?: "User"
            } else {
                "User"
            }
            
            val friendMask = profileJson.optString("friend_mask", "circle")

            val profile = Profile(
                displayName = finalDisplayName,
                email = email,
                bio = profileJson.optString("bio", null),
                profilePicture = profileJson.optString("profile_picture", null),
                level = level,
                experience = xp.toInt(),
                friendMask = friendMask,
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