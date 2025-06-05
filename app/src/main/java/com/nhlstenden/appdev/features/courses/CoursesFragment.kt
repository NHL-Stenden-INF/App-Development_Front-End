package com.nhlstenden.appdev.features.courses

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.chip.ChipGroup
import com.nhlstenden.appdev.R
import com.nhlstenden.appdev.features.courses.screens.CourseAdapter
import com.nhlstenden.appdev.core.utils.NavigationManager
import com.nhlstenden.appdev.features.courses.model.Course
import com.nhlstenden.appdev.shared.ui.base.BaseFragment
import kotlinx.coroutines.launch
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class CoursesFragment : BaseFragment() {
    private val viewModel: CourseViewModel by viewModels()
    private lateinit var coursesList: RecyclerView
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

        coursesList = view.findViewById(R.id.coursesList)
        filterChipGroup = view.findViewById(R.id.filterChipGroup)

        setupCoursesList()
        setupFilterChips()
        observeViewModel()
        
        viewModel.loadCourses()
    }

    private fun setupCoursesList() {
        coursesList.layoutManager = LinearLayoutManager(context)
        adapter = CourseAdapter { course ->
            NavigationManager.navigateToCourseTasks(requireActivity(), course.id)
        }
        coursesList.adapter = adapter
    }

    private fun setupFilterChips() {
        filterChipGroup.setOnCheckedStateChangeListener { group, checkedIds ->
            // TODO: Implement filtering when connected to backend
        }
    }

    private fun observeViewModel() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.courses.collect { courses ->
                    val mapped = courses.map { domainCourse ->
                        Course(
                            id = domainCourse.id,
                            title = domainCourse.title,
                            difficulty = domainCourse.difficulty,
                            description = domainCourse.description,
                            imageResId = domainCourse.imageResId,
                            progress = 0,
                            totalTasks = domainCourse.totalTasks,
                        )
                    }
                    adapter.submitList(mapped)
                }
            }
        }
    }
} 