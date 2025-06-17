package com.nhlstenden.appdev.features.rewards.repositories

import android.util.Log
import com.nhlstenden.appdev.core.repositories.AuthRepository
import com.nhlstenden.appdev.core.repositories.RewardsRepository
import com.nhlstenden.appdev.core.repositories.UserRepository
import com.nhlstenden.appdev.supabase.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class RewardsRepositoryImpl @Inject constructor(
    private val supabaseClient: SupabaseClient,
    private val authRepository: AuthRepository,
    private val userRepository: UserRepository
) : RewardsRepository {

    private val TAG = "RewardsRepositoryImpl"

    override suspend fun getUserUnlockedRewards(): Result<JSONArray> {
        return try {
            val currentUser = authRepository.getCurrentUserSync()
                ?: return Result.failure(Exception("User not authenticated"))
                
            val response = withContext(Dispatchers.IO) {
                supabaseClient.getUserUnlockedRewards(currentUser.id, currentUser.authToken)
            }
            
            if (response.isSuccessful) {
                val body = response.body?.string()
                if (!body.isNullOrEmpty()) {
                    val array = JSONArray(body)
                    Log.d(TAG, "Retrieved ${array.length()} unlocked rewards")
                    Result.success(array)
                } else {
                    Result.success(JSONArray()) // Empty array for no rewards
                }
            } else {
                Result.failure(Exception("Failed to fetch unlocked rewards: ${response.code}"))
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error fetching unlocked rewards", e)
            Result.failure(e)
        }
    }

    override suspend fun unlockReward(rewardId: Int): Result<Unit> {
        return try {
            val currentUser = authRepository.getCurrentUserSync()
                ?: return Result.failure(Exception("User not authenticated"))
                
            val response = withContext(Dispatchers.IO) {
                supabaseClient.unlockReward(currentUser.id, rewardId, currentUser.authToken)
            }
            
            if (response.isSuccessful) {
                Log.d(TAG, "Reward $rewardId unlocked successfully")
                Result.success(Unit)
            } else {
                Result.failure(Exception("Failed to unlock reward: ${response.code}"))
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error unlocking reward", e)
            Result.failure(e)
        }
    }

    override suspend fun purchaseReward(rewardId: Int, cost: Int): Result<Unit> {
        return try {
            val currentUser = authRepository.getCurrentUserSync()
                ?: return Result.failure(Exception("User not authenticated"))
            
            // First, get current user attributes to check points
            val userAttributesResult = userRepository.getUserAttributes(currentUser.id)
            if (userAttributesResult.isFailure) {
                return Result.failure(userAttributesResult.exceptionOrNull() ?: Exception("Failed to get user attributes"))
            }
            
            val userAttributes = userAttributesResult.getOrThrow()
            val currentPoints = userAttributes.optInt("points", 0)
            
            if (currentPoints < cost) {
                return Result.failure(Exception("Insufficient points"))
            }
            
            // Deduct points
            val newPoints = currentPoints - cost
            val updatePointsResult = userRepository.updateUserPoints(currentUser.id, newPoints)
            if (updatePointsResult.isFailure) {
                return Result.failure(updatePointsResult.exceptionOrNull() ?: Exception("Failed to update points"))
            }
            
            // Unlock the reward
            val unlockResult = unlockReward(rewardId)
            if (unlockResult.isFailure) {
                // Rollback points if unlocking failed
                userRepository.updateUserPoints(currentUser.id, currentPoints)
                return unlockResult
            }
            
            Log.d(TAG, "Reward $rewardId purchased successfully for $cost points")
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "Error purchasing reward", e)
            Result.failure(e)
        }
    }
} 