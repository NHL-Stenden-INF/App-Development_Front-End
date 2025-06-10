package com.nhlstenden.appdev.utils

import android.content.Context
import android.content.SharedPreferences
import android.util.Log
import com.nhlstenden.appdev.core.repositories.AuthRepository
import com.nhlstenden.appdev.features.profile.repositories.ProfileRepositoryImpl
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Centralized utility for checking reward unlock status.
 * Ensures consistent validation by always checking the database and proper preference management.
 */
@Singleton
class RewardChecker @Inject constructor(
    private val authRepository: AuthRepository,
    private val profileRepository: ProfileRepositoryImpl
) {
    
    companion object {
        const val MUSIC_LOBBY_REWARD_ID = 11
        const val PREFS_NAME = "reward_settings"
        const val MUSIC_LOBBY_KEY = "music_lobby_enabled"
        private const val TAG = "RewardChecker"
    }
    
    /**
     * Check if a specific reward is unlocked for the current user.
     * This method validates against the database to ensure accuracy.
     * 
     * @param rewardId The ID of the reward to check
     * @return True if the reward is unlocked, false otherwise
     */
    suspend fun isRewardUnlocked(rewardId: Int): Boolean {
        return try {
            val currentUser = authRepository.getCurrentUserSync()
            if (currentUser == null) {
                Log.w(TAG, "No current user found when checking reward $rewardId")
                return false
            }
            
            val profileResult = profileRepository.getProfile()
            if (profileResult.isSuccess) {
                val profile = profileResult.getOrThrow()
                val unlockedRewards = profile.unlockedRewardIds ?: emptyList()
                val isUnlocked = unlockedRewards.contains(rewardId)
                
                Log.d(TAG, "Reward $rewardId unlock status: $isUnlocked")
                Log.d(TAG, "User's unlocked rewards: $unlockedRewards")
                
                isUnlocked
            } else {
                Log.e(TAG, "Failed to fetch profile when checking reward $rewardId: ${profileResult.exceptionOrNull()?.message}")
                false
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error checking reward unlock status for reward $rewardId", e)
            false
        }
    }
    
    /**
     * Check if music lobby is both unlocked AND enabled by the user.
     * This combines database validation with user preference.
     * 
     * @param context Context for accessing SharedPreferences
     * @return True if music lobby is unlocked and enabled, false otherwise
     */
    suspend fun isMusicLobbyEnabledAndUnlocked(context: Context): Boolean {
        return withContext(Dispatchers.IO) {
            try {
                // First, check if the reward is actually unlocked in the database
                val isUnlocked = isRewardUnlocked(MUSIC_LOBBY_REWARD_ID)
                
                if (!isUnlocked) {
                    Log.d(TAG, "Music lobby reward not unlocked in database")
                    return@withContext false
                }
                
                // If unlocked, check user's preference setting
                val sharedPrefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
                val isEnabled = sharedPrefs.getBoolean(MUSIC_LOBBY_KEY, true) // Default to true if unlocked
                
                Log.d(TAG, "Music lobby - Unlocked: $isUnlocked, User enabled: $isEnabled")
                
                isEnabled
            } catch (e: Exception) {
                Log.e(TAG, "Error checking music lobby status", e)
                false
            }
        }
    }
    
    /**
     * Update the user's preference for music lobby (only if they have the reward unlocked).
     * This ensures users can't enable features they haven't unlocked.
     * 
     * @param context Context for accessing SharedPreferences
     * @param enabled Whether the user wants to enable music lobby
     * @return True if the preference was updated, false if the user doesn't have the reward unlocked
     */
    suspend fun setMusicLobbyEnabled(context: Context, enabled: Boolean): Boolean {
        return withContext(Dispatchers.IO) {
            try {
                // Verify the user has the reward unlocked before allowing them to enable it
                val isUnlocked = isRewardUnlocked(MUSIC_LOBBY_REWARD_ID)
                
                if (!isUnlocked && enabled) {
                    Log.w(TAG, "Attempted to enable music lobby without having the reward unlocked")
                    return@withContext false
                }
                
                // Update the preference
                val sharedPrefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
                sharedPrefs.edit().putBoolean(MUSIC_LOBBY_KEY, enabled).apply()
                
                Log.d(TAG, "Music lobby preference updated to: $enabled")
                true
            } catch (e: Exception) {
                Log.e(TAG, "Error updating music lobby preference", e)
                false
            }
        }
    }
} 