package com.nhlstenden.appdev.features.profile.adapters

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.nhlstenden.appdev.databinding.ItemAchievementBinding
import com.nhlstenden.appdev.core.models.Achievement

class AchievementAdapter : ListAdapter<Achievement, AchievementAdapter.AchievementViewHolder>(AchievementDiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): AchievementViewHolder {
        val binding = ItemAchievementBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return AchievementViewHolder(binding)
    }

    override fun onBindViewHolder(holder: AchievementViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    class AchievementViewHolder(
        private val binding: ItemAchievementBinding
    ) : RecyclerView.ViewHolder(binding.root) {
        
        fun bind(achievement: Achievement) {
            binding.achievementTitle.text = achievement.title
            binding.achievementDescription.text = achievement.description
            binding.achievementIcon.setImageResource(achievement.iconResId)
            
            // Set alpha based on whether achievement is unlocked
            binding.root.alpha = if (achievement.unlocked) 1.0f else 0.5f
        }
    }

    private class AchievementDiffCallback : DiffUtil.ItemCallback<Achievement>() {
        override fun areItemsTheSame(oldItem: Achievement, newItem: Achievement): Boolean {
            return oldItem.id == newItem.id
        }

        override fun areContentsTheSame(oldItem: Achievement, newItem: Achievement): Boolean {
            return oldItem == newItem
        }
    }
} 