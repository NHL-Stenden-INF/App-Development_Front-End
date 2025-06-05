package com.nhlstenden.appdev.features.courses

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
import com.nhlstenden.appdev.features.courses.model.Task
import com.nhlstenden.appdev.R
import com.nhlstenden.appdev.databinding.FragmentCourseBinding
import com.google.android.material.progressindicator.LinearProgressIndicator
import android.widget.TextView
import kotlinx.coroutines.launch
import dagger.hilt.android.AndroidEntryPoint
import com.nhlstenden.appdev.features.task.screens.TaskActivity
import com.nhlstenden.appdev.core.utils.NavigationManager
import android.media.MediaPlayer
import com.nhlstenden.appdev.features.profile.viewmodels.ProfileViewModel

@AndroidEntryPoint
class CourseFragment : Fragment() {
    private var _binding: FragmentCourseBinding? = null
    private val binding get() = _binding!!
    private val viewModel: CourseViewModel by viewModels()
    private lateinit var taskAdapter: TaskAdapter
    private var mediaPlayer: MediaPlayer? = null
    private val profileViewModel: ProfileViewModel by viewModels()

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

        // Set user data for ProfileViewModel (required for profile loading)
        val userData = arguments?.getParcelable<com.nhlstenden.appdev.core.models.User>("USER_DATA")
            ?: com.nhlstenden.appdev.core.utils.UserManager.getCurrentUser()
        userData?.let { user ->
            android.util.Log.d("CourseFragment", "user.id=${user.id}, user.authToken=${user.authToken}")
            profileViewModel.setUserData(user)
        }

        viewLifecycleOwner.lifecycleScope.launch {
            profileViewModel.profileState.collect { state ->
                if (state is ProfileViewModel.ProfileState.Success) {
                    val courseId = arguments?.getString("COURSE_ID")
                    if (courseId != null) {
                        val parser = CourseParser(requireContext())
                        val course = parser.loadCourseByTitle(courseId)
                        if (course != null) {
                            binding.courseTitle.text = course.title
                            binding.courseDescription.text = course.description
                            maybePlayLobbyMusic(course.title, state.profile.unlockedRewardIds ?: emptyList())
                        }
                    }
                }
            }
        }
        profileViewModel.loadProfile()

        // Set up back button
        binding.backButton.setOnClickListener {
            NavigationManager.navigateBack(requireActivity())
        }
    }

    private fun setupRecyclerView() {
        taskAdapter = TaskAdapter { task ->
            // Open TaskActivity for the selected task using the correct intent method
            val intent = TaskActivity.newIntent(requireContext(), task.id)
            startActivity(intent)
        }
        binding.tasksList.adapter = taskAdapter
    }

    private fun observeViewModel() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.tasksState.collect { state ->
                when (state) {
                    is CourseViewModel.TasksState.Loading -> {
                        binding.loadingProgressBar.visibility = View.VISIBLE
                        binding.tasksList.visibility = View.GONE
                        binding.errorTextView.visibility = View.GONE
                        binding.swipeRefreshLayout.isRefreshing = false
                    }
                    is CourseViewModel.TasksState.Success -> {
                        binding.loadingProgressBar.visibility = View.GONE
                        binding.tasksList.visibility = View.VISIBLE
                        binding.errorTextView.visibility = View.GONE
                        taskAdapter.submitList(state.tasks)
                        binding.swipeRefreshLayout.isRefreshing = false
                    }
                    is CourseViewModel.TasksState.Error -> {
                        binding.loadingProgressBar.visibility = View.GONE
                        binding.tasksList.visibility = View.GONE
                        binding.errorTextView.visibility = View.VISIBLE
                        binding.errorTextView.text = state.message
                        binding.swipeRefreshLayout.isRefreshing = false
                    }
                }
            }
        }
    }

    private fun maybePlayLobbyMusic(courseTitle: String, unlockedRewardIds: List<Int>) {
        val sharedPrefs = requireContext().getSharedPreferences("reward_settings", android.content.Context.MODE_PRIVATE)
        val isMusicLobbyEnabled = sharedPrefs.getBoolean("music_lobby_enabled", true)
        val unlocked = unlockedRewardIds.contains(11)
        if (!isMusicLobbyEnabled || !unlocked) return
        val musicResId = when (courseTitle.trim().uppercase()) {
            "CSS" -> com.nhlstenden.appdev.R.raw.css_themesong
            "HTML" -> com.nhlstenden.appdev.R.raw.html_themesong
            "SQL" -> com.nhlstenden.appdev.R.raw.sql_themesong
            else -> com.nhlstenden.appdev.R.raw.default_themesong
        }
        mediaPlayer = MediaPlayer.create(requireContext(), musicResId)
        mediaPlayer?.isLooping = true
        mediaPlayer?.start()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        mediaPlayer?.stop()
        mediaPlayer?.release()
        mediaPlayer = null
        _binding = null
    }

    private class TaskAdapter(
        private val onClick: (Task) -> Unit
    ) : ListAdapter<Task, TaskAdapter.TaskViewHolder>(DiffCallback) {
        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): TaskViewHolder {
            val view = LayoutInflater.from(parent.context)
                .inflate(R.layout.item_course, parent, false)
            return TaskViewHolder(view, onClick)
        }
        override fun onBindViewHolder(holder: TaskViewHolder, position: Int) {
            holder.bind(getItem(position))
        }
        class TaskViewHolder(
            itemView: View,
            private val onClick: (Task) -> Unit
        ) : RecyclerView.ViewHolder(itemView) {
            private val titleText: TextView = itemView.findViewById(R.id.courseTitle)
            private val descriptionText: TextView = itemView.findViewById(R.id.courseDescription)
            private val difficultyText: TextView = itemView.findViewById(R.id.difficultyLevel)
            private val progressBar: LinearProgressIndicator = itemView.findViewById(R.id.progressBar)
            fun bind(task: Task) {
                titleText.text = task.title
                descriptionText.text = task.description
                difficultyText.text = task.difficulty
//                TODO: Fix progress bar
                progressBar.progress = 0
                itemView.setOnClickListener { onClick(task) }
            }
        }
        companion object {
            val DiffCallback = object : DiffUtil.ItemCallback<Task>() {
                override fun areItemsTheSame(oldItem: Task, newItem: Task): Boolean = oldItem.id == newItem.id
                override fun areContentsTheSame(oldItem: Task, newItem: Task): Boolean = oldItem == newItem
            }
        }
    }
} 