package com.nhlstenden.appdev.core.repositories

import org.json.JSONArray
import okhttp3.Response

interface RewardsRepository {
    suspend fun getUserUnlockedRewards(): Result<JSONArray>
    suspend fun unlockReward(rewardId: Int): Result<Unit>
    suspend fun purchaseReward(rewardId: Int, cost: Int): Result<Unit>
} 