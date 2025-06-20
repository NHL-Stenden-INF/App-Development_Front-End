package com.nhlstenden.appdev.features.course.screens

import android.app.Application
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
import androidx.core.content.ContextCompat
import com.nhlstenden.appdev.features.course.models.Task
import com.nhlstenden.appdev.R
import com.nhlstenden.appdev.databinding.FragmentCourseBinding
import com.google.android.material.progressindicator.LinearProgressIndicator
import android.widget.TextView
import kotlinx.coroutines.launch
import dagger.hilt.android.AndroidEntryPoint
import com.nhlstenden.appdev.features.task.screens.TaskActivity
import com.nhlstenden.appdev.core.utils.NavigationManager
import android.media.MediaPlayer
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
import android.media.AudioManager
import android.media.AudioAttributes
import android.os.Build
import android.content.Context
import android.media.AudioFocusRequest
import com.nhlstenden.appdev.utils.RewardChecker
import com.nhlstenden.appdev.core.repositories.AuthRepository
import com.nhlstenden.appdev.core.repositories.SettingsRepository
import com.nhlstenden.appdev.features.profile.repositories.SettingsRepositoryImpl.SettingsConstants
import com.nhlstenden.appdev.features.course.viewmodels.CourseViewModel
import com.nhlstenden.appdev.features.course.utils.CourseParser
import com.nhlstenden.appdev.features.courses.screens.CoursesFragment
import javax.inject.Inject

@AndroidEntryPoint
class CourseFragment : Fragment() {
    private lateinit var binding: FragmentCourseBinding
    private val viewModel: CourseViewModel by viewModels()
    private val profileViewModel: ProfileViewModel by viewModels()
    private var taskAdapter: TaskAdapter? = null
    private var mediaPlayer: MediaPlayer? = null
    private var audioManager: AudioManager? = null
    private var audioFocusRequest: AudioFocusRequest? = null
    
    @Inject
    lateinit var rewardChecker: RewardChecker
    
    @Inject
    lateinit var authRepository: AuthRepository

    @Inject
    lateinit var settingsRepository: SettingsRepository

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

        audioManager = requireContext().getSystemService(Context.AUDIO_SERVICE) as AudioManager
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            audioFocusRequest = AudioFocusRequest.Builder(AudioManager.AUDIOFOCUS_GAIN)
                .setAudioAttributes(AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_GAME)
                    .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                    .build())
                .setOnAudioFocusChangeListener { focusChange ->
                    when (focusChange) {
                        AudioManager.AUDIOFOCUS_LOSS -> {
                            mediaPlayer?.pause()
                        }
                        AudioManager.AUDIOFOCUS_LOSS_TRANSIENT -> {
                            mediaPlayer?.pause()
                        }
                        AudioManager.AUDIOFOCUS_GAIN -> {
                            mediaPlayer?.start()
                        }
                        else -> {
                            // Handle any other focus changes if needed
                        }
                    }
                }
                .build()
        }

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
            
            // Load and display course info
            lifecycleScope.launch(Dispatchers.IO) {
                try {
                    val courseParser = CourseParser(requireContext())
                    val course = courseParser.loadCourseByTitle(courseId)
                    withContext(Dispatchers.Main) {
                        course?.let {
                            binding.courseTitle.text = it.title
                            binding.courseDescription.text = it.description
                        }
                    }
                } catch (e: Exception) {
                    Log.e("CourseFragment", "Error loading course info", e)
                }
            }
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
        }
    }

    override fun onPause() {
        super.onPause()
        stopMusic()
    }

    override fun onDestroy() {
        super.onDestroy()
        stopMusic()
        mediaPlayer?.release()
        mediaPlayer = null
        audioManager?.let { am ->
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                audioFocusRequest?.let { request ->
                    am.abandonAudioFocusRequest(request)
                }
            }
        }
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
            }
        }
    }

    private fun setupBackButton() {
        binding.backButton.setOnClickListener {
            requireActivity().onBackPressed()
        }
    }

    private fun setupMusic() {
        // For now, always start music. Settings integration can be added later.
        startMusic()
    }

    private fun startMusic() {
        if (mediaPlayer?.isPlaying == true) return
        
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                audioFocusRequest?.let { request ->
                    val result = audioManager?.requestAudioFocus(request)
                    if (result == AudioManager.AUDIOFOCUS_REQUEST_GRANTED) {
                        playMusic()
                    }
                }
            } else {
                @Suppress("DEPRECATION")
                val result = audioManager?.requestAudioFocus(
                    null,
                    AudioManager.STREAM_MUSIC,
                    AudioManager.AUDIOFOCUS_GAIN
                )
                if (result == AudioManager.AUDIOFOCUS_REQUEST_GRANTED) {
                    playMusic()
                }
            }
        } catch (e: Exception) {
            Log.e("CourseFragment", "Error starting music", e)
        }
    }

    private fun playMusic() {
        try {
            if (mediaPlayer == null) {
                mediaPlayer = MediaPlayer.create(requireContext(), R.raw.default_themesong)
                mediaPlayer?.isLooping = true
                mediaPlayer?.setVolume(0.3f, 0.3f)
            }
            mediaPlayer?.start()
        } catch (e: Exception) {
            Log.e("CourseFragment", "Error playing music", e)
        }
    }

    private fun stopMusic() {
        try {
            mediaPlayer?.pause()
            audioManager?.let { am ->
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    audioFocusRequest?.let { request ->
                        am.abandonAudioFocusRequest(request)
                    }
                } else {
                    @Suppress("DEPRECATION")
                    am.abandonAudioFocus(null)
                }
            }
        } catch (e: Exception) {
            Log.e("CourseFragment", "Error stopping music", e)
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