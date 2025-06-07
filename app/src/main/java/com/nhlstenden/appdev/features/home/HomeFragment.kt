package com.nhlstenden.appdev.features.home

import android.graphics.Color
import android.graphics.PorterDuff
import android.os.Bundle
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.viewpager2.widget.ViewPager2
import com.nhlstenden.appdev.features.profile.screens.ProfileFragment
import com.nhlstenden.appdev.R
import android.app.AlertDialog
import android.app.Application
import android.os.Build
import android.widget.EditText
import androidx.annotation.RequiresApi
import com.nhlstenden.appdev.features.profile.viewmodels.ProfileViewModel
import com.nhlstenden.appdev.features.profile.viewmodels.ProfileViewModel.ProfileState
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.launch
import dagger.hilt.android.AndroidEntryPoint
import com.bumptech.glide.Glide
import com.google.android.material.progressindicator.LinearProgressIndicator
import com.nhlstenden.appdev.features.home.StreakManager
import java.time.LocalDate
import android.util.Log
import com.nhlstenden.appdev.features.courses.repositories.CourseRepositoryImpl
import com.nhlstenden.appdev.features.home.repositories.StreakRepository
import javax.inject.Inject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import java.time.temporal.ChronoUnit
import com.mikhaellopez.circularprogressbar.CircularProgressBar
import com.daimajia.numberprogressbar.NumberProgressBar
import com.nhlstenden.appdev.core.utils.UserManager
import com.nhlstenden.appdev.core.utils.NavigationManager
import java.io.File

// Data class for course info
data class HomeCourse(
    val title: String,
    val progressText: String,
    val progressPercent: Int,
    val iconResId: Int,
    val accentColor: Int
)

class HomeCourseAdapter(private val courses: List<HomeCourse>, private val fragment: Fragment) : RecyclerView.Adapter<HomeCourseAdapter.ViewHolder>() {
    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val courseImage: ImageView = view.findViewById(R.id.courseImage)
        val courseTitle: TextView = view.findViewById(R.id.courseTitle)
        val difficultyLevel: TextView = view.findViewById(R.id.difficultyLevel)
        val courseDescription: TextView = view.findViewById(R.id.courseDescription)
        val progressBar: NumberProgressBar = view.findViewById(R.id.progressBar)
        val root: View = view
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_course, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val course = courses[position]
        holder.courseTitle.text = course.title
        holder.difficultyLevel.text = course.progressText
        holder.courseDescription.visibility = View.GONE
        holder.courseImage.setImageResource(course.iconResId)
        holder.progressBar.progress = course.progressPercent

        holder.root.setOnClickListener {
            NavigationManager.navigateToCourseTasks(fragment.requireActivity(), course.title)
        }
    }

    override fun getItemCount() = courses.size
}

/**
 * A simple [Fragment] subclass.
 * Use the [HomeFragment.newInstance] factory method to
 * create an instance of this fragment.
 */
@AndroidEntryPoint
class HomeFragment : Fragment() {
    private lateinit var greetingText: TextView
    private lateinit var motivationalMessage: TextView
    private lateinit var profilePicture: ImageView
    private lateinit var circularXpBar: CircularProgressBar
    private lateinit var levelInCircleText: TextView
    private lateinit var livesDisplay: ImageView
    private lateinit var courseRepositoryImpl: CourseRepositoryImpl
    private val profileViewModel: ProfileViewModel by viewModels()
    private var displayNameDialogShown = false

    @Inject
    lateinit var streakRepository: StreakRepository
    private val streakManager = StreakManager()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_home, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        courseRepositoryImpl = CourseRepositoryImpl(requireContext().applicationContext as Application)
        // Wait for user data to be set before setting up UI
        val userData = UserManager.getCurrentUser()
        if (userData != null && userData.authToken.isNotEmpty()) {
            setupUI(view)
        } else {
            Log.e("HomeFragment", "No valid user data available")
        }
        observeViewModel()
        dayCounter(view)
        
        // Set up fragment result listener for profile picture updates
        parentFragmentManager.setFragmentResultListener("profile_picture_updated", viewLifecycleOwner) { _, bundle ->
            if (bundle.getBoolean("updated", false)) {
                // Reload the profile to get the updated picture
                profileViewModel.loadProfile()
            }
        }
    }

    override fun onResume() {
        super.onResume()
        setupUI(requireView())
        dayCounter(requireView())
    }

    fun setupUI(view: View) {
        greetingText = view.findViewById(R.id.greetingText)
        motivationalMessage = view.findViewById(R.id.motivationalMessage)
        profilePicture = view.findViewById(R.id.profileImage)
        circularXpBar = view.findViewById(R.id.circularXpBar)
        levelInCircleText = view.findViewById(R.id.levelInCircleText)
        livesDisplay = view.findViewById(R.id.livesDisplay)

        val userData = UserManager.getCurrentUser()
        if (userData == null || userData.authToken.isEmpty()) {
            Log.e("HomeFragment", "No valid user data or auth token available")
            return
        }

        // Load profile picture
        loadProfilePicture(userData.profilePicture ?: "")

        // Add click listener to profile picture
        profilePicture.setOnClickListener {
            val profileFragment = ProfileFragment()
            
            // Hide ViewPager and show fragment container
            activity?.findViewById<ViewPager2>(R.id.viewPager)?.visibility = View.GONE
            activity?.findViewById<FrameLayout>(R.id.fragment_container)?.visibility = View.VISIBLE
            
            // Replace fragment container with profile fragment
            parentFragmentManager.beginTransaction()
                .replace(R.id.fragment_container, profileFragment)
                .addToBackStack(null)
                .commit()
        }

        // Set user data in ProfileViewModel
        profileViewModel.setUserData(userData)
        profileViewModel.loadProfile()
    }

    private fun observeViewModel() {
        viewLifecycleOwner.lifecycleScope.launch {
            profileViewModel.profileState.collect { state ->
                if (state is ProfileState.Success) {
                    val displayName = state.profile.displayName
                    val invalidNames = listOf("", "null", "default", "user", "anonymous")
                    if (displayName in invalidNames && !displayNameDialogShown) {
                        showDisplayNameDialog()
                    } else {
                        greetingText.text = getString(R.string.greeting_format, displayName)
                    }

                    // Update profile picture
                    loadProfilePicture(state.profile.profilePicture ?: "")

                    // Update lives display
                    updateLivesDisplay(state.profile.bellPeppers)

                    // Set circular XP bar and level
                    val level = state.profile.level
                    val xp = state.profile.experience
                    levelInCircleText.text = level.toString()
                    
                    // Calculate XP needed for next level
                    var requiredXp = 100.0
                    var totalXp = 0.0
                    for (i in 1 until level) {
                        totalXp += requiredXp
                        requiredXp *= 1.1
                    }
                    val xpForCurrentLevel = xp - totalXp.toInt()
                    val xpForNextLevel = requiredXp.toInt()
                    circularXpBar.progressMax = xpForNextLevel.toFloat()
                    circularXpBar.setProgressWithAnimation(xpForCurrentLevel.coerceAtLeast(0).toFloat(), 800)
                }
            }
        }
    }

    private fun loadProfilePicture(profilePic: String) {
        if (profilePic.isNotEmpty() && profilePic != "null") {
            if (profilePic.startsWith("http")) {
                Glide.with(this)
                    .load(profilePic)
                    .placeholder(R.drawable.zorotlpf)
                    .error(R.drawable.zorotlpf)
                    .circleCrop()
                    .into(profilePicture)
            } else {
                // Try to load as base64
                try {
                    val imageBytes = android.util.Base64.decode(profilePic, android.util.Base64.DEFAULT)
                    Glide.with(this)
                        .load(imageBytes)
                        .placeholder(R.drawable.zorotlpf)
                        .error(R.drawable.zorotlpf)
                        .circleCrop()
                        .into(profilePicture)
                } catch (e: Exception) {
                    profilePicture.setImageResource(R.drawable.zorotlpf)
                }
            }
        } else {
            profilePicture.setImageResource(R.drawable.zorotlpf)
        }
    }

    private fun showDisplayNameDialog() {
        displayNameDialogShown = true
        val editText = EditText(requireContext()).apply {
            hint = "Enter display name"
            setSingleLine()
        }

        AlertDialog.Builder(requireContext())
            .setTitle("Set Display Name")
            .setMessage("Please enter a display name to continue.")
            .setView(editText)
            .setCancelable(false)
            .setPositiveButton("Save") { _, _ ->
                val newName = editText.text.toString().trim()
                if (newName.isNotEmpty()) {
                    profileViewModel.updateProfile(newName, null, null)
                } else {
                    displayNameDialogShown = false
                    showDisplayNameDialog()
                }
            }
            .show()
    }

    @RequiresApi(Build.VERSION_CODES.TIRAMISU)
    private fun dayCounter(view: View) {
        val days = listOf("Mon", "Tue", "Wed", "Thu", "Fri", "Sat", "Sun")
        val container = view.findViewById<LinearLayout>(R.id.daysContainer)
        container.removeAllViews()

        val today = LocalDate.now()
        val startOfWeek = today.minusDays(today.dayOfWeek.value.toLong() -1)

        val streakCounter = view.findViewById<TextView>(R.id.streakCount)

        viewLifecycleOwner.lifecycleScope.launch(Dispatchers.IO) {
            try {
                val currentUser = com.nhlstenden.appdev.core.utils.UserManager.getCurrentUser()
                if (currentUser != null) {
                    Log.d("HomeFragment", "Fetching last task date for user: ${currentUser.id}")
                    val lastTaskDate = streakRepository.getLastTaskDate(currentUser.id.toString(), currentUser.authToken)
                    val currentStreak = streakRepository.getCurrentStreak(currentUser.id.toString(), currentUser.authToken)
                    Log.d("HomeFragment", "Last task date: $lastTaskDate")
                    Log.d("HomeFragment", "Current streak from DB: $currentStreak")
                    
                    // Initialize streak manager with database values
                    streakManager.initializeFromDatabase(lastTaskDate, currentStreak)
                    
                    // Switch to main thread for UI updates
                    withContext(Dispatchers.Main) {
                        // Update streak counter with the value from streak manager
                        streakCounter.text = "${streakManager.getCurrentStreak()} days"
                        
                        // Draw the days
                        for (i in 0..6) {
                            val currentDate = startOfWeek.plusDays(i.toLong())
                            val dayLayout = LinearLayout(requireContext()).apply {
                                orientation = LinearLayout.VERTICAL
                                gravity = Gravity.CENTER
                                layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
                            }

                            // Calculate if this day should be marked as completed
                            val isCompleted = if (lastTaskDate != null) {
                                // Check if this date is within the streak period
                                val daysFromLastTask = ChronoUnit.DAYS.between(currentDate, lastTaskDate)
                                daysFromLastTask >= 0 && daysFromLastTask < currentStreak
                            } else {
                                false
                            }

                            // Draw the circle
                            val circle = FrameLayout(requireContext()).apply {
                                layoutParams = FrameLayout.LayoutParams(64, 64).apply {
                                    gravity = Gravity.CENTER
                                }

                                background = ContextCompat.getDrawable(
                                    requireContext(),
                                    if (isCompleted) R.drawable.day_circle_active else R.drawable.day_circle_inactive
                                )
                            }

                            // Draw the fire icon for completed days
                            val fireIcon = ImageView(requireContext()).apply {
                                layoutParams = FrameLayout.LayoutParams(32, 32, Gravity.CENTER)
                                setImageResource(R.drawable.ic_fire)
                                visibility = if (isCompleted) View.VISIBLE else View.INVISIBLE
                                setColorFilter(Color.WHITE, PorterDuff.Mode.SRC_IN)
                            }

                            val label = TextView(requireContext()).apply {
                                text = days[i]
                                setTextColor(if (isNightMode()) Color.WHITE else Color.BLACK)
                                textSize = 16f
                                setPadding(0, 8, 0, 0)
                                gravity = Gravity.CENTER
                            }

                            circle.addView(fireIcon)
                            dayLayout.addView(circle)
                            dayLayout.addView(label)
                            container.addView(dayLayout)
                        }
                    }
                } else {
                    Log.e("HomeFragment", "No current user found")
                    withContext(Dispatchers.Main) {
                        streakCounter.text = "0 days"
                    }
                }
            } catch (e: Exception) {
                Log.e("HomeFragment", "Error updating streak: ${e.message}")
                Log.e("HomeFragment", "Stack trace: ${e.stackTraceToString()}")
                withContext(Dispatchers.Main) {
                    streakCounter.text = "0 days"
                }
            }
        }
    }
    
    private fun isNightMode(): Boolean {
        return resources.configuration.uiMode and 
            android.content.res.Configuration.UI_MODE_NIGHT_MASK == 
            android.content.res.Configuration.UI_MODE_NIGHT_YES
    }

    private fun updateUserData(_user: com.nhlstenden.appdev.core.models.User) {
        // Implementation
    }

    private fun updateLivesDisplay(bellPeppers: Int) {
        android.util.Log.d("LivesDisplay", "bellPeppers = $bellPeppers")
        when (bellPeppers) {
            0 -> {
                livesDisplay.setImageResource(R.drawable.animated_zero_lives)
                (livesDisplay.drawable as? android.graphics.drawable.AnimationDrawable)?.start()
            }
            1 -> livesDisplay.setImageResource(R.drawable.profile_bellpepper_one_life)
            2 -> livesDisplay.setImageResource(R.drawable.profile_bellpepper_two_lifes)
            3 -> livesDisplay.setImageResource(R.drawable.profile_bellpepper_three_lifes)
            else -> livesDisplay.setImageResource(R.drawable.profile_bellpepper_anim_three)
        }
    }
}