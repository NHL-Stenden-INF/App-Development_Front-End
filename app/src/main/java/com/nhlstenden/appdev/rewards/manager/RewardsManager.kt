package com.nhlstenden.appdev.rewards.manager

import android.content.Context
import android.content.res.Resources
import com.nhlstenden.appdev.R
import com.nhlstenden.appdev.rewards.ui.Reward

class RewardsManager(private val context: Context, private val resources: Resources) {

    fun loadRewards(): List<Reward> {
        val ids = resources.getIntArray(R.array.reward_ids)
        val titles = resources.getStringArray(R.array.reward_titles)
        val descriptions = resources.getStringArray(R.array.reward_descriptions)
        val points = resources.getIntArray(R.array.reward_points)
        val iconResourceNames = resources.getStringArray(R.array.reward_icons)
        val iconResIds = iconResourceNames.map { resourceName ->
            val resId = resources.getIdentifier(resourceName, "drawable", context.packageName)
            if (resId == 0) R.drawable.ic_achievement else resId
        }
        if (ids.size != titles.size || titles.size != descriptions.size || titles.size != points.size || titles.size != iconResIds.size) {
            throw IllegalStateException("Reward resource arrays must have the same length")
        }
        return titles.indices.map { i ->
            Reward(ids[i], titles[i], descriptions[i], points[i], iconResIds[i], false)
        }
    }
    
    fun getReward(index: Int): Reward {
        val ids = resources.getIntArray(R.array.reward_ids)
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
            ids[index],
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
    
    fun updateUnlockedStatus(rewards: List<Reward>, unlockedRewardIds: List<Int>): List<Reward> {
        android.util.Log.d("RewardsManager", "Updating unlocked status with IDs: $unlockedRewardIds")
        
        val normalizedUnlockedIds = unlockedRewardIds
        
        return rewards.map { reward ->
            android.util.Log.d("RewardsManager", "Checking if reward id '${reward.id}' is in unlocked IDs")
            val isUnlocked = normalizedUnlockedIds.contains(reward.id)
            if (isUnlocked) {
                android.util.Log.d("RewardsManager", "Marking reward as unlocked: ${reward.title}")
                reward.copy(isUnlocked = true)
            } else {
                reward
            }
        }
    }
} 