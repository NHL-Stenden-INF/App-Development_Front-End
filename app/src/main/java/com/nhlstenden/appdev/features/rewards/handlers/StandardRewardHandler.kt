package com.nhlstenden.appdev.features.rewards.handlers

import com.nhlstenden.appdev.core.models.Reward
import com.nhlstenden.appdev.core.repositories.RewardsRepository
import javax.inject.Inject

class StandardRewardHandler @Inject constructor(
    private val rewardsRepository: RewardsRepository
) : RewardHandler {
    
    override suspend fun purchaseReward(reward: Reward, userPoints: Int): PurchaseResult {
        return try {
            if (!canPurchase(reward, userPoints)) {
                return PurchaseResult.InsufficientFunds(reward.pointsCost, userPoints)
            }
            
            val result = rewardsRepository.purchaseReward(reward.id, reward.pointsCost)
            if (result.isSuccess) {
                PurchaseResult.Success
            } else {
                PurchaseResult.Error(result.exceptionOrNull()?.message ?: "Purchase failed")
            }
        } catch (e: Exception) {
            PurchaseResult.Error(e.message ?: "Failed to purchase reward")
        }
    }
    
    override fun canPurchase(reward: Reward, userPoints: Int): Boolean {
        return userPoints >= reward.pointsCost && !reward.unlocked
    }
    
    override fun getDisplayText(reward: Reward): String {
        return "${reward.pointsCost} pts"
    }
} 