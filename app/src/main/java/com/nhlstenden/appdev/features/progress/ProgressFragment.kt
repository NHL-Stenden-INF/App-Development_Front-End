package com.nhlstenden.appdev.features.progress

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.TextView
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.viewpager2.widget.ViewPager2
import com.github.mikephil.charting.charts.PieChart
import com.nhlstenden.appdev.R
import com.nhlstenden.appdev.features.courses.CourseFragment
import com.nhlstenden.appdev.features.progress.adapters.CourseProgressAdapter
import com.nhlstenden.appdev.features.progress.utils.ChartHelper
import com.nhlstenden.appdev.features.progress.viewmodels.ProgressViewModel
import com.nhlstenden.appdev.core.utils.NavigationManager
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch

@AndroidEntryPoint
class ProgressFragment : Fragment() {
    private val viewModel: ProgressViewModel by viewModels()
    private lateinit var pieChart: PieChart
    private lateinit var courseProgressList: RecyclerView
    private lateinit var overallProgressTitle: TextView
    private lateinit var overallProgressPercentage: TextView
    private lateinit var adapter: CourseProgressAdapter

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_progress, container, false)

        initializeViews(view)
        setupRecyclerView()
        observeViewModel()
        
        return view
    }

    override fun onResume() {
        super.onResume()
        viewModel.loadData()
    }

    private fun initializeViews(view: View) {
        pieChart = view.findViewById(R.id.tasksPieChart)
        courseProgressList = view.findViewById(R.id.courseProgressList)
        overallProgressTitle = view.findViewById(R.id.overallProgressTitle)
        overallProgressPercentage = view.findViewById(R.id.overallProgressPercentage)
        
        courseProgressList.contentDescription = "List of course progress"
        overallProgressTitle.text = getString(R.string.overall_progress_title)
    }

    private fun setupRecyclerView() {
        adapter = CourseProgressAdapter { courseId ->
            NavigationManager.navigateToCourseTasks(requireActivity(), courseId)
        }
        
        courseProgressList.apply {
            layoutManager = LinearLayoutManager(context)
            adapter = this@ProgressFragment.adapter
        }
    }

    private fun observeViewModel() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.overallProgress.collect { overallProgress ->
                overallProgressPercentage.text = getString(
                    R.string.overall_progress_percentage, 
                    overallProgress.completionPercentage
                )
                ChartHelper.setupPieChart(pieChart, overallProgress)
            }
        }

        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.courseProgressList.collect { courseList ->
                adapter.submitList(courseList)
            }
        }

        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.uiState.collect { uiState ->
                // Handle loading and error states here if needed
                // For now, keeping it simple
            }
        }
        
        // Initial data load
        viewModel.loadData()
    }


    // Legacy navigation method - kept for potential future use
    private fun navigateToCourse(courseName: String) {
        val fragment = CourseFragment().apply {
            arguments = Bundle().apply {
                putString("courseName", courseName)
            }
        }

        requireActivity().findViewById<FrameLayout>(R.id.fragment_container).visibility = View.VISIBLE
        requireActivity().findViewById<ViewPager2>(R.id.viewPager).visibility = View.GONE
        
        parentFragmentManager.beginTransaction()
            .replace(R.id.fragment_container, fragment)
            .addToBackStack(null)
            .commit()
    }
}