package com.nhlstenden.appdev.core.repositories

import com.nhlstenden.appdev.core.models.User
import org.json.JSONObject

interface UserRepository {
    suspend fun getUserAttributes(userId: String): Result<JSONObject>
    suspend fun updateUserPoints(userId: String, points: Int): Result<Unit>
    suspend fun updateUserXp(userId: String, xp: Long): Result<Unit>
    suspend fun updateUserBellPeppers(userId: String, bellPeppers: Int): Result<Unit>
    suspend fun updateUserStreak(userId: String, streak: Int): Result<Unit>
    suspend fun updateUserLastTaskDate(userId: String, date: String): Result<Unit>
    suspend fun updateUserOpenedDaily(userId: String, date: String): Result<Unit>
} 