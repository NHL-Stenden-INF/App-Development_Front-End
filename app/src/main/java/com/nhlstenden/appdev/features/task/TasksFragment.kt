package com.nhlstenden.appdev.features.task

import android.app.Application
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.chip.ChipGroup
import com.nhlstenden.appdev.features.courses.model.Course
import com.nhlstenden.appdev.R
import com.nhlstenden.appdev.core.utils.NavigationManager
import com.nhlstenden.appdev.features.courses.repositories.CourseRepositoryImpl
import dagger.hilt.android.AndroidEntryPoint
import com.nhlstenden.appdev.features.courses.screens.CourseAdapter
import kotlinx.coroutines.runBlocking

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
        runBlocking {
            val courses = CourseRepositoryImpl(context as Application).getCourses()
            adapter.submitList(courses)
        }
    }

    private fun setupFilterChips() {
        filterChipGroup.setOnCheckedStateChangeListener { _group, _checkedIds ->
            // TODO: Implement filtering when connected to backend
        }
    }
}