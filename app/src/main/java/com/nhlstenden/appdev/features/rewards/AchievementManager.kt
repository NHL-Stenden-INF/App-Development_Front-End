package com.nhlstenden.appdev.features.rewards

import android.content.Context
import android.util.Log
import com.nhlstenden.appdev.core.repositories.AchievementRepository
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AchievementManager @Inject constructor(
    private val achievementRepository: AchievementRepository,
    @ApplicationContext private val context: Context
) {
    private val TAG = "AchievementManager"

    fun checkAchievementsAfterTaskCompletion(userId: String, courseId: String) {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val logMessage = if (courseId == "all") {
                    "🎯 AchievementManager: Checking all achievements for user $userId on login"
                } else {
                    "🎯 AchievementManager: Checking achievements for user $userId after completing task in course $courseId"
                }
                Log.d(TAG, logMessage)
                
                Log.d(TAG, "🎯 AchievementManager: Calling achievementRepository.checkAndUnlockAchievements()")
                val newAchievements = achievementRepository.checkAndUnlockAchievements(userId)
                
                if (newAchievements.isSuccess) {
                    val unlockedAchievements = newAchievements.getOrThrow()
                    if (unlockedAchievements.isNotEmpty()) {
                        Log.d(TAG, "🎯 AchievementManager: Unlocked ${unlockedAchievements.size} new achievements")
                        // You can add notification logic here if needed
                        unlockedAchievements.forEach { achievement ->
                            Log.d(TAG, "🎯 AchievementManager: Achievement unlocked: ${achievement.title}")
                        }
                    } else {
                        Log.d(TAG, "🎯 AchievementManager: No new achievements to unlock")
                    }
                } else {
                    Log.e(TAG, "🎯 AchievementManager: Failed to check achievements: ${newAchievements.exceptionOrNull()?.message}")
                }
            } catch (e: Exception) {
                Log.e(TAG, "🎯 AchievementManager: Error checking achievements", e)
            }
        }
    }

    fun checkStreakAchievement(userId: String) {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                Log.d(TAG, "🎯 AchievementManager: Checking streak achievement specifically for user $userId")
                Log.d(TAG, "🎯 AchievementManager: Calling achievementRepository.checkAndUnlockAchievements() for streak check")
                val newAchievements = achievementRepository.checkAndUnlockAchievements(userId)
                
                if (newAchievements.isSuccess) {
                    val unlockedAchievements = newAchievements.getOrThrow()
                    Log.d(TAG, "🎯 AchievementManager: Streak check returned ${unlockedAchievements.size} achievements")
                    unlockedAchievements.forEach { achievement ->
                        if (achievement.id == "5") { // Streak Master achievement
                            Log.d(TAG, "🎯 AchievementManager: Streak achievement unlocked: ${achievement.title}")
                        } else {
                            Log.d(TAG, "🎯 AchievementManager: Non-streak achievement found: ${achievement.title}")
                        }
                    }
                } else {
                    Log.e(TAG, "🎯 AchievementManager: Streak check failed: ${newAchievements.exceptionOrNull()?.message}")
                }
            } catch (e: Exception) {
                Log.e(TAG, "🎯 AchievementManager: Error checking streak achievement", e)
            }
        }
    }
} 