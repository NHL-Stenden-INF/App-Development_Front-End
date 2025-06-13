package com.nhlstenden.appdev.core.repositories

import com.nhlstenden.appdev.core.models.Achievement
import org.json.JSONArray

interface AchievementRepository {
    suspend fun getUserUnlockedAchievements(userId: String): Result<List<Int>>
    suspend fun unlockAchievement(userId: String, achievementId: Int): Result<Unit>
    suspend fun getAllAchievements(): List<Achievement>
    suspend fun checkAndUnlockAchievements(userId: String): Result<List<Achievement>>
} 