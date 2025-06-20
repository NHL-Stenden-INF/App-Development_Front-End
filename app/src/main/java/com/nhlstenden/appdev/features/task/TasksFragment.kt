package com.nhlstenden.appdev.features.task

import android.app.Application
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.button.MaterialButton
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.textfield.TextInputEditText
import com.nhlstenden.appdev.R
import com.nhlstenden.appdev.core.utils.NavigationManager
import com.nhlstenden.appdev.features.courses.repositories.CoursesRepositoryImpl
import dagger.hilt.android.AndroidEntryPoint
import com.nhlstenden.appdev.features.courses.adapters.CourseAdapter
import kotlinx.coroutines.runBlocking
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class TasksFragment : Fragment() {
    @Inject lateinit var coursesRepository: CoursesRepositoryImpl
    private lateinit var tasksList: RecyclerView
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

        tasksList = view.findViewById(R.id.coursesList)
        searchEditText = view.findViewById(R.id.searchEditText)
        filterButton = view.findViewById(R.id.filterButton)

        adapter = CourseAdapter { course ->
            // Use NavigationManager for consistent navigation and back behavior
            NavigationManager.navigateToCourseTasks(requireActivity(), course.id)
        }
        tasksList.layoutManager = LinearLayoutManager(context)
        tasksList.adapter = adapter

        setupTasksList()
        setupSearchBar()
        setupFilterButton()
    }

    private fun setupTasksList() {
        lifecycleScope.launch {
            val courses = coursesRepository.getCoursesWithoutProgress()
            adapter.submitList(courses)
        }
    }

    private fun setupSearchBar() {
        searchEditText.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {
                // Not needed
            }

            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                // Not needed
            }

            override fun afterTextChanged(s: Editable?) {
                // Implement search functionality if needed
            }
        })
    }

    private fun setupFilterButton() {
        filterButton.setOnClickListener {
            showFilterDialog()
        }
    }

    private fun showFilterDialog() {
        val starOptions = arrayOf("★☆☆☆☆", "★★☆☆☆", "★★★☆☆", "★★★★☆", "★★★★★")
        
        MaterialAlertDialogBuilder(requireContext())
            .setTitle("Filter by Difficulty (Stars)")
            .setSingleChoiceItems(starOptions, -1) { dialog, which ->
                // Implement star filtering if needed in future
                dialog.dismiss()
            }
            .setNeutralButton("Clear") { dialog, _ ->
                // Clear filters if needed
                dialog.dismiss()
            }
            .setNegativeButton("Cancel") { dialog, _ ->
                dialog.dismiss()
            }
            .show()
    }
}