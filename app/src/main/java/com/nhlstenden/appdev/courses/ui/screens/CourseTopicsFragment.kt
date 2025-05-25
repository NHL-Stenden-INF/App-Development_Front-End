package com.nhlstenden.appdev.courses.ui.screens

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.nhlstenden.appdev.databinding.FragmentCourseTopicsBinding
import com.nhlstenden.appdev.task.ui.screens.TaskActivity
import com.nhlstenden.appdev.courses.ui.adapters.TopicsAdapter
import com.nhlstenden.appdev.courses.ui.viewmodels.CourseTopicsViewModel
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch

@AndroidEntryPoint
class CourseTopicsFragment : Fragment() {
    private var _binding: FragmentCourseTopicsBinding? = null
    private val binding get() = _binding!!
    private val viewModel: CourseTopicsViewModel by viewModels()
    private lateinit var adapter: TopicsAdapter

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentCourseTopicsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupRecyclerView()
        setupSwipeRefresh()
        observeTopicsState()
    }

    private fun setupRecyclerView() {
        adapter = TopicsAdapter { topic ->
            startActivity(TaskActivity.newIntent(requireContext(), topic.id))
        }

        binding.topicsList.apply {
            layoutManager = LinearLayoutManager(context)
            adapter = this@CourseTopicsFragment.adapter
        }
    }

    private fun setupSwipeRefresh() {
        binding.swipeRefreshLayout.setOnRefreshListener {
            viewModel.loadTopics()
        }
    }

    private fun observeTopicsState() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.topicsState.collect { state ->
                when (state) {
                    is CourseTopicsViewModel.TopicsState.Loading -> {
                        binding.loadingProgressBar.visibility = View.VISIBLE
                        binding.topicsList.visibility = View.GONE
                    }
                    is CourseTopicsViewModel.TopicsState.Success -> {
                        binding.loadingProgressBar.visibility = View.GONE
                        binding.topicsList.visibility = View.VISIBLE
                        adapter.submitList(state.topics)
                    }
                    is CourseTopicsViewModel.TopicsState.Error -> {
                        binding.loadingProgressBar.visibility = View.GONE
                        binding.topicsList.visibility = View.GONE
                        Toast.makeText(context, state.message, Toast.LENGTH_SHORT).show()
                    }
                }
                binding.swipeRefreshLayout.isRefreshing = false
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
} 