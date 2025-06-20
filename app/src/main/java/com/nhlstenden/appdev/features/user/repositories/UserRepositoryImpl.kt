package com.nhlstenden.appdev.features.user.repositories

import android.os.Build
import android.util.Log
import androidx.annotation.RequiresApi
import com.nhlstenden.appdev.core.repositories.AuthRepository
import com.nhlstenden.appdev.core.repositories.UserRepository
import com.nhlstenden.appdev.supabase.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class UserRepositoryImpl @Inject constructor(
    private val supabaseClient: SupabaseClient,
    private val authRepository: AuthRepository
) : UserRepository {

    private val TAG = "UserRepositoryImpl"
    override var cachedProfile: JSONObject? = null
    
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

    override suspend fun getUserAttributes(userId: String): Result<JSONObject> {
        return try {
            val currentUser = authRepository.getCurrentUserSync()
                ?: return Result.failure(Exception("User not authenticated"))
                
            val responseResult = supabaseClient.getUserAttributes(userId, currentUser.authToken)

            if (responseResult.isFailure) {
                val ex = responseResult.exceptionOrNull() ?: Exception("Failed to fetch user attributes")
                return Result.failure(ex)
            }
            
            val response = responseResult.getOrThrow()
            
            if (response.isSuccessful) {
                val body = response.body?.string()
                if (!body.isNullOrEmpty()) {
                    val array = JSONArray(body)
                    if (array.length() > 0) {
                        val profile = array.getJSONObject(0)
                        cachedProfile = profile
                        return Result.success(profile)
                    } else {
                        return Result.failure(Exception("User attributes not found"))
                    }
                } else {
                    return Result.failure(Exception("Empty response body"))
                }
            } else {
                if (isJWTExpired(response)) {
                    return Result.failure(Exception("Session expired. Please login again."))
                } else {
                    return Result.failure(Exception("Failed to fetch user attributes: ${response.code}"))
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error fetching user attributes", e)
            return Result.failure(e)
        }
    }

    override suspend fun updateUserPoints(userId: String, points: Int): Result<Unit> {
        return try {
            val currentUser = authRepository.getCurrentUserSync()
                ?: return Result.failure(Exception("User not authenticated"))
                
            val responseResult = supabaseClient.updateUserPoints(userId, points, currentUser.authToken)

            if (responseResult.isFailure) return Result.failure(responseResult.exceptionOrNull()!!)

            val response = responseResult.getOrThrow()
            
            if (response.isSuccessful) {
                Log.d(TAG, "User points updated successfully")
                Result.success(Unit)
            } else {
                if (isJWTExpired(response)) {
                    Result.failure(Exception("Session expired. Please login again."))
                } else {
                    Result.failure(Exception("Failed to update points: ${response.code}"))
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error updating user points", e)
            Result.failure(e)
        }
    }

    override suspend fun updateUserXp(userId: String, xp: Long): Result<Unit> {
        return try {
            val currentUser = authRepository.getCurrentUserSync()
                ?: return Result.failure(Exception("User not authenticated"))
                
            val responseResult = supabaseClient.updateUserXp(userId, xp.toInt(), currentUser.authToken)

            if (responseResult.isFailure) return Result.failure(responseResult.exceptionOrNull()!!)

            val response = responseResult.getOrThrow()
            
            if (response.isSuccessful) {
                Log.d(TAG, "User XP updated successfully")
                Result.success(Unit)
            } else {
                Result.failure(Exception("Failed to update XP: ${response.code}"))
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error updating user XP", e)
            Result.failure(e)
        }
    }

    override suspend fun updateUserBellPeppers(userId: String, bellPeppers: Int): Result<Unit> {
        return try {
            val currentUser = authRepository.getCurrentUserSync()
                ?: return Result.failure(Exception("User not authenticated"))
                
            val responseResult = supabaseClient.updateUserBellPeppers(userId, bellPeppers, currentUser.authToken)

            if (responseResult.isFailure) return Result.failure(responseResult.exceptionOrNull()!!)

            val response = responseResult.getOrThrow()
            
            if (response.isSuccessful) {
                Log.d(TAG, "User bell peppers updated successfully")
                Result.success(Unit)
            } else {
                if (isJWTExpired(response)) {
                    Result.failure(Exception("Session expired. Please login again."))
                } else {
                    Result.failure(Exception("Failed to update bell peppers: ${response.code}"))
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error updating user bell peppers", e)
            Result.failure(e)
        }
    }

    override suspend fun updateUserStreak(userId: String, streak: Int): Result<Unit> {
        return try {
            val currentUser = authRepository.getCurrentUserSync()
                ?: return Result.failure(Exception("User not authenticated"))
                
            val responseResult = supabaseClient.updateUserStreak(userId, streak, currentUser.authToken)

            if (responseResult.isFailure) return Result.failure(responseResult.exceptionOrNull()!!)

            val response = responseResult.getOrThrow()
            
            if (response.isSuccessful) {
                Log.d(TAG, "User streak updated successfully")
                Result.success(Unit)
            } else {
                Result.failure(Exception("Failed to update streak: ${response.code}"))
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error updating user streak", e)
            Result.failure(e)
        }
    }

    override suspend fun updateUserLastTaskDate(userId: String, date: String): Result<Unit> {
        return try {
            val currentUser = authRepository.getCurrentUserSync()
                ?: return Result.failure(Exception("User not authenticated"))
                
            val responseResult = supabaseClient.updateUserLastTaskDate(userId, date, currentUser.authToken)

            if (responseResult.isFailure) return Result.failure(responseResult.exceptionOrNull()!!)

            val response = responseResult.getOrThrow()
            
            if (response.isSuccessful) {
                Log.d(TAG, "User last task date updated successfully")
                Result.success(Unit)
            } else {
                Result.failure(Exception("Failed to update last task date: ${response.code}"))
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error updating user last task date", e)
            Result.failure(e)
        }
    }

    override suspend fun updateUserOpenedDaily(userId: String, date: String): Result<Unit> {
        return try {
            val currentUser = authRepository.getCurrentUserSync()
                ?: return Result.failure(Exception("User not authenticated"))
                
            val responseResult = supabaseClient.updateUserOpenedDaily(userId, date, currentUser.authToken)

            if (responseResult.isFailure) return Result.failure(responseResult.exceptionOrNull()!!)

            val response = responseResult.getOrThrow()
            
            if (response.isSuccessful) {
                Log.d(TAG, "User opened daily updated successfully")
                Result.success(Unit)
            } else {
                Result.failure(Exception("Failed to update opened daily: ${response.code}"))
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error updating user opened daily", e)
            Result.failure(e)
        }
    }

    @RequiresApi(Build.VERSION_CODES.O)
    override suspend fun updateUserDailyChallenge(userId: String): Result<Unit> {
        return try {
            val currentUser = authRepository.getCurrentUserSync()
                ?: return Result.failure(Exception("User not authenticated"))

            Log.d(TAG, "Updating daily challenge")

            val result = supabaseClient.updateUserDailyChallenge(userId, currentUser.authToken)
            val response = result.getOrNull()

            if (response != null && response.isSuccessful) {
                Log.d(TAG, "User daily challenge updated successfully")
                Result.success(Unit)
            } else {
                Log.d(TAG, "Failed to update daily challenge: ${response?.body?.toString()}")
                Result.failure(Exception("Failed to update daily challenge: ${response?.code ?: "unknown"}"))
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error updating daily challenge", e)
            Result.failure(e)
        }
    }
} 