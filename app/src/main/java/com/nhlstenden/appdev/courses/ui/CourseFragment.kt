package com.nhlstenden.appdev.courses.ui

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.nhlstenden.appdev.courses.domain.models.Topic
import com.nhlstenden.appdev.courses.ui.viewmodels.CourseViewModel
import com.nhlstenden.appdev.R
import com.nhlstenden.appdev.databinding.FragmentCourseBinding
import com.google.android.material.progressindicator.LinearProgressIndicator
import android.widget.TextView
import kotlinx.coroutines.launch
import dagger.hilt.android.AndroidEntryPoint
import android.content.Intent
import com.nhlstenden.appdev.task.ui.screens.TaskActivity

@AndroidEntryPoint
class CourseFragment : Fragment() {
    private var _binding: FragmentCourseBinding? = null
    private val binding get() = _binding!!
    private val viewModel: CourseViewModel by viewModels()
    private lateinit var topicAdapter: TopicAdapter

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        _binding = FragmentCourseBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupRecyclerView()
        observeViewModel()

        // Get the courseId from arguments and load topics
        val courseId = arguments?.getString("courseId")
        if (courseId != null) {
            viewModel.loadTopics(courseId)
            // Set course title and description from XML or mock data
            val parser = com.nhlstenden.appdev.courses.parser.CourseParser(requireContext())
            val course = parser.loadCourseById(courseId)
            if (course != null) {
                binding.courseTitle.text = course.title
                binding.courseDescription.text = course.description
            }
        }
        // Set up back button
        binding.backButton.setOnClickListener {
            com.nhlstenden.appdev.shared.navigation.NavigationManager.navigateBack(requireActivity())
        }
    }

    private fun setupRecyclerView() {
        topicAdapter = TopicAdapter { topic ->
            // Open TaskActivity for the selected topic using the correct intent method
            val intent = TaskActivity.newIntent(requireContext(), topic.id)
            startActivity(intent)
        }
        binding.topicsList.adapter = topicAdapter
    }

    private fun observeViewModel() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.topicsState.collect { state ->
                when (state) {
                    is CourseViewModel.TopicsState.Loading -> {
                        binding.loadingProgressBar.visibility = View.VISIBLE
                        binding.topicsList.visibility = View.GONE
                        binding.errorTextView.visibility = View.GONE
                        binding.swipeRefreshLayout.isRefreshing = false
                    }
                    is CourseViewModel.TopicsState.Success -> {
                        binding.loadingProgressBar.visibility = View.GONE
                        binding.topicsList.visibility = View.VISIBLE
                        binding.errorTextView.visibility = View.GONE
                        topicAdapter.submitList(state.topics)
                        binding.swipeRefreshLayout.isRefreshing = false
                    }
                    is CourseViewModel.TopicsState.Error -> {
                        binding.loadingProgressBar.visibility = View.GONE
                        binding.topicsList.visibility = View.GONE
                        binding.errorTextView.visibility = View.VISIBLE
                        binding.errorTextView.text = state.message
                        binding.swipeRefreshLayout.isRefreshing = false
                    }
                }
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    private class TopicAdapter(
        private val onClick: (Topic) -> Unit
    ) : ListAdapter<Topic, TopicAdapter.TopicViewHolder>(DiffCallback) {
        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): TopicViewHolder {
            val view = LayoutInflater.from(parent.context)
                .inflate(R.layout.item_course, parent, false)
            return TopicViewHolder(view, onClick)
        }
        override fun onBindViewHolder(holder: TopicViewHolder, position: Int) {
            holder.bind(getItem(position))
        }
        class TopicViewHolder(
            itemView: View,
            private val onClick: (Topic) -> Unit
        ) : RecyclerView.ViewHolder(itemView) {
            private val titleText: TextView = itemView.findViewById(R.id.courseTitle)
            private val descriptionText: TextView = itemView.findViewById(R.id.courseDescription)
            private val difficultyText: TextView = itemView.findViewById(R.id.difficultyLevel)
            private val progressBar: LinearProgressIndicator = itemView.findViewById(R.id.progressBar)
            fun bind(topic: Topic) {
                titleText.text = topic.title
                descriptionText.text = topic.description
                difficultyText.text = topic.difficulty
                progressBar.progress = topic.progress
                itemView.setOnClickListener { onClick(topic) }
            }
        }
        companion object {
            val DiffCallback = object : DiffUtil.ItemCallback<Topic>() {
                override fun areItemsTheSame(oldItem: Topic, newItem: Topic): Boolean = oldItem.id == newItem.id
                override fun areContentsTheSame(oldItem: Topic, newItem: Topic): Boolean = oldItem == newItem
            }
        }
    }
} 