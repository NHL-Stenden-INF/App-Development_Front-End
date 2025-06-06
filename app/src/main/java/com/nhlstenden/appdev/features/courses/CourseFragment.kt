package com.nhlstenden.appdev.features.courses

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
import android.util.Log
import androidx.recyclerview.widget.LinearLayoutManager
import com.nhlstenden.appdev.features.courses.repositories.CourseRepositoryImpl
import com.nhlstenden.appdev.features.profile.viewmodels.ProfileViewModel
import com.nhlstenden.appdev.main.MainActivity
import android.widget.ImageView
import com.daimajia.numberprogressbar.NumberProgressBar
import android.content.Intent
import android.app.Activity
import com.nhlstenden.appdev.core.utils.UserManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import android.media.AudioManager
import android.media.AudioAttributes
import android.os.Build
import android.content.Context
import android.media.AudioFocusRequest

@AndroidEntryPoint
class CourseFragment : Fragment() {
    private lateinit var binding: FragmentCourseBinding
    private val viewModel: CourseViewModel by viewModels()
    private val profileViewModel: ProfileViewModel by viewModels()
    private var taskAdapter: TaskAdapter? = null
    private var mediaPlayer: MediaPlayer? = null
    private val PREFS_NAME = "reward_settings"
    private val MUSIC_LOBBY_KEY = "music_lobby_enabled"
    private var audioManager: AudioManager? = null
    private var audioFocusRequest: AudioFocusRequest? = null

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
        val currentUser = UserManager.getCurrentUser()
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
                taskAdapter = TaskAdapter(progress) { task ->
                    val intent = TaskActivity.newIntent(requireContext(), task.id)
                    startActivityForResult(intent, TASK_COMPLETION_REQUEST_CODE)
                }
                binding.tasksList.adapter = taskAdapter
                // Re-submit the current list if available
                val currentTasks = (viewModel.tasksState.value as? CourseViewModel.TasksState.Success)?.tasks
                if (currentTasks != null) {
                    taskAdapter?.submitList(currentTasks)
                }
            }
        }

        // Set up back button
        binding.backButton.setOnClickListener {
            (requireActivity() as? MainActivity)?.navigateToTab("courses")
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == TASK_COMPLETION_REQUEST_CODE && resultCode == Activity.RESULT_OK) {
            // Refresh the task list
            val courseId = arguments?.getString("COURSE_ID") ?: return
            val currentUser = UserManager.getCurrentUser()
            if (currentUser != null) {
                viewModel.loadTasks(courseId, currentUser)
            }
        }
    }

    override fun onResume() {
        super.onResume()
        val courseId = arguments?.getString("COURSE_ID") ?: return
        val currentUser = UserManager.getCurrentUser()
        if (currentUser != null) {
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
    }

    private fun setupMusic() {
        val sharedPrefs = requireContext().getSharedPreferences(PREFS_NAME, android.content.Context.MODE_PRIVATE)
        val isMusicLobbyEnabled = sharedPrefs.getBoolean(MUSIC_LOBBY_KEY, false)
        Log.d("CourseFragment", "Music lobby enabled: $isMusicLobbyEnabled")
        
        if (isMusicLobbyEnabled) {
            try {
                val courseId = arguments?.getString("COURSE_ID") ?: return
                Log.d("CourseFragment", "Setting up music for course: $courseId")
                val musicResId = when (courseId) {
                    "html" -> R.raw.html_themesong
                    "css" -> R.raw.css_themesong
                    "sql" -> R.raw.sql_themesong
                    else -> R.raw.default_themesong
                }
                Log.d("CourseFragment", "Using music resource ID: $musicResId")

                // Request audio focus
                val result = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    Log.d("CourseFragment", "Requesting audio focus for Android O and above")
                    audioManager?.requestAudioFocus(audioFocusRequest!!)
                } else {
                    Log.d("CourseFragment", "Requesting audio focus for older Android versions")
                    @Suppress("DEPRECATION")
                    audioManager?.requestAudioFocus({ focusChange ->
                        Log.d("CourseFragment", "Audio focus changed: $focusChange")
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
                    }, AudioManager.STREAM_MUSIC, AudioManager.AUDIOFOCUS_GAIN)
                }
                Log.d("CourseFragment", "Audio focus request result: $result")

                if (result == AudioManager.AUDIOFOCUS_REQUEST_GRANTED) {
                    Log.d("CourseFragment", "Audio focus granted, creating MediaPlayer")
                    mediaPlayer = MediaPlayer.create(context, musicResId)
                    if (mediaPlayer == null) {
                        Log.e("CourseFragment", "Failed to create MediaPlayer")
                        return
                    }
                    Log.d("CourseFragment", "MediaPlayer created successfully")
                    mediaPlayer?.isLooping = true
                    mediaPlayer?.setOnPreparedListener {
                        Log.d("CourseFragment", "MediaPlayer prepared, starting playback")
                        it.start()
                    }
                    mediaPlayer?.setOnErrorListener { mp, what, extra ->
                        Log.e("CourseFragment", "MediaPlayer error: what=$what, extra=$extra")
                        true
                    }
                    Log.d("CourseFragment", "Music playback started")
                } else {
                    Log.e("CourseFragment", "Failed to get audio focus")
                }
            } catch (e: Exception) {
                Log.e("CourseFragment", "Error playing music", e)
            }
        } else {
            Log.d("CourseFragment", "Music lobby is not enabled in settings")
        }
    }

    private fun stopMusic() {
        Log.d("CourseFragment", "Stopping music")
        mediaPlayer?.apply {
            if (isPlaying) {
                stop()
                Log.d("CourseFragment", "Music stopped")
            }
            release()
            Log.d("CourseFragment", "MediaPlayer released")
        }
        mediaPlayer = null

        // Abandon audio focus
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            audioFocusRequest?.let { request ->
                audioManager?.abandonAudioFocusRequest(request)
                Log.d("CourseFragment", "Audio focus abandoned for Android O and above")
            }
        } else {
            @Suppress("DEPRECATION")
            audioManager?.abandonAudioFocus(null)
            Log.d("CourseFragment", "Audio focus abandoned for older Android versions")
        }
    }

    private fun setupRecyclerView() {
        val courseProgress = arguments?.getInt("COURSE_PROGRESS") ?: 0
        taskAdapter = TaskAdapter(courseProgress) { task ->
            val intent = TaskActivity.newIntent(requireContext(), task.id)
            startActivityForResult(intent, TASK_COMPLETION_REQUEST_CODE)
        }
        binding.tasksList.adapter = taskAdapter
    }

    private fun setupSwipeRefresh() {
        binding.swipeRefreshLayout.setOnRefreshListener {
            val courseId = arguments?.getString("COURSE_ID") ?: return@setOnRefreshListener
            val currentUser = UserManager.getCurrentUser()
            if (currentUser != null) {
                viewModel.loadTasks(courseId, currentUser)
            }
        }
    }

    private fun setupBackButton() {
        binding.backButton.setOnClickListener {
            (requireActivity() as? MainActivity)?.navigateToTab("courses")
        }
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
                        taskAdapter?.submitList(state.tasks)
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

    private class TaskAdapter(
        private val courseProgress: Int,
        private val onClick: (Task) -> Unit
    ) : ListAdapter<Task, TaskAdapter.TaskViewHolder>(DiffCallback) {
        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): TaskViewHolder {
            val view = LayoutInflater.from(parent.context)
                .inflate(R.layout.item_course, parent, false)
            return TaskViewHolder(view, onClick, courseProgress)
        }

        override fun onBindViewHolder(holder: TaskViewHolder, position: Int) {
            holder.bind(getItem(position))
        }

        class TaskViewHolder(
            itemView: View,
            private val onClick: (Task) -> Unit,
            private val courseProgress: Int
        ) : RecyclerView.ViewHolder(itemView) {
            private val titleText: TextView = itemView.findViewById(R.id.courseTitle)
            private val descriptionText: TextView = itemView.findViewById(R.id.courseDescription)
            private val difficultyText: TextView = itemView.findViewById(R.id.difficultyLevel)
            private val progressBar: NumberProgressBar = itemView.findViewById(R.id.progressBar)
            private val lockIcon: ImageView = itemView.findViewById(R.id.lockIcon)

            fun bind(task: Task) {
                titleText.text = task.title
                descriptionText.text = task.description
                difficultyText.text = task.difficulty
                progressBar.visibility = View.GONE

                // Handle task state based on course progress
                when {
                    // If task index is less than progress, it's completed
                    task.index < courseProgress -> {
                        itemView.isEnabled = false
                        itemView.alpha = 0.5f
                        lockIcon.visibility = View.GONE
                        itemView.setOnClickListener(null)
                    }
                    // If task index equals progress, it's the current task
                    task.index == courseProgress -> {
                        itemView.isEnabled = true
                        itemView.alpha = 1.0f
                        lockIcon.visibility = View.GONE
                        itemView.setOnClickListener { onClick(task) }
                    }
                    // If task index is greater than progress, it's locked
                    else -> {
                        itemView.isEnabled = false
                        itemView.alpha = 0.3f
                        lockIcon.visibility = View.VISIBLE
                        itemView.setOnClickListener(null)
                        // Make sure the lock icon is visible and properly positioned
                        lockIcon.bringToFront()
                    }
                }
            }
        }

        companion object {
            val DiffCallback = object : DiffUtil.ItemCallback<Task>() {
                override fun areItemsTheSame(oldItem: Task, newItem: Task): Boolean {
                    return oldItem.id == newItem.id
                }

                override fun areContentsTheSame(oldItem: Task, newItem: Task): Boolean {
                    return oldItem == newItem
                }
            }
        }
    }

    companion object {
        private const val TASK_COMPLETION_REQUEST_CODE = 1001
    }
}