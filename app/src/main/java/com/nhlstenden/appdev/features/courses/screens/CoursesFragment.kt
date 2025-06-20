package com.nhlstenden.appdev.features.courses.screens

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
import com.nhlstenden.appdev.features.courses.adapters.CourseAdapter
import com.nhlstenden.appdev.core.utils.NavigationManager
import com.nhlstenden.appdev.features.courses.model.Course
import com.nhlstenden.appdev.core.ui.base.BaseFragment
import com.nhlstenden.appdev.features.courses.viewmodels.CoursesViewModel
import com.nhlstenden.appdev.core.utils.DifficultyFormatter
import kotlinx.coroutines.launch
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class CoursesFragment : BaseFragment() {
    private val viewModel: CoursesViewModel by viewModels()
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
        viewModel.loadCoursesWithProgress()
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
        val starOptions = DifficultyFormatter.getStarOptions()
        val currentStars = viewModel.selectedStars.value
        val checkedItem = if (currentStars != null) currentStars - 1 else -1

        MaterialAlertDialogBuilder(requireContext())
            .setTitle("Filter by Difficulty (Stars)")
            .setSingleChoiceItems(starOptions, checkedItem) { dialog, which ->
                viewModel.setStarFilter(which + 1) // Stars are 1-indexed
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
                launch {
                    viewModel.filteredCourses.collect { courses ->
                        adapter.submitList(courses)
                    }
                }
                
                launch {
                    viewModel.error.collect { error ->
                        error?.let {
                            // Handle error display - could show toast or snackbar
                            handleError(Exception(it))
                        }
                    }
                }
                
                launch {
                    viewModel.isLoading.collect { isLoading ->
                        // Handle loading state - could show/hide progress indicator
                        // filterButton.isEnabled = !isLoading
                    }
                }
            }
        }
    }

    fun refreshCourses() {
        viewModel.refreshCourses()
    }
} 