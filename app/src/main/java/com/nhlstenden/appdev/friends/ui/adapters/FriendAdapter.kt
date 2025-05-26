package com.nhlstenden.appdev.friends.ui.adapters

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.nhlstenden.appdev.R
import com.nhlstenden.appdev.databinding.ItemFriendBinding
import com.nhlstenden.appdev.friends.domain.models.Friend

class FriendAdapter(
    private val onFriendClick: (Friend) -> Unit
) : ListAdapter<Friend, FriendAdapter.FriendViewHolder>(FriendDiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): FriendViewHolder {
        val binding = ItemFriendBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return FriendViewHolder(binding)
    }

    override fun onBindViewHolder(holder: FriendViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    inner class FriendViewHolder(
        private val binding: ItemFriendBinding
    ) : RecyclerView.ViewHolder(binding.root) {
        
        fun bind(friend: Friend) {
            binding.friendUsername.text = friend.username
            
            // Set points and progress bar
            binding.friendProgressBar.progress = friend.progress
            binding.friendProgressText.text = "${friend.progress} points"
            
            // Load profile picture using Glide
            val profilePicUrl = friend.profilePicture
            val context = binding.friendProfilePicture.context
            
            if (!profilePicUrl.isNullOrEmpty() && profilePicUrl != "null") {
                Glide.with(context)
                    .load(profilePicUrl)
                    .placeholder(R.drawable.ic_profile_placeholder) // Replace with your placeholder drawable
                    .error(R.drawable.ic_profile_placeholder) // Replace with your error drawable
                    .circleCrop() // Optional: if you want circular images
                    .into(binding.friendProfilePicture)
            } else {
                // Load a default placeholder if no profile picture is available
                Glide.with(context)
                    .load(R.drawable.ic_profile_placeholder) // Replace with your placeholder drawable
                    .circleCrop() // Optional: if you want circular images
                    .into(binding.friendProfilePicture)
            }
            
            binding.root.setOnClickListener { onFriendClick(friend) }
        }
    }

    private class FriendDiffCallback : DiffUtil.ItemCallback<Friend>() {
        override fun areItemsTheSame(oldItem: Friend, newItem: Friend): Boolean {
            return oldItem.id == newItem.id
        }

        override fun areContentsTheSame(oldItem: Friend, newItem: Friend): Boolean {
            return oldItem == newItem
        }
    }
} 