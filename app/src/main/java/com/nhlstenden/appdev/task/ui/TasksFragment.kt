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
import com.nhlstenden.appdev.courses.ui.CourseFragment
import dagger.hilt.android.AndroidEntryPoint
import com.nhlstenden.appdev.shared.navigation.NavigationManager

@AndroidEntryPoint
class TasksFragment : Fragment() {
    private lateinit var tasksList: RecyclerView
    private lateinit var filterChipGroup: ChipGroup
    private lateinit var adapter: CourseAdapter

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

        adapter = CourseAdapter { course ->
            // Use NavigationManager for consistent navigation and back behavior
            NavigationManager.navigateToCourseTopics(requireActivity(), course.id)
        }
        tasksList.layoutManager = LinearLayoutManager(context)
        tasksList.adapter = adapter

        setupTasksList()
        setupFilterChips()
    }

    private fun setupTasksList() {
        val courses = listOf(
            Course(
                id = "1",
                title = "HTML",
                level = "Beginner",
                description = "Learn the fundamentals of HTML markup language",
                imageResId = R.drawable.html_course
            ),
            Course(
                id = "2",
                title = "CSS",
                level = "Intermediate",
                description = "Master CSS styling and layout techniques",
                imageResId = R.drawable.css_course
            ),
            Course(
                id = "3",
                title = "SQL",
                level = "Advanced",
                description = "Learn database management with SQL",
                imageResId = R.drawable.sql_course
            )
        )
        adapter.submitList(courses)
    }

    private fun setupFilterChips() {
        filterChipGroup.setOnCheckedStateChangeListener { _group, _checkedIds ->
            // TODO: Implement filtering when connected to backend
        }
    }
}