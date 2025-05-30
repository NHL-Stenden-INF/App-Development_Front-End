package com.nhlstenden.appdev.home.ui

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
import com.nhlstenden.appdev.profile.ui.screens.ProfileFragment
import com.nhlstenden.appdev.R
import com.nhlstenden.appdev.supabase.User
import com.nhlstenden.appdev.courses.parser.CourseParser
import android.app.AlertDialog
import android.os.Build
import android.widget.EditText
import androidx.annotation.RequiresApi
import com.nhlstenden.appdev.profile.ui.viewmodels.ProfileViewModel
import com.nhlstenden.appdev.profile.ui.viewmodels.ProfileViewModel.ProfileState
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.launch
import dagger.hilt.android.AndroidEntryPoint
import com.bumptech.glide.Glide
import com.google.android.material.progressindicator.LinearProgressIndicator
import com.nhlstenden.appdev.home.manager.StreakManager
import com.nhlstenden.appdev.shared.components.UserManager
import java.time.LocalDate
import android.util.Log
import com.nhlstenden.appdev.home.data.repositories.StreakRepository
import javax.inject.Inject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

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
        val progressBar: LinearProgressIndicator = view.findViewById(R.id.progressBar)
        val progressPercentage: TextView = view.findViewById(R.id.progressPercentage)
        val difficultyLevel: TextView = view.findViewById(R.id.difficultyLevel)
        val courseDescription: TextView = view.findViewById(R.id.courseDescription)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_course, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val course = courses[position]
        holder.courseImage.setImageResource(course.iconResId)
        holder.courseTitle.text = course.title
        holder.difficultyLevel.text = course.progressText // Map to difficultyLevel for now
        holder.difficultyLevel.visibility = View.GONE
        holder.courseDescription.text = ""
        holder.courseDescription.visibility = View.GONE
        holder.progressBar.visibility = View.VISIBLE
        holder.progressBar.progress = course.progressPercent
        holder.progressPercentage.visibility = View.VISIBLE
        holder.progressPercentage.text = "${course.progressPercent}%"
        holder.itemView.setOnClickListener {
            navigateToCourse(course)
        }
    }
    
    private fun navigateToCourse(course: HomeCourse) {
        val activity = fragment.requireActivity() as androidx.fragment.app.FragmentActivity
        // Use NavigationManager and pass courseId (use title as fallback if no id)
        com.nhlstenden.appdev.shared.navigation.NavigationManager.navigateToCourseTopics(activity, course.title)
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
    private val profileViewModel: ProfileViewModel by viewModels()
    private var displayNameDialogShown = false

    @Inject
    lateinit var streakRepository: StreakRepository
    private val streakManager = StreakManager()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_home, container, false)
    }

    @RequiresApi(Build.VERSION_CODES.TIRAMISU)
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        dayCounter(view)

        greetingText = view.findViewById(R.id.greetingText)
        motivationalMessage = view.findViewById(R.id.motivationalMessage)
        profilePicture = view.findViewById(R.id.profileImage)

        // Set user data in ProfileViewModel
        val userData = arguments?.getParcelable<User>("USER_DATA") ?: com.nhlstenden.appdev.shared.components.UserManager.getCurrentUser()
        userData?.let { user ->
            profileViewModel.setUserData(user)
            profileViewModel.loadProfile()
        }

        setupProfileButton()
        setupUI(view)

        // Observe profile state using StateFlow
        viewLifecycleOwner.lifecycleScope.launch {
            android.util.Log.d("HomeFragment", "Started collecting profileState")
            profileViewModel.profileState.collect { state ->
                android.util.Log.d("HomeFragment", "profileState emitted: $state")
                if (state is ProfileState.Success) {
                    val displayName = state.profile.displayName
                    val invalidNames = listOf("", "null", "default", "user", "anonymous")
                    android.util.Log.d("HomeFragment", "Display name from profile: '$displayName'")
                    if (displayName in invalidNames && !displayNameDialogShown) {
                        showDisplayNameDialog()
                    } else {
                        greetingText.text = getString(R.string.greeting_format, displayName)
                    }
                    // Show correct profile picture
                    val profilePic = state.profile.profilePicture
                    val invalidPics = listOf(null, "", "null")
                    if (profilePic !in invalidPics) {
                        if (profilePic!!.startsWith("http")) {
                            Glide.with(this@HomeFragment)
                                .load(profilePic)
                                .placeholder(R.drawable.zorotlpf)
                                .error(R.drawable.zorotlpf)
                                .into(profilePicture)
                        } else {
                            // Assume base64
                            val imageBytes = android.util.Base64.decode(profilePic, android.util.Base64.DEFAULT)
                            val bitmap = android.graphics.BitmapFactory.decodeByteArray(imageBytes, 0, imageBytes.size)
                            profilePicture.setImageBitmap(bitmap)
                        }
                    } else {
                        profilePicture.setImageResource(R.drawable.zorotlpf)
                    }
                }
            }
        }
    }
    
    private fun setupProfileButton() {
        profilePicture.setOnClickListener {
            val profileFragment = ProfileFragment()
            parentFragmentManager.beginTransaction()
                .replace(R.id.fragment_container, profileFragment)
                .addToBackStack(null)
                .commit()

            activity?.findViewById<ViewPager2>(R.id.viewPager)?.visibility = View.GONE
            activity?.findViewById<FrameLayout>(R.id.fragment_container)?.visibility = View.VISIBLE
        }
    }
    
    @RequiresApi(Build.VERSION_CODES.TIRAMISU)
    fun setupUI(view: View) {
        // Get user data from arguments
        val userData = arguments?.getParcelable("USER_DATA", User::class.java)
        userData?.let { user ->
            greetingText.text = getString(R.string.greeting_format, user.username)
            updateMotivationalMessage(user)
            
            // Load profile picture
            loadProfilePicture(user.profilePicture)
        }

        // Get course data from XML
        val courseParser = CourseParser(requireContext())
        val parsedCourses = courseParser.loadAllCourses()
        
        // Process course data for the UI
        val courses = if (parsedCourses.isNotEmpty()) {
            parsedCourses.map { course ->
                // Calculate progress information
                val totalTopics = course.topics.size
                val topicsWithProgress = course.topics.count { it.progress > 0 }
                val averageProgress = if (totalTopics > 0) {
                    course.topics.sumOf { it.progress } / totalTopics
                } else 0
                
                // Get appropriate icon and accent color based on course title
                val (iconResId, accentColor) = when (course.title) {
                    "HTML" -> Pair(
                        R.drawable.html_course,
                        ContextCompat.getColor(requireContext(), R.color.html_color)
                    )
                    "CSS" -> Pair(
                        R.drawable.css_course,
                        ContextCompat.getColor(requireContext(), R.color.css_color)
                    )
                    "SQL" -> Pair(
                        R.drawable.sql_course,
                        ContextCompat.getColor(requireContext(), R.color.sql_color)
                    )
                    else -> Pair(
                        R.drawable.html_course,
                        ContextCompat.getColor(requireContext(), R.color.html_color)
                    )
                }
                
                HomeCourse(
                    course.title,
                    "Lesson $topicsWithProgress of $totalTopics",
                    averageProgress,
                    iconResId,
                    accentColor
                )
            }
        } else {
            // Fallback to hardcoded data if XML parsing fails
            listOf(
                HomeCourse(
                    "HTML", 
                    "Lesson 5 of 10", 
                    50, 
                    R.drawable.html_course,
                    ContextCompat.getColor(requireContext(), R.color.html_color)
                ),
                HomeCourse(
                    "CSS", 
                    "Lesson 8 of 12", 
                    67, 
                    R.drawable.css_course,
                    ContextCompat.getColor(requireContext(), R.color.css_color)
                ),
                HomeCourse(
                    "SQL", 
                    "Lesson 3 of 8", 
                    38, 
                    R.drawable.sql_course,
                    ContextCompat.getColor(requireContext(), R.color.sql_color)
                )
            )
        }

        val recyclerView = view.findViewById<RecyclerView>(R.id.continueLearningList)
        recyclerView.layoutManager = LinearLayoutManager(context)
        recyclerView.adapter = HomeCourseAdapter(courses, this)
    }
    
    private fun loadProfilePicture(profilePictureData: String) {
        if (profilePictureData.isNotEmpty()) {
            try {
                val imageData = android.util.Base64.decode(profilePictureData, android.util.Base64.DEFAULT)
                val bitmap = android.graphics.BitmapFactory.decodeByteArray(imageData, 0, imageData.size)
                if (bitmap != null) {
                    profilePicture.setImageBitmap(bitmap)
                }
            } catch (e: Exception) {
                // If there's an error, keep the default image
                android.util.Log.e("HomeFragment", "Error loading profile picture: ${e.message}")
            }
        }
    }

    private fun updateMotivationalMessage(_user: User) {
        // TODO: Replace with actual friend data from the backend
        val friendName = "John"
        val tasksAhead = 5
        motivationalMessage.text = getString(R.string.motivational_message, friendName, tasksAhead)
    }

    @RequiresApi(Build.VERSION_CODES.O)
    private fun dayCounter(view: View) {
        val days = listOf("Mon", "Tue", "Wed", "Thu", "Fri", "Sat", "Sun")
        val container = view.findViewById<LinearLayout>(R.id.daysContainer)
        container.removeAllViews()

        val today = LocalDate.now()
        val startOfWeek = today.minusDays(today.dayOfWeek.value.toLong() -1)

        val streakCounter = view.findViewById<TextView>(R.id.streakCount)

        viewLifecycleOwner.lifecycleScope.launch(Dispatchers.IO) {
            try {
                val currentUser = UserManager.getCurrentUser()
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

                            // Draw the circle
                            val circle = FrameLayout(requireContext()).apply {
                                layoutParams = FrameLayout.LayoutParams(64, 64).apply {
                                    gravity = Gravity.CENTER
                                }

                                // Only mark as completed if it's the last task date
                                val isCompleted = lastTaskDate?.isEqual(currentDate) == true

                                background = ContextCompat.getDrawable(
                                    requireContext(),
                                    if (isCompleted) R.drawable.day_circle_active else R.drawable.day_circle_inactive
                                )
                            }

                            // Draw the checkmark
                            val check = ImageView(requireContext()).apply {
                                layoutParams = FrameLayout.LayoutParams(32, 32, Gravity.CENTER)
                                setImageResource(R.drawable.ic_check)
                                // Show checkmark only on the last completed day
                                visibility = if (lastTaskDate?.isEqual(currentDate) == true) View.VISIBLE else View.INVISIBLE
                                setColorFilter(Color.WHITE, PorterDuff.Mode.SRC_IN)
                            }

                            val label = TextView(requireContext()).apply {
                                text = days[i]
                                setTextColor(if (isNightMode()) Color.WHITE else Color.BLACK)
                                textSize = 16f
                                setPadding(0, 8, 0, 0)
                                gravity = Gravity.CENTER
                            }

                            circle.addView(check)
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

    private fun updateUserData(_user: User) {
        // Implementation
    }

    private fun showDisplayNameDialog() {
        displayNameDialogShown = true
        val editText = EditText(requireContext())
        editText.hint = "Enter display name"
        AlertDialog.Builder(requireContext())
            .setTitle("Set Display Name")
            .setMessage("Please enter a display name to continue.")
            .setView(editText)
            .setCancelable(false)
            .setPositiveButton("Save") { _, _ ->
                val newDisplayName = editText.text.toString().trim()
                if (newDisplayName.isNotEmpty()) {
                    // Update profile in Supabase
                    profileViewModel.updateProfile(newDisplayName, null, null)
                } else {
                    // Show dialog again if input is empty
                    displayNameDialogShown = false
                    showDisplayNameDialog()
                }
            }
            .show()
    }
}