package com.nhlstenden.appdev.features.rewards.handlers

import com.nhlstenden.appdev.core.models.Reward

interface RewardHandler {
    suspend fun purchaseReward(reward: Reward, userPoints: Int): PurchaseResult
    fun canPurchase(reward: Reward, userPoints: Int): Boolean
    fun getDisplayText(reward: Reward): String
}

sealed class PurchaseResult {
    object Success : PurchaseResult()
    data class Error(val message: String) : PurchaseResult()
    data class InsufficientFunds(val required: Int, val available: Int) : PurchaseResult()
} 