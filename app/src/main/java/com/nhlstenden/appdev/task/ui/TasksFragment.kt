package com.nhlstenden.appdev.task.ui

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
import com.nhlstenden.appdev.courses.ui.CourseTopicsFragment

class TasksFragment : Fragment() {
    private lateinit var tasksList: RecyclerView
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

        tasksList = view.findViewById(R.id.coursesList)
        filterChipGroup = view.findViewById(R.id.filterChipGroup)

        setupTasksList()
        setupFilterChips()
    }

    private fun setupTasksList() {
        val courses = listOf(
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

        tasksList.apply {
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
        filterChipGroup.setOnCheckedStateChangeListener { _group, _checkedIds ->
            // TODO: Implement filtering when connected to backend
        }
    }
}