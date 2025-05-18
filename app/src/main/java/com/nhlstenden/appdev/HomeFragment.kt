package com.nhlstenden.appdev

import android.graphics.Color
import android.graphics.PorterDuff
import android.os.Bundle
import android.view.Gravity
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import android.widget.ProgressBar
import android.content.res.ColorStateList
import android.widget.ImageButton
import androidx.core.graphics.toColorInt

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
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.course_select_card, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val course = courses[position]
        holder.courseIcon.setImageResource(course.iconResId)
        holder.courseTitle.text = course.title
        holder.courseProgress.text = course.progressText
        holder.courseProgressBar.max = 100
        holder.courseProgressBar.progress = course.progressPercent
        holder.courseProgressBar.progressTintList = ColorStateList.valueOf(course.accentColor)
        holder.itemView.findViewById<ImageButton>(R.id.coursePlayButton)
            .backgroundTintList = ColorStateList.valueOf(course.accentColor)
    }

    override fun getItemCount() = courses.size
}

class HomeFragment : Fragment() {

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        return inflater.inflate(R.layout.fragment_home, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        dayCounter(view)

        // Set up dynamic course cards
        val courses = listOf(
            HomeCourse("HTML", "Lesson 5 of 10", 50, R.drawable.html_course, "#E44D26".toColorInt()),
            HomeCourse("CSS", "Lesson 8 of 12", 67, R.drawable.css_course, "#264DE4".toColorInt()),
            HomeCourse("SQL", "Lesson 3 of 8", 38, R.drawable.sql_course, "#336791".toColorInt())
        )
        
        val recyclerView = view.findViewById<RecyclerView>(R.id.continueLearningList)
        recyclerView.layoutManager = LinearLayoutManager(context)
        recyclerView.adapter = HomeCourseAdapter(courses)
    }

    private fun dayCounter(view: View) {
        val days = listOf("Mon", "Tue", "Wed", "Thu", "Fri", "Sat", "Sun")
        val completedDays = setOf(0, 1, 2, 3, 4)

        val container = view.findViewById<LinearLayout>(R.id.daysContainer)
        container.removeAllViews()

        for ((index, day) in days.withIndex()) {
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
                setTextColor(Color.BLACK)
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
}