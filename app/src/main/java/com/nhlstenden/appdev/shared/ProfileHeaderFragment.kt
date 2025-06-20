package com.nhlstenden.appdev.shared

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.lifecycleScope
import com.bumptech.glide.Glide
import com.nhlstenden.appdev.R
import com.nhlstenden.appdev.databinding.FragmentProfileHeaderBinding
import com.nhlstenden.appdev.features.profile.viewmodels.ProfileViewModel
import com.nhlstenden.appdev.features.profile.ProfileFragment
import com.nhlstenden.appdev.features.task.BuyBellPepperDialogFragment
import com.nhlstenden.appdev.core.repositories.AuthRepository
import com.nhlstenden.appdev.core.repositories.UserRepository
import com.nhlstenden.appdev.utils.LevelCalculator
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import android.util.Log
import android.widget.Toast
import android.widget.ImageView
import androidx.viewpager2.widget.ViewPager2
import android.widget.FrameLayout
import javax.inject.Inject

@AndroidEntryPoint
class ProfileHeaderFragment : Fragment() {
    
    private var _binding: FragmentProfileHeaderBinding? = null
    private val binding get() = _binding!!
    private val profileViewModel: ProfileViewModel by activityViewModels()
    
    @Inject lateinit var authRepository: AuthRepository
    @Inject lateinit var userRepository: UserRepository
    
    private var isProfileLoaded = false // Flag to prevent multiple loads
    
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentProfileHeaderBinding.inflate(inflater, container, false)
        return binding.root
    }
    
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        
        Log.d("ProfileHeaderFragment", "ProfileHeaderFragment onViewCreated - setting up profile observation and loading")
        
        // Hide content initially to prevent showing default values
        hideProfileContent()
        
        // Always use ProfileViewModel for complete profile data (display_name, profile_picture, etc.)
        // The cached profile only contains user_attributes (points, streak, etc.) not profile table data
        observeProfile()
        
        // Listen for profile updates from ProfileFragment
        setupProfileUpdateListener()
        
        // Only load profile if not already loaded
        if (!isProfileLoaded) {
            Log.d("ProfileHeaderFragment", "Loading profile for the first time...")
            profileViewModel.loadProfile()
            isProfileLoaded = true
        } else {
            Log.d("ProfileHeaderFragment", "Profile already loaded, skipping...")
        }
        
        setupClickListeners()
    }
    
    private fun setupClickListeners() {
        binding.profileImage.setOnClickListener {
            navigateToProfile()
        }
    }
    
    private fun setupProfileUpdateListener() {
        Log.d("ProfileHeaderFragment", "Setting up profile update listener...")
        parentFragmentManager.setFragmentResultListener("profile_updated", viewLifecycleOwner) { _, bundle ->
            if (bundle.getBoolean("updated", false)) {
                Log.d("ProfileHeaderFragment", "Profile update notification received - refreshing profile")
                refreshProfile()
            }
        }
    }
    
    private fun navigateToProfile() {
        val profileFragment = ProfileFragment()
        
        // Hide ViewPager and show fragment container
        activity?.findViewById<ViewPager2>(R.id.viewPager)?.visibility = View.GONE
        activity?.findViewById<FrameLayout>(R.id.fragment_container)?.visibility = View.VISIBLE
        
        // Hide profile header when navigating to profile
        activity?.findViewById<FrameLayout>(R.id.profileHeaderContainer)?.visibility = View.GONE
        
        // Replace fragment container with profile fragment
        parentFragmentManager.beginTransaction()
            .replace(R.id.fragment_container, profileFragment)
            .addToBackStack(null)
            .commit()
    }

    private fun hideProfileContent() {
        Log.d("ProfileHeaderFragment", "Hiding profile content to prevent showing default values")
        binding.root.alpha = 0f
    }
    
    private fun showProfileContent() {
        Log.d("ProfileHeaderFragment", "Showing profile content with loaded data")
        binding.root.animate()
            .alpha(1f)
            .setDuration(200)
            .start()
    }

    private fun observeProfile() {
        Log.d("ProfileHeaderFragment", "Setting up profile state observation...")
        viewLifecycleOwner.lifecycleScope.launch {
            profileViewModel.profileState.collect { state ->
                Log.d("ProfileHeaderFragment", "Profile state changed: ${state::class.simpleName}")
                when (state) {
                    is ProfileViewModel.ProfileState.Loading -> {
                        Log.d("ProfileHeaderFragment", "Profile state: Loading")
                        // Keep content hidden while loading
                    }
                    is ProfileViewModel.ProfileState.Success -> {
                        Log.d("ProfileHeaderFragment", "Profile state: Success - Bell peppers: ${state.profile.bellPeppers}")
                        updateProfileHeader(state)
                        showProfileContent() // Show content after successful load
                    }
                    is ProfileViewModel.ProfileState.Error -> {
                        Log.e("ProfileHeaderFragment", "Profile state: Error - ${state.message}")
                        // Set default values on error and show content
                        setDefaultValues()
                        showProfileContent()
                    }
                    is ProfileViewModel.ProfileState.LoggedOut -> {
                        Log.d("ProfileHeaderFragment", "Profile state: LoggedOut")
                        // Handle logged out state if needed
                        setDefaultValues()
                        showProfileContent()
                    }
                }
            }
        }
    }
    
    private fun updateProfileHeader(profileState: ProfileViewModel.ProfileState.Success) {
        val profile = profileState.profile
        
        // Update greeting text with display name
        val greetingText = getString(R.string.greeting_text, profile.displayName.takeIf { it.isNotBlank() } ?: "User")
        binding.greetingText.text = greetingText
        
        // Load profile picture
        loadProfilePicture(profile.profilePicture ?: "")
        
        // Update XP and level information
        val xp = profile.experience.toLong()
        val level = profile.level
        val (currentLevel, xpInLevel, xpForNextLevel) = LevelCalculator.calculateLevelAndProgress(xp)
        
        // Update level text in circle
        binding.levelInCircleText.text = level.toString()
        
        // Update XP progress circle
        val progressPercentage = if (xpForNextLevel > 0) {
            (xpInLevel.toFloat() / xpForNextLevel.toFloat() * 100).toInt()
        } else {
            100
        }
        binding.circularXpBar.progress = progressPercentage.toFloat()
        
        // Update bell peppers display
        updateLivesDisplay(profile.bellPeppers)
        
        Log.d("ProfileHeaderFragment", "Updated profile header - Level: $level, XP: $xp")
    }
    
    private fun loadProfilePicture(profilePic: String) {
        Log.d("ProfileHeaderFragment", "loadProfilePicture called with: ${profilePic.take(50)}...")
        if (profilePic.isNotEmpty() && profilePic != "null") {
            if (profilePic.startsWith("http")) {
                Log.d("ProfileHeaderFragment", "Loading profile picture from URL")
                // Load from URL with cache invalidation
                Glide.with(this)
                    .load(profilePic)
                    .skipMemoryCache(true)
                    .placeholder(R.drawable.zorotlpf)
                    .error(R.drawable.zorotlpf)
                    .circleCrop()
                    .into(binding.profileImage)
            } else {
                Log.d("ProfileHeaderFragment", "Loading profile picture from base64")
                // Try to load as base64 with cache invalidation
                try {
                    val imageBytes = android.util.Base64.decode(profilePic, android.util.Base64.DEFAULT)
                    Log.d("ProfileHeaderFragment", "Successfully decoded base64 image, ${imageBytes.size} bytes")
                    Glide.with(this)
                        .load(imageBytes)
                        .skipMemoryCache(true)
                        .placeholder(R.drawable.zorotlpf)
                        .error(R.drawable.zorotlpf)
                        .circleCrop()
                        .into(binding.profileImage)
                } catch (e: Exception) {
                    Log.e("ProfileHeaderFragment", "Error loading base64 image: ${e.message}")
                    binding.profileImage.setImageResource(R.drawable.zorotlpf)
                }
            }
        } else {
            Log.d("ProfileHeaderFragment", "No profile picture to load, using default")
            binding.profileImage.setImageResource(R.drawable.zorotlpf)
        }
    }

    private fun updateLivesDisplay(bellPeppers: Int) {
        Log.d("ProfileHeaderFragment", "updateLivesDisplay called with bellPeppers: $bellPeppers")
        binding.livesContainer.removeAllViews()

        // Make the lives container clickable for buying bell peppers
        binding.livesContainer.setOnClickListener {
            val currentUser = authRepository.getCurrentUserSync()
            if (currentUser != null) {
                CoroutineScope(Dispatchers.IO).launch {
                    try {
                        val attributesResult = userRepository.getUserAttributes(currentUser.id)
                        if (attributesResult.isSuccess) {
                            val profile = attributesResult.getOrThrow()
                            val currentBellPeppers = profile.optInt("bell_peppers", 0)

                            withContext(Dispatchers.Main) {
                                if (currentBellPeppers >= 3) {
                                    Toast.makeText(context, "You already have the maximum number of bell peppers!", Toast.LENGTH_SHORT).show()
                                } else {
                                    BuyBellPepperDialogFragment().show(parentFragmentManager, "buy_bell_pepper")
                                }
                            }
                        } else {
                            withContext(Dispatchers.Main) {
                                Toast.makeText(context, "Error checking bell peppers. Please try again.", Toast.LENGTH_SHORT).show()
                            }
                        }
                    } catch (e: Exception) {
                        withContext(Dispatchers.Main) {
                            Toast.makeText(context, "Error checking bell peppers. Please try again.", Toast.LENGTH_SHORT).show()
                        }
                    }
                }
            }
        }

        // Add the appropriate bell pepper image based on the number of lives
        val imageView = ImageView(requireContext()).apply {
            when (bellPeppers) {
                0 -> {
                    Log.d("ProfileHeaderFragment", "Setting image for 0 bell peppers")
                    // Use animated drawable for zero lives
                    setImageResource(R.drawable.animated_zero_lives)
                    val animationDrawable = drawable as? android.graphics.drawable.AnimationDrawable
                    animationDrawable?.start()
                }
                1 -> {
                    Log.d("ProfileHeaderFragment", "Setting image for 1 bell pepper")
                    setImageResource(R.drawable.profile_bellpepper_one_life)
                }
                2 -> {
                    Log.d("ProfileHeaderFragment", "Setting image for 2 bell peppers")
                    setImageResource(R.drawable.profile_bellpepper_two_lifes)
                }
                3 -> {
                    Log.d("ProfileHeaderFragment", "Setting image for 3 bell peppers")
                    setImageResource(R.drawable.profile_bellpepper_three_lifes)
                }
                else -> {
                    Log.d("ProfileHeaderFragment", "Setting image for invalid bell pepper count: $bellPeppers")
                    // Use animated drawable for zero lives as default
                    setImageResource(R.drawable.animated_zero_lives)
                    val animationDrawable = drawable as? android.graphics.drawable.AnimationDrawable
                    animationDrawable?.start()
                }
            }
            scaleType = ImageView.ScaleType.FIT_CENTER
            layoutParams = android.widget.LinearLayout.LayoutParams(
                80, // Fixed width in dp
                40   // Fixed height in dp
            ).apply {
                // Convert dp to pixels
                val density = resources.displayMetrics.density
                width = (80 * density).toInt()
                height = (40 * density).toInt()
            }
        }
        binding.livesContainer.addView(imageView)
        Log.d("ProfileHeaderFragment", "updateLivesDisplay completed for $bellPeppers bell peppers")
    }
    
    private fun setDefaultValues() {
        binding.greetingText.text = getString(R.string.greeting_text, "User")
        binding.profileImage.setImageResource(R.drawable.zorotlpf)
        binding.levelInCircleText.text = "1"
        binding.circularXpBar.progress = 0f
        updateLivesDisplay(3) // Default to 3 bell peppers
    }
    
    /**
     * Public method to refresh the profile data
     * Called after bell pepper purchases or other profile updates
     */
    fun refreshProfile() {
        Log.d("ProfileHeaderFragment", "refreshProfile() called - triggering ProfileViewModel.loadProfile()")
        isProfileLoaded = false // Reset flag to allow reload
        
        // Clear any potential image cache for the profile image
        Glide.get(requireContext()).clearMemory()
        
        profileViewModel.loadProfile()
        isProfileLoaded = true
    }
    
    /**
     * Force an immediate UI refresh by fetching fresh data directly
     * Used when we need to bypass StateFlow delays
     */
    fun forceRefreshUI() {
        Log.d("ProfileHeaderFragment", "forceRefreshUI() called - fetching fresh data directly...")
        
        val currentUser = authRepository.getCurrentUserSync()
        if (currentUser != null) {
            viewLifecycleOwner.lifecycleScope.launch(Dispatchers.IO) {
                try {
                    Log.d("ProfileHeaderFragment", "Fetching fresh user attributes for UI update...")
                    val attributesResult = userRepository.getUserAttributes(currentUser.id)
                    
                    if (attributesResult.isSuccess) {
                        val userAttributes = attributesResult.getOrThrow()
                        val bellPeppers = userAttributes.optInt("bell_peppers", 0)
                        val points = userAttributes.optInt("points", 0)
                        
                        Log.d("ProfileHeaderFragment", "Fresh data fetched - Bell peppers: $bellPeppers, Points: $points")
                        
                        withContext(Dispatchers.Main) {
                            Log.d("ProfileHeaderFragment", "Updating UI with fresh bell pepper count: $bellPeppers")
                            updateLivesDisplay(bellPeppers)
                        }
                    } else {
                        Log.e("ProfileHeaderFragment", "Failed to fetch fresh attributes: ${attributesResult.exceptionOrNull()?.message}")
                    }
                } catch (e: Exception) {
                    Log.e("ProfileHeaderFragment", "Error in forceRefreshUI: ${e.message}", e)
                }
            }
        } else {
            Log.w("ProfileHeaderFragment", "No current user found for forceRefreshUI")
        }
    }
    

    
    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
} 