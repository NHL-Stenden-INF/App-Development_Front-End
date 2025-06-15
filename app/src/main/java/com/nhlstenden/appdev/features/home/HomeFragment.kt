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
import com.nhlstenden.appdev.R
import android.app.AlertDialog
import android.content.Intent
import android.os.Build
import android.widget.EditText
import androidx.annotation.RequiresApi
import com.nhlstenden.appdev.features.profile.viewmodels.ProfileViewModel
import com.nhlstenden.appdev.features.profile.viewmodels.ProfileViewModel.ProfileState
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.launch
import dagger.hilt.android.AndroidEntryPoint
import com.nhlstenden.appdev.features.rewards.AchievementManager
import java.time.LocalDate
import android.util.Log
import android.widget.Button
import com.nhlstenden.appdev.features.courses.repositories.CourseRepositoryImpl
import com.nhlstenden.appdev.features.home.repositories.StreakRepository
import javax.inject.Inject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.time.temporal.ChronoUnit
import com.daimajia.numberprogressbar.NumberProgressBar
import com.nhlstenden.appdev.core.repositories.AuthRepository
import com.nhlstenden.appdev.core.repositories.UserRepository
import com.nhlstenden.appdev.core.utils.NavigationManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.runBlocking
import android.widget.Toast
import com.bumptech.glide.Glide
import com.nhlstenden.appdev.features.task.BuyBellPepperDialogFragment
import com.nhlstenden.appdev.utils.LevelCalculator
import com.nhlstenden.appdev.core.repositories.FriendsRepository
import com.nhlstenden.appdev.supabase.SupabaseClient

// Data class for course info
data class HomeCourse(
    val id: String,
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
            NavigationManager.navigateToCourseTasks(fragment.requireActivity(), course.id)
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
    
    @Inject lateinit var authRepository: AuthRepository
    @Inject lateinit var userRepository: UserRepository
    @Inject lateinit var courseRepositoryImpl: CourseRepositoryImpl
    @Inject lateinit var friendsRepository: FriendsRepository
    private val profileViewModel: ProfileViewModel by viewModels()
    private var displayNameDialogShown = false

    private lateinit var continueLearningRecyclerView: RecyclerView

    @Inject
    lateinit var streakRepository: StreakRepository
    
    @Inject
    lateinit var achievementManager: AchievementManager
    
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
        val userData = authRepository.getCurrentUserSync()
        if (userData != null) {
            setupUI(view)
        } else {
            Log.e("HomeFragment", "No valid user data available")
        }
        observeViewModel()
        dayCounter(view)
        updateMotivationalMessage(view)
        setupDailyChallenge(view)

        parentFragmentManager.setFragmentResultListener("profile_picture_updated", viewLifecycleOwner) { _, bundle ->
            if (bundle.getBoolean("updated", false)) {
                profileViewModel.loadProfile()
            }
        }
    }

    override fun onResume() {
        super.onResume()
        setupUI(requireView())
        dayCounter(requireView())
        updateMotivationalMessage(requireView())
        setupDailyChallenge(requireView())

        val userData = authRepository.getCurrentUserSync()
        if (userData != null) {
            setupContinueLearning(userData)
        }
    }

    // Initialize UI components and load user profile data
    fun setupUI(view: View) {
        continueLearningRecyclerView = view.findViewById(R.id.continueLearningList)

        val userData = authRepository.getCurrentUserSync()
        if (userData == null) {
            Log.e("HomeFragment", "No valid user data available")
            return
        }

        profileViewModel.loadProfile()
        setupContinueLearning(userData)
    }

    // Monitor profile state changes and handle invalid display names
    private fun observeViewModel() {
        viewLifecycleOwner.lifecycleScope.launch {
            profileViewModel.profileState.collect { state ->
                if (state is ProfileState.Success) {
                    val displayName = state.profile.displayName
                    val invalidNames = listOf("", "null", "default", "user", "anonymous")
                    if (displayName in invalidNames && !displayNameDialogShown) {
                        showDisplayNameDialog()
                    }
                }
            }
        }
    }

    // Show dialog to prompt user for a valid display name
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

    // Create and display the weekly streak counter with visual indicators
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
                val currentUser = authRepository.getCurrentUserSync()
                if (currentUser != null) {
                    Log.d("HomeFragment", "Fetching last task date for user: ${currentUser.id}")
                    val lastTaskDate = streakRepository.getLastTaskDate(currentUser.id.toString(), currentUser.authToken)
                    val currentStreak = streakRepository.getCurrentStreak(currentUser.id.toString(), currentUser.authToken)
                    Log.d("HomeFragment", "Last task date: $lastTaskDate")
                    Log.d("HomeFragment", "Current streak from DB: $currentStreak")
                    
                    streakManager.initializeFromDatabase(lastTaskDate, currentStreak)
                    
                    // Check if streak should be reset due to inactivity
                    val actualStreak = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                        streakManager.checkAndResetStreak(lastTaskDate)
                    } else {
                        currentStreak
                    }
                    
                    // Update streak in database if it changed
                    if (actualStreak != currentStreak) {
                        Log.d("HomeFragment", "Resetting streak from $currentStreak to $actualStreak due to inactivity")
                        try {
                            streakRepository.updateStreak(currentUser.id.toString(), actualStreak, currentUser.authToken)
                        } catch (e: Exception) {
                            Log.e("HomeFragment", "Failed to update streak in database", e)
                        }
                    }
                    
                    // Check for streak achievement if streak is 7 or more
                    if (actualStreak >= 7) {
                        achievementManager.checkStreakAchievement(currentUser.id.toString())
                    }
                    
                    withContext(Dispatchers.Main) {
                        streakCounter.text = "$actualStreak days"
                        
                        for (i in 0..6) {
                            val currentDate = startOfWeek.plusDays(i.toLong())
                            val dayLayout = LinearLayout(requireContext()).apply {
                                orientation = LinearLayout.VERTICAL
                                gravity = Gravity.CENTER
                                layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
                            }

                            val isCompleted = if (lastTaskDate != null) {
                                val daysFromLastTask = ChronoUnit.DAYS.between(currentDate, lastTaskDate)
                                daysFromLastTask >= 0 && daysFromLastTask < currentStreak
                            } else {
                                false
                            }

                            val circle = FrameLayout(requireContext()).apply {
                                layoutParams = FrameLayout.LayoutParams(64, 64).apply {
                                    gravity = Gravity.CENTER
                                }

                                background = ContextCompat.getDrawable(
                                    requireContext(),
                                    if (isCompleted) R.drawable.day_circle_active else R.drawable.day_circle_inactive
                                )
                            }

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
    
    // Check if the app is currently in dark mode
    private fun isNightMode(): Boolean {
        return resources.configuration.uiMode and 
            android.content.res.Configuration.UI_MODE_NIGHT_MASK == 
            android.content.res.Configuration.UI_MODE_NIGHT_YES
    }

    private fun updateUserData(_user: com.nhlstenden.appdev.core.models.User) {
        // Implementation
    }

    // Load and display courses that the user is currently progressing through
    private fun setupContinueLearning(userData: com.nhlstenden.appdev.core.models.User) {
        continueLearningRecyclerView.layoutManager = LinearLayoutManager(requireContext(), LinearLayoutManager.VERTICAL, false)
        
        lifecycleScope.launch {
            try {
                val courses = withContext(Dispatchers.IO) {
                    courseRepositoryImpl.getCourses(userData)
                }
                
                if (courses != null) {
                    val activeCourses = courses.filter { course ->
                        course.progress > 0 && course.progress < course.totalTasks
                    }
                    
                    val homeCourses = activeCourses.map { course ->
                        val progressText = "${course.progress}/${course.totalTasks} tasks"
                        val progressPercent = if (course.totalTasks > 0) {
                            (course.progress.toFloat() / course.totalTasks * 100).toInt()
                        } else {
                            0
                        }
                        
                        HomeCourse(
                            id = course.id,
                            title = course.title,
                            progressText = progressText,
                            progressPercent = progressPercent,
                            iconResId = course.imageResId,
                            accentColor = ContextCompat.getColor(requireContext(), R.color.colorAccent)
                        )
                    }
                    
                    val adapter = HomeCourseAdapter(homeCourses, this@HomeFragment)
                    continueLearningRecyclerView.adapter = adapter
                    
                } else {
                    Log.e("HomeFragment", "Failed to load courses")
                }
            } catch (e: Exception) {
                Log.e("HomeFragment", "Error setting up continue learning: ${e.message}")
            }
        }
    }

    // Generate a motivational message based on a random friend and show it under the streak counter.
    private fun updateMotivationalMessage(root: View) {
        val messageView = root.findViewById<TextView>(R.id.motivationalMessage) ?: return
        val imageView = root.findViewById<ImageView>(R.id.motivationalFriendImage)

        viewLifecycleOwner.lifecycleScope.launch {
            val currentUser = authRepository.getCurrentUserSync() ?: return@launch

            // Get current user stats
            val userStreak = withContext(Dispatchers.IO) {
                try { streakRepository.getCurrentStreak(currentUser.id, currentUser.authToken) } catch (e: Exception) { 0 }
            }

            val userXp = userRepository.getUserAttributes(currentUser.id).getOrNull()?.optInt("xp", 0) ?: 0
            val userLevel = LevelCalculator.calculateLevelFromXp(userXp.toLong())

            // Get the percent completed of courseId
            val userCourseProgress: Map<String, Int> = withContext(Dispatchers.IO) {
                val list = courseRepositoryImpl.getCourses(currentUser) ?: emptyList()
                list.associate { c -> c.id to if (c.totalTasks > 0) (c.progress.toFloat() / c.totalTasks * 100).toInt() else 0 }
            }

            // Get friends info
            val friendsResult = withContext(Dispatchers.IO) { friendsRepository.getAllFriends() }
            if (friendsResult.isFailure) return@launch

            val friends = friendsResult.getOrNull()?.filter { it.username.isNotBlank() } ?: emptyList()

            val candidateMessages = mutableListOf<Pair<String, String?>>()

            for (friend in friends) {
                val friendName = friend.username

                // Get the friend details
                val friendDetails = withContext(Dispatchers.IO) { friendsRepository.getFriendDetails(friend.id) }.getOrNull()
                val friendPic = friend.profilePicture ?: friendDetails?.profilePicture

                // Level comparison
                if (friend.level > userLevel) {
                    candidateMessages.add("$friendName hit level ${friend.level}, can you level up and pass them?" to friendPic)
                }

                // Streak comparison
                val friendStreak = withContext(Dispatchers.IO) {
                    try { streakRepository.getCurrentStreak(friend.id, currentUser.authToken) } catch (e: Exception) { 0 }
                }
                if (friendStreak > userStreak) {
                    candidateMessages.add("$friendName is on a ${friendStreak}-day streak, think you can keep up?" to friendPic)
                }

                // Course comparison
                val coursesOfInterest = listOf("sql", "css", "html")
                if (coursesOfInterest.isNotEmpty()) {
                    val friendCourseMap = friendDetails?.courseProgress?.associate { cp -> cp.courseId to cp.progress } ?: emptyMap()

                    for (courseId in coursesOfInterest) {
                        val friendProgress = friendCourseMap[courseId] ?: continue
                        val userProgress = userCourseProgress[courseId] ?: 0
                        if (friendProgress > userProgress) {
                            val courseName = courseId.uppercase()
                            candidateMessages.add("$friendName is ahead of you in $courseName, time to close the gap!" to friendPic)
                        }
                    }
                }
            }

            // Pick final message or fallback
            val selection: Pair<String, String?>? = if (candidateMessages.isNotEmpty()) {
                candidateMessages.random()
            } else {
                // Fallbacks when user leads in everything or has no friends
                val fallbackMessages = if (friends.isEmpty()) {
                    listOf("Add some friends to start friendly competitions and boost your learning!")
                } else {
                    listOf(
                        "You're leading the pack! Can you keep your top spot?",
                        "You're the highest level among your friends—keep it up!",
                        "Your streak beats all your friends right now—don't slow down!"
                    )
                }
                fallbackMessages.random() to null
            }

            withContext(Dispatchers.Main) {
                messageView.text = selection?.first
                val pic = selection?.second
                val invalidPics = listOf<String?>(null, "", "null")
                if (pic !in invalidPics && imageView != null) {
                    imageView.visibility = View.VISIBLE
                    if (pic!!.startsWith("http")) {
                        Glide.with(this@HomeFragment)
                            .load(resolveProfilePictureUrl(pic))
                            .placeholder(R.drawable.ic_profile_placeholder)
                            .error(R.drawable.ic_profile_placeholder)
                            .circleCrop()
                            .into(imageView)
                    } else {
                        try {
                            val bytes = android.util.Base64.decode(pic, android.util.Base64.DEFAULT)
                            Glide.with(this@HomeFragment)
                                .load(bytes)
                                .placeholder(R.drawable.ic_profile_placeholder)
                                .error(R.drawable.ic_profile_placeholder)
                                .circleCrop()
                                .into(imageView)
                        } catch (e: Exception) {
                            imageView.setImageResource(R.drawable.ic_profile_placeholder)
                        }
                    }
                } else {
                    imageView?.setImageResource(R.drawable.ic_profile_placeholder)
                }
            }
        }
    }

    private fun resolveProfilePictureUrl(url: String?): String? {
        if (url.isNullOrBlank()) return null
        if (url.startsWith("http")) return url

        val trimmed = url.trimStart('/')
        val base = SupabaseClient().supabaseUrl.trimEnd('/')

        return if (trimmed.startsWith("storage/v1/object/public")) {
            "$base/${trimmed}"
        } else {
            "$base/storage/v1/object/public/${trimmed}"
        }
    }

    @RequiresApi(Build.VERSION_CODES.O)
    private fun setupDailyChallenge(view: View) {
        val dailyChallengeStart: Button = view.findViewById(R.id.dailyChallengeButton)
        val dailyChallengeSubtitle: TextView = view.findViewById(R.id.dailyChallengeSubtitle)
        CoroutineScope(Dispatchers.IO).launch {
            val currentUser = authRepository.getCurrentUserSync()
            val startDate = userRepository.getUserAttributes(currentUser?.id.toString()).getOrNull()?.getString("finished_daily_challenge_at")
            var lastCompletedDate = if (startDate == "null") LocalDate.now().minusDays(1) else LocalDate.parse(startDate.toString())

            val isTodayTheDay = ChronoUnit.DAYS.between(lastCompletedDate, LocalDate.now()) != 0L
            CoroutineScope(Dispatchers.Main).launch {
                if (isTodayTheDay) {
                    dailyChallengeStart.setOnClickListener {
                        val intent = Intent(context, DailyChallengeActivity::class.java)
                        startActivity(intent)
                    }
                } else {
                    dailyChallengeSubtitle.text = "You've already done todays challenge, try again tomorrow"
                    dailyChallengeStart.visibility = View.GONE
                }
            }
        }
    }
}