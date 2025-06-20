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
import androidx.fragment.app.activityViewModels
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
import com.nhlstenden.appdev.supabase.*
import com.nhlstenden.appdev.supabase.SupabaseClient
import com.nhlstenden.appdev.features.home.HomeViewModel
import com.nhlstenden.appdev.features.home.HomeCourse
import com.nhlstenden.appdev.features.courses.repositories.CoursesRepository
import com.bumptech.glide.request.target.CustomTarget
import com.bumptech.glide.request.transition.Transition
import android.graphics.Bitmap
import android.graphics.drawable.Drawable
import androidx.core.graphics.drawable.toDrawable

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
    @Inject lateinit var friendsRepository: FriendsRepository
    private val profileViewModel: ProfileViewModel by viewModels()
    private var displayNameDialogShown = false
    private val homeViewModel: HomeViewModel by activityViewModels()

    private lateinit var continueLearningRecyclerView: RecyclerView

    @Inject
    lateinit var streakRepository: StreakRepository
    
    @Inject
    lateinit var achievementManager: AchievementManager
    
    @Inject
    lateinit var coursesRepository: CoursesRepository
    
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
        observeStreakState(view)
        observeStreakCircles(view)
        observeHomeCourses(view)
        observeMotivationalMessage(view)
        observeDailyChallengeState(view)

        parentFragmentManager.setFragmentResultListener("profile_updated", viewLifecycleOwner) { _, bundle ->
            if (bundle.getBoolean("updated", false)) {
                profileViewModel.loadProfile()
            }
        }

        parentFragmentManager.setFragmentResultListener("profile_mask_updated", viewLifecycleOwner) { _, bundle ->
            if (bundle.getBoolean("updated", false)) {
                profileViewModel.loadProfile()
            }
        }
    }

    override fun onResume() {
        super.onResume()
        setupUI(requireView())
        observeStreakState(requireView())
        observeStreakCircles(requireView())
        observeHomeCourses(requireView())
        observeMotivationalMessage(requireView())
        observeDailyChallengeState(requireView())
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
    private fun observeStreakState(view: View) {
        val streakCounter = view.findViewById<TextView>(R.id.streakCount)
        viewLifecycleOwner.lifecycleScope.launch {
            homeViewModel.streakState.collect { state ->
                when (state) {
                    is HomeViewModel.StreakState.Loading -> streakCounter.text = "Loading..."
                    is HomeViewModel.StreakState.Success -> streakCounter.text = "${state.streak} days"
                    is HomeViewModel.StreakState.Error -> streakCounter.text = "0 days"
                }
            }
        }
        homeViewModel.loadStreak()
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
                    coursesRepository.getCourses(userData)
                }
                if (courses != null) {
                    val activeCourses = courses.filter { course: com.nhlstenden.appdev.features.courses.model.Course ->
                        course.progress > 0 && course.progress < course.totalTasks
                    }
                    val homeCourses = activeCourses.map { course: com.nhlstenden.appdev.features.courses.model.Course ->
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
                        .sortedByDescending { homeCourse: HomeCourse -> homeCourse.progressPercent } // Sort by progress percentage (highest to lowest)
                        .take(3) // Take only the top 3 courses
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
    private fun observeMotivationalMessage(view: View) {
        val messageView = view.findViewById<TextView>(R.id.motivationalMessage) ?: return
        val imageView = view.findViewById<ImageView>(R.id.motivationalFriendImage)
        viewLifecycleOwner.lifecycleScope.launch {
            homeViewModel.motivationalMessage.collect { info ->
                messageView.text = info.text
                val pic = info.profilePicture
                val mask = info.profileMask
                val invalidPics = listOf<String?>(null, "", "null")
                if (pic !in invalidPics && imageView != null) {
                    imageView.visibility = View.VISIBLE

                    val applyDrawable: (android.graphics.drawable.Drawable?) -> Unit = { drawable ->
                        imageView.background = drawable
                        imageView.setImageResource(getImageResource(mask))
                    }

                    if (pic!!.startsWith("http")) {
                        Glide.with(this@HomeFragment)
                            .asBitmap()
                            .load(resolveProfilePictureUrl(pic))
                            .placeholder(R.drawable.ic_profile_placeholder)
                            .error(R.drawable.ic_profile_placeholder)
                            .into(object : CustomTarget<Bitmap>() {
                                override fun onResourceReady(resource: Bitmap, transition: Transition<in Bitmap>?) {
                                    applyDrawable(resource.toDrawable(resources))
                                }

                                override fun onLoadCleared(placeholder: Drawable?) {
                                    imageView.background = placeholder
                                }
                            })
                    } else {
                        try {
                            val bytes = android.util.Base64.decode(pic, android.util.Base64.DEFAULT)
                            Glide.with(this@HomeFragment)
                                .asBitmap()
                                .load(bytes)
                                .placeholder(R.drawable.ic_profile_placeholder)
                                .error(R.drawable.ic_profile_placeholder)
                                .into(object : CustomTarget<Bitmap>() {
                                    override fun onResourceReady(resource: Bitmap, transition: Transition<in Bitmap>?) {
                                        applyDrawable(resource.toDrawable(resources))
                                    }

                                    override fun onLoadCleared(placeholder: Drawable?) {
                                        imageView.background = placeholder
                                    }
                                })
                        } catch (e: Exception) {
                            imageView.setImageResource(R.drawable.ic_profile_placeholder)
                            imageView.background = null
                        }
                    }
                } else {
                    imageView?.setImageResource(R.drawable.ic_profile_placeholder)
                    imageView?.background = null
                }
            }
        }
        homeViewModel.loadMotivationalMessage()
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

    private fun observeDailyChallengeState(view: View) {
        val dailyChallengeStart: Button = view.findViewById(R.id.dailyChallengeButton)
        val dailyChallengeSubtitle: TextView = view.findViewById(R.id.dailyChallengeSubtitle)
        viewLifecycleOwner.lifecycleScope.launch {
            homeViewModel.dailyChallengeState.collect { state ->
                when (state) {
                    is HomeViewModel.DailyChallengeState.Loading -> {
                        dailyChallengeStart.visibility = View.GONE
                        dailyChallengeSubtitle.text = "Loading..."
                    }
                    is HomeViewModel.DailyChallengeState.Success -> {
                        if (state.isAvailable) {
                            dailyChallengeStart.visibility = View.VISIBLE
                            dailyChallengeSubtitle.text = getString(R.string.daily_challenge_home_subtitle)
                            dailyChallengeStart.setOnClickListener {
                                val intent = Intent(context, DailyChallengeActivity::class.java)
                                startActivity(intent)
                            }
                        } else {
                            dailyChallengeSubtitle.setText(R.string.daily_challenge_home_subtitle_completed)
                            dailyChallengeStart.visibility = View.GONE
                        }
                    }
                    is HomeViewModel.DailyChallengeState.Error -> {
                        dailyChallengeStart.visibility = View.GONE
                        dailyChallengeSubtitle.text = "Error loading challenge"
                    }
                }
            }
        }
        homeViewModel.loadDailyChallengeState()
    }

    private fun observeHomeCourses(view: View) {
        continueLearningRecyclerView = view.findViewById(R.id.continueLearningList)
        continueLearningRecyclerView.layoutManager = LinearLayoutManager(requireContext(), LinearLayoutManager.VERTICAL, false)
        viewLifecycleOwner.lifecycleScope.launch {
            homeViewModel.homeCourses.collect { courses ->
                val context = requireContext()
                val coloredCourses = courses.map { it.copy(accentColor = ContextCompat.getColor(context, R.color.colorAccent)) }
                continueLearningRecyclerView.adapter = HomeCourseAdapter(coloredCourses, this@HomeFragment)
            }
        }
        homeViewModel.loadHomeCourses()
    }

    private fun observeStreakCircles(view: View) {
        val container = view.findViewById<LinearLayout>(R.id.daysContainer)
        viewLifecycleOwner.lifecycleScope.launch {
            homeViewModel.streakDays.collect { streakDays ->
                container.removeAllViews()
                for (streakDay in streakDays) {
                    val dayLayout = LinearLayout(requireContext()).apply {
                        orientation = LinearLayout.VERTICAL
                        gravity = Gravity.CENTER
                        layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
                    }
                    val circle = FrameLayout(requireContext()).apply {
                        layoutParams = FrameLayout.LayoutParams(64, 64).apply {
                            gravity = Gravity.CENTER
                        }
                        background = ContextCompat.getDrawable(
                            requireContext(),
                            if (streakDay.isCompleted) R.drawable.day_circle_active else R.drawable.day_circle_inactive
                        )
                    }
                    val fireIcon = ImageView(requireContext()).apply {
                        layoutParams = FrameLayout.LayoutParams(32, 32, Gravity.CENTER)
                        setImageResource(R.drawable.ic_fire)
                        visibility = if (streakDay.isCompleted) View.VISIBLE else View.INVISIBLE
                        setColorFilter(Color.WHITE, PorterDuff.Mode.SRC_IN)
                    }
                    val label = TextView(requireContext()).apply {
                        text = streakDay.label
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
        }
    }

    private fun getImageResource(maskId: String): Int {
        return when (maskId) {
            "circle" -> R.drawable.profile_mask_circle
            "square" -> R.drawable.profile_mask_square
            "cross" -> R.drawable.profile_mask_cross
            "triangle" -> R.drawable.profile_mask_triangle
            else -> R.drawable.profile_mask_circle
        }
    }
}