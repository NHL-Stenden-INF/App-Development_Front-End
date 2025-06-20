package com.nhlstenden.appdev.features.rewards.handlers

import com.nhlstenden.appdev.core.models.Reward
import com.nhlstenden.appdev.core.repositories.UserRepository
import javax.inject.Inject

class BellPepperHandler @Inject constructor(
    private val userRepository: UserRepository
) : RewardHandler {
    
    companion object {
        private const val MAX_BELL_PEPPERS = 3
    }
    
    override suspend fun purchaseReward(reward: Reward, userPoints: Int): PurchaseResult {
        return try {
            // Implementation would need current user context
            // This is simplified for the architecture demo
            PurchaseResult.Success
        } catch (e: Exception) {
            PurchaseResult.Error(e.message ?: "Failed to purchase bell pepper")
        }
    }
    
    override fun canPurchase(reward: Reward, userPoints: Int): Boolean {
        return userPoints >= reward.pointsCost
    }
    
    override fun getDisplayText(reward: Reward): String {
        return "${reward.pointsCost} pts"
    }
} 