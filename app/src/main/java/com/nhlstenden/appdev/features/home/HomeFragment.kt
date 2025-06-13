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
import com.nhlstenden.appdev.features.profile.ProfileFragment
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
import com.nhlstenden.appdev.core.repositories.AuthRepository
import com.nhlstenden.appdev.core.repositories.UserRepository
import com.nhlstenden.appdev.core.utils.NavigationManager
import java.io.File
import kotlinx.coroutines.CoroutineScope
import android.widget.Toast
import com.nhlstenden.appdev.features.task.BuyBellPepperDialogFragment
import com.nhlstenden.appdev.utils.LevelCalculator

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
    private val profileViewModel: ProfileViewModel by viewModels()
    private var displayNameDialogShown = false

    private lateinit var continueLearningRecyclerView: RecyclerView

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
        val userData = authRepository.getCurrentUserSync()
        if (userData != null) {
            setupUI(view)
        } else {
            Log.e("HomeFragment", "No valid user data available")
        }
        observeViewModel()
        dayCounter(view)
        
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
                    
                    withContext(Dispatchers.Main) {
                        streakCounter.text = "${streakManager.getCurrentStreak()} days"
                        
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

}