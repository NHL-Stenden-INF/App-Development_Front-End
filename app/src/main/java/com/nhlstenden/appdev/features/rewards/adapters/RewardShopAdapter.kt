package com.nhlstenden.appdev.features.rewards.adapters

import android.text.SpannableString
import android.text.Spanned
import android.text.style.StrikethroughSpan
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.button.MaterialButton
import com.nhlstenden.appdev.R
import com.nhlstenden.appdev.core.models.Reward
import com.nhlstenden.appdev.core.models.RewardType

class RewardShopAdapter(
    private val rewards: MutableList<Reward>,
    private val onRewardPurchase: (Reward, RewardType) -> Unit
) : RecyclerView.Adapter<RewardShopAdapter.ViewHolder>() {
    
    private var currentPoints: Int = 0
    private var unlockedRewardIds: Set<Int> = emptySet()
    
    fun updatePoints(newPoints: Int) {
        if (currentPoints != newPoints) {
            currentPoints = newPoints
            notifyDataSetChanged()
        }
    }
    
    fun updateUnlockedRewards(newUnlockedIds: Set<Int>) {
        if (unlockedRewardIds != newUnlockedIds) {
            unlockedRewardIds = newUnlockedIds
            updateRewardsUnlockedStatus()
            notifyDataSetChanged()
        }
    }
    
    fun updateRewards(newRewards: List<Reward>) {
        rewards.clear()
        rewards.addAll(newRewards)
        updateRewardsUnlockedStatus()
        notifyDataSetChanged()
    }
    
    private fun updateRewardsUnlockedStatus() {
        for (i in rewards.indices) {
            rewards[i] = rewards[i].copy(unlocked = unlockedRewardIds.contains(rewards[i].id))
        }
    }
    
    private fun canAffordReward(cost: Int): Boolean {
        return currentPoints >= cost
    }
    
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_reward_shop, parent, false)
        return ViewHolder(view)
    }
    
    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(rewards[position])
    }
    
    override fun getItemCount(): Int = rewards.size
    
    inner class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val rewardIcon: ImageView = itemView.findViewById(R.id.rewardIcon)
        private val rewardTitle: TextView = itemView.findViewById(R.id.rewardTitle)
        private val rewardDescription: TextView = itemView.findViewById(R.id.rewardDescription)
        private val unlockButton: MaterialButton = itemView.findViewById(R.id.unlockButton)
        private val comingSoonSticker: FrameLayout = itemView.findViewById(R.id.comingSoonSticker)

        fun bind(reward: Reward) {
            setupRewardDisplay(reward)
            setupUnlockState(reward)
            setupComingSoonSticker(reward)
            setupClickListener(reward)
        }
        
        private fun setupRewardDisplay(reward: Reward) {
            rewardIcon.setImageResource(reward.iconResId)
            rewardTitle.text = reward.title
            rewardDescription.text = reward.description
        }
        
        private fun setupUnlockState(reward: Reward) {
            if (reward.unlocked) {
                setUnlockedState(reward)
            } else {
                setLockedState(reward)
            }
        }
        
        private fun setUnlockedState(reward: Reward) {
            val pointsText = "${reward.pointsCost} pts"
            val spannableString = SpannableString(pointsText)
            spannableString.setSpan(StrikethroughSpan(), 0, pointsText.length, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
            
            unlockButton.text = spannableString
            unlockButton.isEnabled = false
            
            setElementsAlpha(0.5f)
        }
        
        private fun setLockedState(reward: Reward) {
            unlockButton.text = "${reward.pointsCost} pts"
            unlockButton.isEnabled = canAffordReward(reward.pointsCost)
            
            setElementsAlpha(1.0f)
        }
        
        private fun setElementsAlpha(alpha: Float) {
            unlockButton.alpha = alpha
            rewardIcon.alpha = alpha
            rewardTitle.alpha = alpha
            rewardDescription.alpha = alpha
        }
        
        private fun setupComingSoonSticker(reward: Reward) {
            // Show the "Soon" ribbon only for rewards we haven't implemented yet
            // Currently implemented rewards: 2 (Theme Customization), 4 (Profile Badges), and 11 (Course Lobby Music)
            val implementedIds = setOf(2, 4, 11)
            comingSoonSticker.visibility = if (implementedIds.contains(reward.id)) View.GONE else View.VISIBLE
        }
        
        private fun setupClickListener(reward: Reward) {
            unlockButton.setOnClickListener {
                if (!reward.unlocked && canAffordReward(reward.pointsCost)) {
                    val rewardType = RewardType.fromReward(reward)
                    onRewardPurchase(reward, rewardType)
                }
            }
        }
    }
} 