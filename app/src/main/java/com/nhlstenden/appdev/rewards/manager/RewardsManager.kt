package com.nhlstenden.appdev.rewards.manager

import android.content.Context
import android.content.res.Resources
import com.nhlstenden.appdev.R
import com.nhlstenden.appdev.rewards.ui.Reward

class RewardsManager(private val context: Context, private val resources: Resources) {

    fun loadRewards(): List<Reward> {
        val titles = resources.getStringArray(R.array.reward_titles)
        val descriptions = resources.getStringArray(R.array.reward_descriptions)
        val points = resources.getIntArray(R.array.reward_points)
        val iconResourceNames = resources.getStringArray(R.array.reward_icons)
        val iconResIds = iconResourceNames.map { resourceName ->
            val resId = resources.getIdentifier(resourceName, "drawable", context.packageName)
            if (resId == 0) R.drawable.ic_achievement else resId
        }
        if (titles.size != descriptions.size || titles.size != points.size || titles.size != iconResIds.size) {
            throw IllegalStateException("Reward resource arrays must have the same length")
        }
        return titles.indices.map { i ->
            Reward(titles[i], descriptions[i], points[i], iconResIds[i], false)
        }
    }
    
    fun getReward(index: Int): Reward {
        val titles = resources.getStringArray(R.array.reward_titles)
        val descriptions = resources.getStringArray(R.array.reward_descriptions)
        val points = resources.getIntArray(R.array.reward_points)
        val iconResourceNames = resources.getStringArray(R.array.reward_icons)
        if (index < 0 || index >= titles.size) {
            throw IndexOutOfBoundsException("Invalid reward index: $index")
        }
        val iconResId = resources.getIdentifier(iconResourceNames[index], "drawable", context.packageName)
        val finalIconResId = if (iconResId == 0) R.drawable.ic_achievement else iconResId
        return Reward(
            titles[index],
            descriptions[index],
            points[index],
            finalIconResId,
            false
        )
    }
    
    fun getRewardCount(): Int {
        return resources.getStringArray(R.array.reward_titles).size
    }
    
    fun updateUnlockedStatus(rewards: List<Reward>, unlockedRewardIds: List<String>): List<Reward> {
        android.util.Log.d("RewardsManager", "Updating unlocked status with IDs: $unlockedRewardIds")
        
        // Convert unlocked IDs to lowercase for case-insensitive comparison
        val normalizedUnlockedIds = unlockedRewardIds.map { it.trim().lowercase() }
        
        return rewards.map { reward ->
            val rewardTitle = reward.title.trim().lowercase()
            android.util.Log.d("RewardsManager", "Checking if reward '$rewardTitle' is in unlocked IDs")
            
            // If the reward title is in the list of unlocked rewards, mark it as unlocked
            val isUnlocked = normalizedUnlockedIds.contains(rewardTitle)
            if (isUnlocked) {
                android.util.Log.d("RewardsManager", "Marking reward as unlocked: ${reward.title}")
                reward.copy(isUnlocked = true)
            } else {
                reward
            }
        }
    }
} 