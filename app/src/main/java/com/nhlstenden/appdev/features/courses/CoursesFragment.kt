package com.nhlstenden.appdev.features.courses

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.widget.addTextChangedListener
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.button.MaterialButton
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.textfield.TextInputEditText
import com.nhlstenden.appdev.R
import com.nhlstenden.appdev.features.courses.screens.CourseAdapter
import com.nhlstenden.appdev.core.utils.NavigationManager
import com.nhlstenden.appdev.features.courses.model.Course
import com.nhlstenden.appdev.shared.ui.base.BaseFragment
import kotlinx.coroutines.launch
import dagger.hilt.android.AndroidEntryPoint
import com.nhlstenden.appdev.core.utils.UserManager

@AndroidEntryPoint
class CoursesFragment : BaseFragment() {
    private val viewModel: CourseViewModel by viewModels()
    private lateinit var coursesList: RecyclerView
    private lateinit var searchEditText: TextInputEditText
    private lateinit var filterButton: MaterialButton
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
        searchEditText = view.findViewById(R.id.searchEditText)
        filterButton = view.findViewById(R.id.filterButton)

        setupCoursesList()
        setupSearchBar()
        setupFilterButton()
        observeViewModel()
    }

    override fun onResume() {
        super.onResume()
        val currentUser = UserManager.getCurrentUser()
        if (currentUser != null) {
            viewModel.loadCoursesWithProgress(currentUser)
        }
    }

    private fun setupCoursesList() {
        coursesList.layoutManager = LinearLayoutManager(context)
        adapter = CourseAdapter { course ->
            NavigationManager.navigateToCourseTasks(requireActivity(), course.id)
        }
        coursesList.adapter = adapter
    }

    private fun setupSearchBar() {
        searchEditText.addTextChangedListener { text ->
            viewModel.updateSearchQuery(text?.toString() ?: "")
        }
    }

    private fun setupFilterButton() {
        filterButton.setOnClickListener {
            showFilterDialog()
        }
    }

    private fun showFilterDialog() {
        val difficulties = arrayOf("Beginner", "Intermediate", "Advanced")
        val currentDifficulty = viewModel.selectedDifficulty.value
        val checkedItem = difficulties.indexOfFirst { it.equals(currentDifficulty, ignoreCase = true) }

        MaterialAlertDialogBuilder(requireContext())
            .setTitle("Filter by Difficulty")
            .setSingleChoiceItems(difficulties, checkedItem) { dialog, which ->
                viewModel.setDifficultyFilter(difficulties[which])
                dialog.dismiss()
            }
            .setNeutralButton("Clear") { dialog, _ ->
                viewModel.clearFilters()
                dialog.dismiss()
            }
            .setNegativeButton("Cancel") { dialog, _ ->
                dialog.dismiss()
            }
            .show()
    }

    private fun observeViewModel() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.filteredCourses.collect { courses ->
                    adapter.submitList(courses)
                }
            }
        }
    }

    fun refreshCourses() {
        val currentUser = UserManager.getCurrentUser()
        if (currentUser != null) {
            viewModel.loadCoursesWithProgress(currentUser)
        }
    }
} 