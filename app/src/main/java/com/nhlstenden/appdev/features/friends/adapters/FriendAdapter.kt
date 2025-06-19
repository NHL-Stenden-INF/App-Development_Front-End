package com.nhlstenden.appdev.features.friends.adapters

import android.graphics.Bitmap
import android.graphics.drawable.Drawable
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.graphics.drawable.toDrawable
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.bumptech.glide.request.target.CustomTarget
import com.bumptech.glide.request.transition.Transition
import com.nhlstenden.appdev.R
import com.nhlstenden.appdev.databinding.ItemFriendBinding
import com.nhlstenden.appdev.features.friends.models.Friend


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
            binding.friendLevel.text = friend.level.toString()
            binding.friendBio.text = friend.bio ?: "No bio available"
            
            // Set circular progress bar
            binding.friendCircularXpBar.progressMax = friend.currentLevelMax.toFloat()
            binding.friendCircularXpBar.setProgressWithAnimation(friend.currentLevelProgress.toFloat(), 800)
            
            val profilePic = friend.profilePicture
            val invalidPics = listOf(null, "", "null")
            if (profilePic !in invalidPics) {
                try {
                    val imageBytes = android.util.Base64.decode(profilePic, android.util.Base64.DEFAULT)
                    Glide.with(binding.friendProfilePicture.context)
                        .asBitmap()
                        .load(imageBytes as ByteArray)
                        .placeholder(R.drawable.ic_profile_placeholder)
                        .into(object : CustomTarget<Bitmap>() {
                            public override fun onResourceReady(
                                resource: Bitmap,
                                transition: Transition<in Bitmap>?
                            ) {
                                val drawable: Drawable = resource.toDrawable(binding.friendProfilePicture.context.resources)
                                binding.friendProfilePicture.background = drawable
//                                    TODO: Replace with actual mask
                                binding.friendProfilePicture.setImageResource(R.drawable.coin_side)
                            }

                            override fun onLoadCleared(placeholder: Drawable?) {
                                binding.friendProfilePicture.background = placeholder
                            }
                        })
                } catch (e: Exception) {
                    binding.friendProfilePicture.setImageResource(R.drawable.ic_profile_placeholder)
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