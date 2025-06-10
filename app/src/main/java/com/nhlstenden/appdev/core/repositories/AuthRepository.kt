package com.nhlstenden.appdev.core.repositories

import com.nhlstenden.appdev.core.models.User
import kotlinx.coroutines.flow.Flow

interface AuthRepository {
    fun getCurrentUser(): Flow<User?>
    fun getCurrentUserSync(): User?
    suspend fun login(email: String, password: String): Result<User>
    suspend fun register(email: String, password: String, displayName: String): Result<User>
    suspend fun logout(): Result<Unit>
    fun isLoggedIn(): Boolean
    suspend fun refreshUserData(): Result<User>
    suspend fun handleJWTExpiration()
} 