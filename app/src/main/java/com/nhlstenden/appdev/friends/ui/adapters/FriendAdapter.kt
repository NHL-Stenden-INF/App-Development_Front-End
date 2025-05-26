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
            binding.friendPoints.text = "${friend.progress} pts"
            
            val profilePic = friend.profilePicture
            val invalidPics = listOf(null, "", "null")
            if (profilePic !in invalidPics) {
                if (profilePic!!.startsWith("http")) {
                    Glide.with(binding.friendProfilePicture.context)
                        .load(profilePic)
                        .placeholder(R.drawable.ic_profile_placeholder)
                        .error(R.drawable.ic_profile_placeholder)
                        .circleCrop()
                        .into(binding.friendProfilePicture)
                } else {
                    try {
                        val imageBytes = android.util.Base64.decode(profilePic, android.util.Base64.DEFAULT)
                        val bitmap = android.graphics.BitmapFactory.decodeByteArray(imageBytes, 0, imageBytes.size)
                        binding.friendProfilePicture.setImageBitmap(bitmap)
                    } catch (e: Exception) {
                        binding.friendProfilePicture.setImageResource(R.drawable.ic_profile_placeholder)
                    }
                }
            } else {
                binding.friendProfilePicture.setImageResource(R.drawable.ic_profile_placeholder)
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