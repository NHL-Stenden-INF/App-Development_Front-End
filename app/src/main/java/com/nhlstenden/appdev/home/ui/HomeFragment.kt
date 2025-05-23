package com.nhlstenden.appdev.home.ui

import android.content.res.ColorStateList
import android.graphics.Color
import android.graphics.PorterDuff
import android.os.Bundle
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.ProgressBar
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.viewpager2.widget.ViewPager2
import com.nhlstenden.appdev.profile.ui.ProfileFragment
import com.nhlstenden.appdev.R
import com.nhlstenden.appdev.supabase.User
import com.nhlstenden.appdev.courses.ui.CourseTopicsFragment
import com.nhlstenden.appdev.courses.parser.CourseParser

// Data class for course info
data class HomeCourse(
    val title: String,
    val progressText: String,
    val progressPercent: Int,
    val iconResId: Int,
    val accentColor: Int
)

class HomeCourseAdapter(private val courses: List<HomeCourse>) : RecyclerView.Adapter<HomeCourseAdapter.ViewHolder>() {
    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val courseIcon: ImageView = view.findViewById(R.id.courseIcon)
        val courseTitle: TextView = view.findViewById(R.id.courseTitle)
        val courseProgress: TextView = view.findViewById(R.id.courseProgress)
        val courseProgressBar: ProgressBar = view.findViewById(R.id.courseProgressBar)
        val coursePlayButton: ImageButton = view.findViewById(R.id.coursePlayButton)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.course_select_card, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val course = courses[position]
        holder.courseIcon.setImageResource(course.iconResId)
        holder.courseIcon.contentDescription = "${course.title} course icon"
        holder.courseTitle.text = course.title
        holder.courseProgress.text = course.progressText
        holder.courseProgressBar.max = 100
        holder.courseProgressBar.progress = course.progressPercent
        holder.courseProgressBar.progressTintList = ColorStateList.valueOf(course.accentColor)
        holder.coursePlayButton.backgroundTintList = ColorStateList.valueOf(course.accentColor)
        
        // Add click listener for the entire card
        holder.itemView.setOnClickListener {
            navigateToCourse(holder.itemView.context, course.title)
        }
        
        // Add click listener for the play button
        holder.coursePlayButton.setOnClickListener {
            navigateToCourse(holder.itemView.context, course.title)
        }
    }
    
    private fun navigateToCourse(context: android.content.Context, courseName: String) {
        val fragment = CourseTopicsFragment().apply {
            arguments = Bundle().apply {
                putString("courseName", courseName)
            }
        }

        val activity = context as androidx.fragment.app.FragmentActivity
        activity.findViewById<FrameLayout>(R.id.fragment_container).visibility = View.VISIBLE
        activity.findViewById<ViewPager2>(R.id.viewPager).visibility = View.GONE
        
        activity.supportFragmentManager.beginTransaction()
            .replace(R.id.fragment_container, fragment)
            .addToBackStack(null)
            .commit()
    }

    override fun getItemCount() = courses.size
}

/**
 * A simple [Fragment] subclass.
 * Use the [HomeFragment.newInstance] factory method to
 * create an instance of this fragment.
 */
class HomeFragment : Fragment() {
    private lateinit var greetingText: TextView
    private lateinit var motivationalMessage: TextView
    private lateinit var profilePicture: ImageView

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_home, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        dayCounter(view)

        greetingText = view.findViewById(R.id.greetingText)
        motivationalMessage = view.findViewById(R.id.motivationalMessage)
        profilePicture = view.findViewById(R.id.profileImage)

        setupProfileButton()
        setupUI(view)
    }
    
    private fun setupProfileButton() {
        profilePicture.setOnClickListener {
            val userData = arguments?.getParcelable<User>("USER_DATA")

            val profileFragment = ProfileFragment().apply {
                arguments = Bundle().apply {
                    putParcelable("USER_DATA", userData)
                }
            }

            parentFragmentManager.beginTransaction()
                .replace(R.id.fragment_container, profileFragment)
                .addToBackStack(null)
                .commit()

            activity?.findViewById<ViewPager2>(R.id.viewPager)?.visibility = View.GONE
            activity?.findViewById<FrameLayout>(R.id.fragment_container)?.visibility = View.VISIBLE
        }
    }
    
    fun setupUI(view: View) {
        // Get user data from arguments
        val userData = arguments?.getParcelable<User>("USER_DATA")
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
        recyclerView.adapter = HomeCourseAdapter(courses)
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

    private fun updateMotivationalMessage(user: User) {
        // TODO: Replace with actual friend data from the backend
        val friendName = "John"
        val tasksAhead = 5
        motivationalMessage.text = getString(R.string.motivational_message, friendName, tasksAhead)
    }

    private fun dayCounter(view: View) {
        val days = listOf("Mon", "Tue", "Wed", "Thu", "Fri", "Sat", "Sun")
        val completedDays = setOf(0, 1, 2, 3, 4)

        val container = view.findViewById<LinearLayout>(R.id.daysContainer)
        container.removeAllViews()

        // Using a solid color that's visible in both light and dark modes
        val textColor = if (isNightMode()) {
            Color.WHITE
        } else {
            Color.BLACK
        }

        for ((index, day) in days.withIndex()) {
            val dayLayout = LinearLayout(requireContext()).apply {
                orientation = LinearLayout.VERTICAL
                gravity = Gravity.CENTER
                layoutParams =
                    LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
            }

            // Draw the circle
            val circle = FrameLayout(requireContext()).apply {
                layoutParams = FrameLayout.LayoutParams(64, 64).apply {
                    gravity = Gravity.CENTER
                }

                background = ContextCompat.getDrawable(
                    requireContext(),
                    if (completedDays.contains(index)) R.drawable.day_circle_active else R.drawable.day_circle_inactive
                )
            }

            // Draw the checkmark
            val check = ImageView(requireContext()).apply {
                layoutParams = FrameLayout.LayoutParams(32, 32, Gravity.CENTER)
                setImageResource(R.drawable.ic_check)
                visibility = if (completedDays.contains(index)) View.VISIBLE else View.INVISIBLE
                setColorFilter(Color.WHITE, PorterDuff.Mode.SRC_IN)
            }

            val label = TextView(requireContext()).apply {
                text = day
                setTextColor(textColor)
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
    
    private fun isNightMode(): Boolean {
        return resources.configuration.uiMode and 
            android.content.res.Configuration.UI_MODE_NIGHT_MASK == 
            android.content.res.Configuration.UI_MODE_NIGHT_YES
    }
}