package com.nhlstenden.appdev.core.models

sealed class RewardType {
    object StandardUnlock : RewardType()
    object BellPepper : RewardType()
    object ThemeCustomization : RewardType()
    data class Special(val handlerId: String) : RewardType()
    
    companion object {
        fun fromReward(reward: Reward): RewardType {
            return when {
                reward.title == "Extra Life (Bell Pepper)" -> BellPepper
                reward.id == 2 -> ThemeCustomization
                reward.id == 11 -> Special("course_lobby_music")
                else -> StandardUnlock
            }
        }
    }
} 