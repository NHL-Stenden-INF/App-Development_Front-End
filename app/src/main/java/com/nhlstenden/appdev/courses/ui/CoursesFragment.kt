package com.nhlstenden.appdev.courses.ui

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.commit
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.chip.ChipGroup
import com.nhlstenden.appdev.courses.models.Course
import com.nhlstenden.appdev.courses.models.CourseAdapter
import androidx.viewpager2.widget.ViewPager2
import android.widget.FrameLayout
import com.nhlstenden.appdev.R
import com.nhlstenden.appdev.courses.parser.CourseParser
import android.widget.RadioGroup

class CoursesFragment : Fragment() {
    private lateinit var coursesList: RecyclerView
    private lateinit var filterChipGroup: ChipGroup

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_courses, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        coursesList = view.findViewById(R.id.coursesList)
        filterChipGroup = view.findViewById(R.id.filterChipGroup)

        setupCoursesList()
        setupFilterChips()
    }

    private fun setupCoursesList() {
        val courseParser = CourseParser(requireContext())
        val parsedCourses = courseParser.loadAllCourses()
        
        val courses = if (parsedCourses.isNotEmpty()) {
            parsedCourses.map { parsedCourse ->
                val difficulty = when {
                    parsedCourse.title.contains("HTML", ignoreCase = true) -> "Beginner"
                    parsedCourse.title.contains("CSS", ignoreCase = true) -> "Intermediate"
                    parsedCourse.title.contains("SQL", ignoreCase = true) -> "Advanced"
                    else -> "Beginner"
                }
                
                val imageResId = when (parsedCourse.title) {
                    "HTML" -> R.drawable.html_course
                    "CSS" -> R.drawable.css_course
                    "SQL" -> R.drawable.sql_course
                    else -> R.drawable.html_course
                }
                
                Course(
                    parsedCourse.title,
                    difficulty,
                    parsedCourse.description,
                    imageResId
                )
            }
        } else {
            listOf(
                Course(
                    "HTML",
                    "Beginner",
                    "Learn the fundamentals of HTML markup language",
                    R.drawable.html_course
                ),
                Course(
                    "CSS",
                    "Intermediate",
                    "Master CSS styling and layout techniques",
                    R.drawable.css_course
                ),
                Course(
                    "SQL",
                    "Advanced",
                    "Learn database management with SQL",
                    R.drawable.sql_course
                )
            )
        }

        coursesList.apply {
            layoutManager = LinearLayoutManager(context)
            adapter = CourseAdapter(courses) { course ->
                // Show CourseTopicsFragment in fragment_container
                val fragment = CourseTopicsFragment().apply {
                    arguments = Bundle().apply {
                        putString("courseName", course.title)
                    }
                }

                requireActivity().findViewById<FrameLayout>(R.id.fragment_container).visibility = View.VISIBLE
                requireActivity().findViewById<ViewPager2>(R.id.viewPager).visibility = View.GONE
                parentFragmentManager.commit {
                    replace(R.id.fragment_container, fragment)
                    addToBackStack(null)
                }
            }
        }
    }

    private fun setupFilterChips() {
        filterChipGroup.setOnCheckedStateChangeListener { _, _ ->
            // TODO: Implement filtering when connected to backend
        }
    }

    private val filterListener = RadioGroup.OnCheckedChangeListener { _, _ ->
        // Filter implementation
    }
}