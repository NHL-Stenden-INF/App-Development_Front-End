package com.nhlstenden.appdev.features.course.screens

import android.app.Application
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import com.nhlstenden.appdev.core.ui.base.BaseFragment
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import androidx.core.content.ContextCompat
import com.nhlstenden.appdev.features.courses.model.Task
import com.nhlstenden.appdev.R
import com.nhlstenden.appdev.databinding.FragmentCourseBinding
import com.google.android.material.progressindicator.LinearProgressIndicator
import android.widget.TextView
import kotlinx.coroutines.launch
import dagger.hilt.android.AndroidEntryPoint
import com.nhlstenden.appdev.features.task.screens.TaskActivity
import com.nhlstenden.appdev.core.utils.NavigationManager
import android.util.Log
import androidx.recyclerview.widget.LinearLayoutManager
import com.nhlstenden.appdev.core.repositories.UserRepository
import com.nhlstenden.appdev.features.profile.viewmodels.ProfileViewModel
import com.nhlstenden.appdev.MainActivity
import android.widget.ImageView
import com.daimajia.numberprogressbar.NumberProgressBar
import android.content.Intent
import android.app.Activity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import com.nhlstenden.appdev.core.services.MusicManager
import com.nhlstenden.appdev.utils.RewardChecker
import com.nhlstenden.appdev.core.repositories.AuthRepository
import com.nhlstenden.appdev.features.course.viewmodels.CourseViewModel
import com.nhlstenden.appdev.features.courses.screens.CoursesFragment
import javax.inject.Inject

@AndroidEntryPoint
class CourseFragment : BaseFragment() {
    private lateinit var binding: FragmentCourseBinding
    private val viewModel: CourseViewModel by viewModels()
    private val profileViewModel: ProfileViewModel by viewModels()
    private var taskAdapter: TaskAdapter? = null
    
    @Inject
    lateinit var authRepository: AuthRepository
    
    @Inject
    lateinit var musicManager: MusicManager

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        return inflater.inflate(R.layout.fragment_course, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding = FragmentCourseBinding.bind(view)

        setupRecyclerView()
        observeViewModel()
        setupSwipeRefresh()
        setupBackButton()
        setupMusic()

        // Load tasks for the course
        val courseId = arguments?.getString("COURSE_ID") ?: return
        val currentUser = authRepository.getCurrentUserSync()
        if (currentUser != null) {
            viewModel.loadTasks(courseId, currentUser)
            viewModel.loadCourseInfo(courseId)
        }

        // Observe course progress and update adapter
        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.courseProgress.collect { progress ->
                if (taskAdapter == null) {
                    taskAdapter = TaskAdapter(progress) { task ->
                        val intent = TaskActivity.newIntent(requireContext(), task.id)
                        startActivityForResult(intent, TASK_COMPLETION_REQUEST_CODE)
                    }
                    binding.tasksList.adapter = taskAdapter
                } else {
                    taskAdapter?.updateProgress(progress)
                }
                // Re-submit the current list if available
                val currentTasks = (viewModel.tasksState.value as? CourseViewModel.TasksState.Success)?.tasks
                if (currentTasks != null) {
                    taskAdapter?.submitList(currentTasks)
                }
            }
        }

        // Set up back button
        binding.backButton.setOnClickListener {
            // Trigger refresh of courses list before navigating back
            val currentUser = authRepository.getCurrentUserSync()
            if (currentUser != null) {
                (requireActivity() as? MainActivity)?.let { mainActivity ->
                    val coursesFragment = mainActivity.supportFragmentManager.fragments.find { it is CoursesFragment }
                    if (coursesFragment is CoursesFragment) {
                        coursesFragment.refreshCourses()
                    }
                }
            }
            (requireActivity() as? MainActivity)?.navigateToTab("courses")
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == TASK_COMPLETION_REQUEST_CODE && resultCode == Activity.RESULT_OK) {
            // Refresh the course progress
            val courseId = arguments?.getString("COURSE_ID") ?: return
            val currentUser = authRepository.getCurrentUserSync()
            if (currentUser != null) {
                // Reload tasks and course progress
                viewModel.loadTasks(courseId, currentUser)
                viewModel.loadCourseInfo(courseId)
                // Force refresh the view
                binding.swipeRefreshLayout.isRefreshing = true
            }
        }
    }

    override fun onResume() {
        super.onResume()
        val courseId = arguments?.getString("COURSE_ID") ?: return
        val currentUser = authRepository.getCurrentUserSync()
        if (currentUser != null) {
            // Force refresh the view
            binding.swipeRefreshLayout.isRefreshing = true
            // Reload tasks and course progress
            viewModel.loadTasks(courseId, currentUser)
            viewModel.loadCourseInfo(courseId)
        }
    }

    override fun onPause() {
        super.onPause()
        musicManager.pauseMusic()
    }

    override fun onDestroy() {
        super.onDestroy()
        musicManager.stopMusic()
    }

    private fun setupRecyclerView() {
        binding.tasksList.layoutManager = LinearLayoutManager(requireContext())
    }

    private fun observeViewModel() {
        viewModel.tasksState.observe(viewLifecycleOwner) { state ->
            when (state) {
                is CourseViewModel.TasksState.Loading -> {
                    binding.swipeRefreshLayout.isRefreshing = true
                }
                is CourseViewModel.TasksState.Success -> {
                    binding.swipeRefreshLayout.isRefreshing = false
                    taskAdapter?.submitList(state.tasks)
                }
                is CourseViewModel.TasksState.Error -> {
                    binding.swipeRefreshLayout.isRefreshing = false
                    Log.e("CourseFragment", "Error loading tasks: ${state.message}")
                }
            }
        }

        viewModel.courseInfo.observe(viewLifecycleOwner) { course ->
            if (course != null) {
                binding.courseTitle.text = course.title
                binding.courseDescription.text = course.description
            } else {
                // Fallback if course info couldn't be loaded
                val courseId = arguments?.getString("COURSE_ID") ?: ""
                binding.courseTitle.text = courseId.replace("_", " ").replaceFirstChar { it.uppercase() }
                binding.courseDescription.text = "Course information"
            }
        }
    }

    private fun setupSwipeRefresh() {
        binding.swipeRefreshLayout.setColorSchemeResources(
            R.color.colorPrimary,
            R.color.colorAccent
        )
        binding.swipeRefreshLayout.setOnRefreshListener {
            val courseId = arguments?.getString("COURSE_ID") ?: return@setOnRefreshListener
            val currentUser = authRepository.getCurrentUserSync()
            if (currentUser != null) {
                viewModel.loadTasks(courseId, currentUser)
                viewModel.loadCourseInfo(courseId)
            }
        }
    }

    private fun setupBackButton() {
        binding.backButton.setOnClickListener {
            requireActivity().onBackPressed()
        }
    }

    private fun setupMusic() {
        val courseId = arguments?.getString("COURSE_ID") ?: return
        launchSafely {
            musicManager.startCourseMusic(courseId)
        }
    }

    companion object {
        private const val TASK_COMPLETION_REQUEST_CODE = 1001
        
        fun newInstance(courseId: String): CourseFragment {
            return CourseFragment().apply {
                arguments = Bundle().apply {
                    putString("COURSE_ID", courseId)
                }
            }
        }
    }

    class TaskAdapter(
        private var courseProgress: Map<String, Int>,
        private val onTaskClick: (Task) -> Unit
    ) : ListAdapter<Task, TaskAdapter.TaskViewHolder>(TaskDiffCallback()) {

        fun updateProgress(progress: Map<String, Int>) {
            courseProgress = progress
            notifyDataSetChanged()
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): TaskViewHolder {
            val view = LayoutInflater.from(parent.context).inflate(R.layout.item_task, parent, false)
            return TaskViewHolder(view)
        }

        override fun onBindViewHolder(holder: TaskViewHolder, position: Int) {
            val task = getItem(position)
            holder.bind(task, courseProgress[task.id] ?: 0, onTaskClick)
        }

        class TaskViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
            private val taskTitle: TextView = itemView.findViewById(R.id.taskTitle)
            private val taskDescription: TextView = itemView.findViewById(R.id.taskDescription)
            private val taskIcon: ImageView = itemView.findViewById(R.id.taskIcon)
            private val progressBar: NumberProgressBar = itemView.findViewById(R.id.progressBar)

            fun bind(task: Task, progress: Int, onTaskClick: (Task) -> Unit) {
                taskTitle.text = task.title
                taskDescription.text = task.description
                
                // Determine task state based on progress value
                val isCompleted = progress >= task.questionCount && progress > 0
                val isUnlocked = progress == -1 || isCompleted // -1 means unlocked but not completed
                val isLocked = progress == 0
                
                // Hide progress bar - we use icons to show task status instead
                progressBar.visibility = View.GONE
                
                when {
                    isCompleted -> {
                        // Task is completed
                        taskIcon.setImageResource(R.drawable.ic_check)
                        itemView.alpha = 0.7f
                        taskTitle.setTextColor(ContextCompat.getColor(itemView.context, R.color.textSecondary))
                        itemView.isClickable = true
                    }
                    isUnlocked -> {
                        // Task is unlocked but not completed
                        taskIcon.setImageResource(R.drawable.ic_play_arrow)
                        itemView.alpha = 1.0f
                        taskTitle.setTextColor(ContextCompat.getColor(itemView.context, R.color.textPrimary))
                        itemView.isClickable = true
                    }
                    else -> {
                        // Task is locked
                        taskIcon.setImageResource(android.R.drawable.ic_lock_lock)
                        itemView.alpha = 0.5f
                        taskTitle.setTextColor(ContextCompat.getColor(itemView.context, R.color.textSecondary))
                        itemView.isClickable = false
                    }
                }
                
                itemView.setOnClickListener {
                    if (isUnlocked) {
                        onTaskClick(task)
                    }
                }
            }
        }

        class TaskDiffCallback : DiffUtil.ItemCallback<Task>() {
            override fun areItemsTheSame(oldItem: Task, newItem: Task): Boolean {
                return oldItem.id == newItem.id
            }

            override fun areContentsTheSame(oldItem: Task, newItem: Task): Boolean {
                return oldItem == newItem
            }
        }
    }
} 