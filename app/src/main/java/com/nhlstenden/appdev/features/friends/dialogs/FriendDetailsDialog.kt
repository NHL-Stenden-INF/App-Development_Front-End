package com.nhlstenden.appdev.features.friends.dialogs

import android.app.Dialog
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.Window
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.recyclerview.widget.LinearLayoutManager
import com.bumptech.glide.Glide
import com.nhlstenden.appdev.R
import com.nhlstenden.appdev.databinding.DialogFriendDetailsBinding
import com.nhlstenden.appdev.features.friends.adapters.CourseProgressAdapter
import com.nhlstenden.appdev.features.friends.adapters.AchievementAdapter
import com.nhlstenden.appdev.features.friends.models.Friend
import com.nhlstenden.appdev.features.friends.models.FriendDetails
import com.nhlstenden.appdev.features.friends.viewmodels.FriendDetailsViewModel
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

@AndroidEntryPoint
class FriendDetailsDialog : DialogFragment() {
    
    private var _binding: DialogFriendDetailsBinding? = null
    private val binding get() = _binding!!
    
    private val viewModel: FriendDetailsViewModel by viewModels()
    private lateinit var courseProgressAdapter: CourseProgressAdapter
    private lateinit var achievementAdapter: AchievementAdapter
    
    private lateinit var friend: Friend
    
    companion object {
        private const val ARG_FRIEND = "arg_friend"
        
        fun newInstance(friend: Friend): FriendDetailsDialog {
            val dialog = FriendDetailsDialog()
            val args = Bundle()
            args.putParcelable(ARG_FRIEND, friend)
            dialog.arguments = args
            return dialog
        }
    }
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        friend = arguments?.getParcelable(ARG_FRIEND) ?: throw IllegalArgumentException("Friend is required")
    }
    
    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val dialog = super.onCreateDialog(savedInstanceState)
        dialog.window?.requestFeature(Window.FEATURE_NO_TITLE)
        return dialog
    }
    
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = DialogFriendDetailsBinding.inflate(inflater, container, false)
        return binding.root
    }
    
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        
        setupUI()
        observeViewModel()
        
        // Load friend details
        viewModel.loadFriendDetails(friend.id)
    }
    
    override fun onStart() {
        super.onStart()
        // Make dialog width smaller to prevent card cutoff at horizontal edges
        dialog?.window?.setLayout(
            (resources.displayMetrics.widthPixels * 0.88).toInt(),
            ViewGroup.LayoutParams.WRAP_CONTENT
        )
        // Minimal padding to avoid edge cutoff
        dialog?.window?.decorView?.setPadding(8, 24, 8, 24)
    }
    
    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
    
    private fun setupUI() {
        // Setup basic friend info
        binding.friendDetailUsername.text = friend.username
        
        // Handle bio with 100 character limit
        val bioText = friend.bio?.trim()?.take(100) ?: "No bio available"
        binding.friendDetailBio.text = bioText
        
        binding.friendDetailLevel.text = "Lv ${friend.level}"
        
        // Load profile picture
        loadProfilePicture(friend.profilePicture)
        
        // Setup basic stats
        binding.friendDetailPoints.text = friend.progress.toString()
        
        // Setup level progress
        binding.friendDetailLevelProgress.max = friend.currentLevelMax
        binding.friendDetailLevelProgress.progress = friend.currentLevelProgress
        binding.friendDetailLevelProgressText.text = "${friend.currentLevelProgress} / ${friend.currentLevelMax} XP"
        
        // Setup RecyclerViews
        courseProgressAdapter = CourseProgressAdapter()
        binding.friendDetailCourseProgress.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = courseProgressAdapter
        }

        achievementAdapter = AchievementAdapter()
        binding.friendDetailAchievements.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = achievementAdapter
        }
        
        // Setup close button
        binding.closeButton.setOnClickListener {
            dismiss()
        }
    }
    
    private fun loadProfilePicture(profilePicture: String?) {
        val profilePic = profilePicture
        val invalidPics = listOf(null, "", "null")
        
        if (profilePic !in invalidPics) {
            if (profilePic!!.startsWith("http")) {
                // Load from URL
                Glide.with(binding.friendDetailProfilePicture.context)
                    .load(profilePic)
                    .placeholder(R.drawable.ic_profile_placeholder)
                    .error(R.drawable.ic_profile_placeholder)
                    .circleCrop()
                    .into(binding.friendDetailProfilePicture)
            } else {
                // Try to load as base64
                try {
                    val imageBytes = android.util.Base64.decode(profilePic, android.util.Base64.DEFAULT)
                    Glide.with(binding.friendDetailProfilePicture.context)
                        .load(imageBytes)
                        .placeholder(R.drawable.ic_profile_placeholder)
                        .error(R.drawable.ic_profile_placeholder)
                        .circleCrop()
                        .into(binding.friendDetailProfilePicture)
                } catch (e: Exception) {
                    binding.friendDetailProfilePicture.setImageResource(R.drawable.ic_profile_placeholder)
                }
            }
        } else {
            binding.friendDetailProfilePicture.setImageResource(R.drawable.ic_profile_placeholder)
        }
    }
    
    private fun observeViewModel() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.friendDetails.collect { details ->
                    details?.let { updateUIWithDetails(it) }
                }
            }
        }
        
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.isLoading.collect { isLoading ->
                    // Handle loading state if needed
                }
            }
        }
        
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.error.collect { error ->
                    error?.let {
                        // Handle error state if needed
                    }
                }
            }
        }
    }
    
    private fun updateUIWithDetails(details: FriendDetails) {
        Log.d("FriendDetailsDialog", "Updating UI with details: courses=${details.courseProgress.size}")
        
        // Update username if we got real data
        if (details.username != "Friend User") {
            binding.friendDetailUsername.text = details.username
        }
        
        // Update bio if available
        details.bio?.let { bio ->
            val formattedBio = bio.trim().take(100)
            binding.friendDetailBio.text = formattedBio
        }
        
        // Update profile picture if available
        details.profilePicture?.let { profilePic ->
            loadProfilePicture(profilePic)
        }
        
        // Update stats
        binding.friendDetailPoints.text = formatNumber(details.totalPoints)
        binding.friendDetailStreak.text = details.streakDays.toString()
        
        // Update level display
        binding.friendDetailLevel.text = "Lv ${details.level}"
        
        // Update level progress
        binding.friendDetailLevelProgress.max = details.currentLevelMax
        binding.friendDetailLevelProgress.progress = details.currentLevelProgress
        binding.friendDetailLevelProgressText.text = "${details.currentLevelProgress} / ${details.currentLevelMax} XP"
        
        // Update join date
        details.joinDate?.let { joinDate ->
            binding.friendDetailJoinDate.text = formatJoinDate(joinDate)
        }
        
        // Update course progress
        Log.d("FriendDetailsDialog", "Course progress data: ${details.courseProgress}")
        courseProgressAdapter.submitList(details.courseProgress)
        
        // Update achievements
        achievementAdapter.submitList(details.achievements)
        
        // Hide sections if no data
        if (details.courseProgress.isEmpty()) {
            Log.d("FriendDetailsDialog", "No course progress data, hiding course progress card")
            binding.courseProgressCard.visibility = View.GONE
        } else {
            Log.d("FriendDetailsDialog", "Showing course progress card with ${details.courseProgress.size} courses")
            binding.courseProgressCard.visibility = View.VISIBLE
        }
        
        if (details.achievements.isEmpty()) {
            binding.achievementsCard.visibility = View.GONE
        } else {
            binding.achievementsCard.visibility = View.VISIBLE
        }
    }
    
    private fun formatNumber(number: Int): String {
        return when {
            number >= 1000000 -> String.format("%.1fM", number / 1000000.0)
            number >= 1000 -> String.format("%.1fK", number / 1000.0)
            else -> number.toString()
        }
    }
    
    private fun formatJoinDate(dateString: String): String {
        // Handle unknown case
        if (dateString == "Unknown" || dateString.isBlank()) {
            return "Never"
        }
        
        return try {
            val inputFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
            val lastActiveDate = inputFormat.parse(dateString)
            val currentDate = Date()
            
            if (lastActiveDate != null) {
                val diffInMillis = currentDate.time - lastActiveDate.time
                val diffInDays = (diffInMillis / (1000 * 60 * 60 * 24)).toInt()
                
                when {
                    diffInDays == 0 -> "Today"
                    diffInDays == 1 -> "1 day ago"
                    diffInDays <= 30 -> "$diffInDays days ago"
                    else -> "30+ days ago"
                }
            } else {
                "Never"
            }
        } catch (e: Exception) {
            Log.w("FriendDetailsDialog", "Could not parse date: $dateString", e)
            "Never"
        }
    }
} 