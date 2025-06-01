package com.nhlstenden.appdev.task.ui.screens

import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.Toast
import androidx.activity.viewModels
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.viewpager2.widget.ViewPager2
import com.nhlstenden.appdev.databinding.ActivityTaskBinding
import com.nhlstenden.appdev.home.data.repositories.StreakRepository
import com.nhlstenden.appdev.home.manager.StreakManager
import com.nhlstenden.appdev.shared.components.UserManager
import com.nhlstenden.appdev.task.domain.models.Question
import com.nhlstenden.appdev.task.ui.adapters.TaskPagerAdapter
import com.nhlstenden.appdev.task.ui.viewmodels.TaskViewModel
import com.nhlstenden.appdev.task.listener.TaskCompleteListener
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import kotlinx.coroutines.Dispatchers
import java.time.LocalDate
import javax.inject.Inject

@AndroidEntryPoint
class TaskActivity : AppCompatActivity() {
    private lateinit var binding: ActivityTaskBinding
    private val viewModel: TaskViewModel by viewModels()
    private lateinit var taskPagerAdapter: TaskPagerAdapter
    private var allQuestions: List<Question> = emptyList()
    private var wrongQuestionIds: MutableSet<String> = mutableSetOf()
    private var roundWrongIds: MutableSet<String> = mutableSetOf()
    private var currentQuestions: MutableList<Question> = mutableListOf()
    private var currentIndex = 0
    private var remainingQuestions: MutableList<Question> = mutableListOf()
    private var correctQuestionIds: MutableSet<String> = mutableSetOf()

    @Inject
    lateinit var streakRepository: StreakRepository
    private val streakManager = StreakManager()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityTaskBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val topicId = intent.getStringExtra(EXTRA_TOPIC_ID) ?: run {
            Toast.makeText(this, "Error: No topic ID provided", Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        setupViewPager()
        setupClickListeners()
        observeTaskState()
        viewModel.loadTasks(topicId)
    }

    private fun setupViewPager() {
        taskPagerAdapter = TaskPagerAdapter(this, object : TaskCompleteListener {
            override fun onTaskCompleted(question: Question) {
                // No-op here
            }

            @RequiresApi(Build.VERSION_CODES.O)
            override fun onTaskComplete(isCorrect: Boolean) {
                val question = currentQuestions[currentIndex]
                Log.d("TaskActivity", "Question answered. Correct: $isCorrect")
                
                if (isCorrect) {
                    correctQuestionIds.add(question.id)
                    Log.d("TaskActivity", "Question was correct, updating streak...")

                    lifecycleScope.launch(Dispatchers.IO) {
                        try {
                            val currentUser = UserManager.getCurrentUser()
                            if (currentUser != null) {
                                Log.d("TaskActivity", "Current user found: ${currentUser.id}")
                                val today = LocalDate.now()
                                Log.d("TaskActivity", "Updating streak for date: $today")
                                
                                // Get current streak from database
                                val currentStreak = streakRepository.getCurrentStreak(currentUser.id.toString(), currentUser.authToken)
                                
                                // Update streak with today's date
                                streakManager.updateStreak(today, currentStreak)
                                
                                // Update both last task date and streak in database
                                val streakUpdated = streakRepository.updateLastTaskDate(currentUser.id.toString(), today, currentUser.authToken)
                                val newStreak = streakManager.getCurrentStreak()
                                val streakResponse = streakRepository.updateStreak(currentUser.id.toString(), newStreak, currentUser.authToken)
                                
                                Log.d("TaskActivity", "Streak update result: $streakUpdated")
                                Log.d("TaskActivity", "New streak: $newStreak")
                            } else {
                                Log.e("TaskActivity", "No current user found")
                            }
                        } catch (e: Exception) {
                            Log.e("TaskActivity", "Error updating streak: ${e.message}")
                            Log.e("TaskActivity", "Stack trace: ${e.stackTraceToString()}")
                        }
                    }
                }
            }
        })
        binding.viewPager.adapter = taskPagerAdapter
    }

    private fun setupClickListeners() {
        binding.exitButton.setOnClickListener {
            finish()
        }
    }

    private fun observeTaskState() {
        viewModel.taskState.observe(this) { state ->
            when (state) {
                is TaskViewModel.TaskState.Loading -> {
                    binding.progressBar.visibility = View.VISIBLE
                    binding.viewPager.visibility = View.GONE
                }
                is TaskViewModel.TaskState.Success -> {
                    binding.progressBar.visibility = View.GONE
                    binding.viewPager.visibility = View.VISIBLE
                    allQuestions = state.questions
                    correctQuestionIds.clear()
                    currentQuestions = allQuestions.shuffled().toMutableList()
                    currentIndex = 0
                    updateQuestionNumber()
                    taskPagerAdapter.submitList(currentQuestions)
                    binding.viewPager.setCurrentItem(0, false)
                }
                is TaskViewModel.TaskState.Error -> {
                    binding.progressBar.visibility = View.GONE
                    binding.viewPager.visibility = View.GONE
                    Toast.makeText(this, state.message, Toast.LENGTH_SHORT).show()
                }
                is TaskViewModel.TaskState.Completed -> {
                    Toast.makeText(this, "Task completed!", Toast.LENGTH_SHORT).show()
                    finish()
                }
            }
        }
        binding.viewPager.registerOnPageChangeCallback(object : ViewPager2.OnPageChangeCallback() {
            override fun onPageSelected(position: Int) {
                currentIndex = position
                updateQuestionNumber()
            }
        })
    }

    private fun updateQuestionNumber() {
        binding.taskProgress.text = "${currentIndex + 1} of ${currentQuestions.size}"
    }

    fun onNextQuestion() {
        // Called from fragment after feedback
        if (currentIndex < currentQuestions.size - 1) {
            binding.viewPager.currentItem = currentIndex + 1
        } else {
            // End of round: always repeat all questions, shuffled
            currentQuestions = allQuestions.shuffled().toMutableList()
            currentIndex = 0
            updateQuestionNumber()
            taskPagerAdapter.submitList(currentQuestions)
            binding.viewPager.setCurrentItem(0, false)
        }
    }

    companion object {
        private const val EXTRA_TOPIC_ID = "extra_topic_id"

        fun newIntent(context: Context, topicId: String): Intent {
            return Intent(context, TaskActivity::class.java).apply {
                putExtra(EXTRA_TOPIC_ID, topicId)
            }
        }
    }
} 