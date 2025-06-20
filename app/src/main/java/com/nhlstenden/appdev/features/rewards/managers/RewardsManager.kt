package com.nhlstenden.appdev.features.rewards.managers

import android.content.Context
import android.content.res.Resources
import com.nhlstenden.appdev.R
import com.nhlstenden.appdev.core.models.Reward
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class RewardsManager @Inject constructor(
    @ApplicationContext private val context: Context
) {
    
    private val resources: Resources = context.resources
    
    private data class RewardArrays(
        val ids: IntArray,
        val titles: Array<String>,
        val descriptions: Array<String>,
        val points: IntArray,
        val iconResourceNames: Array<String>
    ) {
        init {
            validateArraySizes()
        }
        
        private fun validateArraySizes() {
            val sizes = arrayOf(ids.size, titles.size, descriptions.size, points.size, iconResourceNames.size)
            if (sizes.distinct().size > 1) {
                throw IllegalStateException("All reward resource arrays must have the same length")
            }
        }
        
        val size: Int get() = titles.size
    }
    
    private fun loadRewardArrays(): RewardArrays {
        return RewardArrays(
            ids = resources.getIntArray(R.array.reward_ids),
            titles = resources.getStringArray(R.array.reward_titles),
            descriptions = resources.getStringArray(R.array.reward_descriptions),
            points = resources.getIntArray(R.array.reward_points),
            iconResourceNames = resources.getStringArray(R.array.reward_icons)
        )
    }
    
    private fun getIconResourceId(resourceName: String): Int {
        val resId = resources.getIdentifier(resourceName, "drawable", context.packageName)
        return if (resId == 0) R.drawable.ic_achievement else resId
    }
    
    fun loadRewards(): List<Reward> {
        val arrays = loadRewardArrays()
        
        val implementedIds = setOf(2, 4, 11)
        val list = (0 until arrays.size).map { index ->
            createReward(arrays, index)
        }
        return list.sortedWith(compareByDescending<Reward> { implementedIds.contains(it.id) }.thenBy { it.id })
    }
    
    fun getReward(index: Int): Reward {
        val arrays = loadRewardArrays()
        
        if (index < 0 || index >= arrays.size) {
            throw IndexOutOfBoundsException("Invalid reward index: $index (size: ${arrays.size})")
        }
        
        return createReward(arrays, index)
    }
    
    fun getRewardCount(): Int {
        return resources.getStringArray(R.array.reward_titles).size
    }
    
    fun updateUnlockedStatus(rewards: List<Reward>, unlockedRewardIds: Set<Int>): List<Reward> {
        return rewards.map { reward ->
            reward.copy(unlocked = unlockedRewardIds.contains(reward.id))
        }
    }
    
    private fun createReward(arrays: RewardArrays, index: Int): Reward {
        return Reward(
            id = arrays.ids[index],
            title = arrays.titles[index],
            description = arrays.descriptions[index],
            pointsCost = arrays.points[index],
            iconResId = getIconResourceId(arrays.iconResourceNames[index]),
            unlocked = false
        )
    }
} 