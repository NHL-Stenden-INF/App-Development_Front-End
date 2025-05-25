package com.nhlstenden.appdev.courses.ui

import android.media.MediaPlayer
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.GestureDetector
import android.view.MotionEvent
import androidx.core.view.GestureDetectorCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.nhlstenden.appdev.R
import com.nhlstenden.appdev.courses.domain.models.Course
import com.nhlstenden.appdev.courses.parser.CourseParser
import com.nhlstenden.appdev.courses.ui.adapters.TopicAdapter
import com.nhlstenden.appdev.courses.ui.viewmodels.CourseTopicsViewModel
import com.nhlstenden.appdev.databinding.FragmentCourseTopicsBinding
import com.nhlstenden.appdev.task.ui.screens.TaskActivity
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class CourseTopicsFragment : Fragment() {
    private var _binding: FragmentCourseTopicsBinding? = null
    private val binding get() = _binding!!

    private val viewModel: CourseTopicsViewModel by viewModels()
    
    private lateinit var topicAdapter: TopicAdapter
    private var mediaPlayer: MediaPlayer? = null
    private var courseName: String = ""
    private var courseData: Course? = null
    private lateinit var gestureDetector: GestureDetectorCompat

    private inner class SwipeGestureListener : GestureDetector.SimpleOnGestureListener() {
        override fun onFling(
            e1: MotionEvent,
            e2: MotionEvent,
            velocityX: Float,
            velocityY: Float
        ): Boolean {
            val diffX = e2.x - e1.x
            val diffY = e2.y - e1.y

            if (Math.abs(diffX) > Math.abs(diffY)) {
                if (Math.abs(diffX) > 100 && Math.abs(velocityX) > 100) {
                    if (diffX > 0) {
                        findNavController().navigateUp()
                        return true
                    }
                }
            }
            return false
        }

        override fun onDown(e: MotionEvent): Boolean {
            return true
        }
    }

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
        setupViews()
        setupRecyclerView()
        setupSwipeRefresh()
        observeViewModel()
        setupCourseInfo()

        gestureDetector = GestureDetectorCompat(requireContext(), SwipeGestureListener())

        view.setOnTouchListener { _, event ->
            gestureDetector.onTouchEvent(event)
        }
    }

    private fun setupViews() {
        binding.backButton.setOnClickListener {
            findNavController().navigateUp()
        }
    }

    private fun setupRecyclerView() {
        topicAdapter = TopicAdapter { topic ->
            val intent = TaskActivity.newIntent(requireContext(), topic.id)
            startActivity(intent)
        }
        binding.topicsList.apply {
            layoutManager = LinearLayoutManager(context)
            adapter = topicAdapter
        }
    }

    private fun setupSwipeRefresh() {
        binding.swipeRefreshLayout.setOnRefreshListener {
            viewModel.loadTopics()
        }
    }

    private fun observeViewModel() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.topicsState.collectLatest { state ->
                when (state) {
                    is CourseTopicsViewModel.TopicsState.Loading -> {
                        binding.loadingProgressBar.visibility = View.VISIBLE
                        binding.topicsList.visibility = View.GONE
                        binding.errorTextView.visibility = View.GONE
                        binding.swipeRefreshLayout.isRefreshing = false
                    }
                    is CourseTopicsViewModel.TopicsState.Success -> {
                        binding.loadingProgressBar.visibility = View.GONE
                        binding.topicsList.visibility = View.VISIBLE
                        binding.errorTextView.visibility = View.GONE
                        topicAdapter.submitList(state.topics)
                        binding.swipeRefreshLayout.isRefreshing = false
                    }
                    is CourseTopicsViewModel.TopicsState.Error -> {
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

    private fun setupCourseInfo() {
        courseName = arguments?.getString(ARG_COURSE_NAME) ?: ""
        val courseParser = CourseParser(requireContext())
        courseData = courseParser.loadCourseByTitle(courseName)
        binding.courseTitle.text = courseData?.title ?: courseName
        binding.courseDescription.text = courseData?.description ?: ""
        viewModel.loadTopics()
    }

    override fun onPause() {
        super.onPause()
        mediaPlayer?.pause()
    }

    override fun onStop() {
        super.onStop()
        mediaPlayer?.stop()
        mediaPlayer?.release()
        mediaPlayer = null
    }

    override fun onDestroy() {
        super.onDestroy()
        mediaPlayer?.release()
        mediaPlayer = null
    }

    data class Topic(
        val title: String,
        val difficulty: String,
        val description: String,
        val progress: Int
    )

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    companion object {
        private const val ARG_COURSE_NAME = "course_name"

        fun newInstance(courseName: String): CourseTopicsFragment {
            return CourseTopicsFragment().apply {
                arguments = Bundle().apply {
                    putString(ARG_COURSE_NAME, courseName)
                }
            }
        }
    }
}