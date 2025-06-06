package com.nhlstenden.appdev.features.task.screens

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
import com.nhlstenden.appdev.core.utils.UserManager
import com.nhlstenden.appdev.databinding.ActivityTaskBinding
import com.nhlstenden.appdev.features.courses.repositories.CourseRepositoryImpl
import com.nhlstenden.appdev.features.task.TaskCompleteListener
import com.nhlstenden.appdev.features.task.TaskFailureDialogFragment
import com.nhlstenden.appdev.features.task.adapters.TaskPagerAdapter
import com.nhlstenden.appdev.features.task.fragments.BaseTaskFragment
import com.nhlstenden.appdev.features.task.models.Question
import com.nhlstenden.appdev.features.task.viewmodels.TaskViewModel
import com.nhlstenden.appdev.main.MainActivity
import com.nhlstenden.appdev.features.home.repositories.StreakRepository
import com.nhlstenden.appdev.features.home.StreakManager
import com.nhlstenden.appdev.supabase.SupabaseClient
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.time.LocalDate
import javax.inject.Inject
import org.json.JSONArray

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
    private var attemptedQuestions: MutableSet<String> = mutableSetOf()

    @Inject
    lateinit var streakRepository: StreakRepository

    @Inject
    lateinit var supabaseClient: SupabaseClient

    @Inject
    lateinit var courseRepository: CourseRepositoryImpl

    private val streakManager = StreakManager()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityTaskBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val taskId = intent.getStringExtra(EXTRA_TASK_ID) ?: run {
            Toast.makeText(this, "Error: No task ID provided", Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        val courseId = taskId.substringBefore("_")
        binding.taskName.text = getTaskTitle(this, courseId, taskId)

        setupViewPager()
        setupClickListeners()
        observeTaskState()
        viewModel.loadTasks(taskId)
    }

    private fun setupViewPager() {
        taskPagerAdapter = TaskPagerAdapter(this, object : TaskCompleteListener {
            override fun onTaskCompleted(question: Question) {
                // No-op here
            }

            @RequiresApi(Build.VERSION_CODES.O)
            override fun onTaskComplete(isCorrect: Boolean) {
                val question = currentQuestions[currentIndex]
                Log.d("TaskActivity", "Question answered. Correct: $isCorrect, Current index: $currentIndex, Total questions: ${currentQuestions.size}")
                if (isCorrect) {
                    correctQuestionIds.add(question.id)
                } else {
                    wrongQuestionIds.add(question.id)
                    roundWrongIds.add(question.id)
                }
                // Move to next question
                if (currentIndex < currentQuestions.size - 1) {
                    currentIndex++
                    binding.viewPager.setCurrentItem(currentIndex, true)
                } else {
                    // We're at the last question
                    if (roundWrongIds.isNotEmpty()) {
                        // Show failure dialog
                        showTaskFailedDialog()
                    } else {
                        // All questions were correct, task is completed
                        viewModel.completeTask()
                    }
                }
            }
        })
        binding.viewPager.adapter = taskPagerAdapter
        
        // Disable swipe between questions
        binding.viewPager.isUserInputEnabled = false
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
                    wrongQuestionIds.clear()
                    roundWrongIds.clear()
                    currentQuestions = allQuestions.shuffled().toMutableList()
                    currentIndex = 0
                    Log.d("TaskActivity", "Questions loaded. Total questions: ${currentQuestions.size}")
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
                    // Calculate points based on correct answers
                    val pointsEarned = calculatePoints()
                    updateUserPoints(pointsEarned)
                    // Update course progress
                    val currentUser = UserManager.getCurrentUser()
                    val taskId = intent.getStringExtra(EXTRA_TASK_ID)
                    Log.d("TaskActivity", "Calling updateTaskProgress for userId=${currentUser?.id}, taskId=$taskId")
                    if (currentUser != null && taskId != null) {
                        val courseId = taskId.substringBefore("_")
                        lifecycleScope.launch(Dispatchers.IO) {
                            try {
                                // Get current progress
                                val userProgresses = supabaseClient.getUserProgress(currentUser.id.toString(), currentUser.authToken)
                                var currentProgress = 0
                                
                                // Find the current progress for this course
                                for(i in 0 until userProgresses.length()) {
                                    val jsonObject = userProgresses.getJSONObject(i)
                                    if (jsonObject.getString("course_id") == courseId) {
                                        currentProgress = jsonObject.getInt("progress")
                                        break
                                    }
                                }
                                
                                // Update progress with the new value
                                val progressUpdated = courseRepository.updateTaskProgress(
                                    currentUser.id.toString(),
                                    taskId,
                                    currentProgress + 1
                                )
                                
                                if (!progressUpdated) {
                                    Log.e("TaskActivity", "Failed to update task progress")
                                }
                            } catch (e: Exception) {
                                Log.e("TaskActivity", "Error updating task progress: ${e.message}")
                            }
                        }
                    }
                    Toast.makeText(this, "Task completed! You earned $pointsEarned points!", Toast.LENGTH_SHORT).show()
                    setResult(RESULT_OK)
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

    private fun calculatePoints(): Int {
        // Base points for completing the task
        var points = 50
        
        // Additional points for each correct answer
        points += correctQuestionIds.size * 10
        
        // Bonus points if all questions were answered correctly
        if (correctQuestionIds.size == allQuestions.size) {
            points += 50 // Bonus for perfect score
        }
        
        return points
    }

    private fun updateUserPoints(pointsEarned: Int) {
        val currentUser = UserManager.getCurrentUser()
        if (currentUser != null) {
            lifecycleScope.launch(Dispatchers.IO) {
                try {
                    // Get current points and XP
                    val response = supabaseClient.getUserAttributes(currentUser.id, currentUser.authToken)
                    if (response.isSuccessful) {
                        val responseBody = response.body?.string()
                        if (!responseBody.isNullOrEmpty()) {
                            val userData = JSONArray(responseBody).getJSONObject(0)
                            val currentPoints = userData.optInt("points", 0)
                            val currentXp = userData.optInt("xp", 0)
                            
                            // Update points and XP (XP is 1:1 with points)
                            val newPoints = currentPoints + pointsEarned
                            val newXp = currentXp + pointsEarned
                            supabaseClient.updateUserPoints(currentUser.id, newPoints, currentUser.authToken)
                            supabaseClient.updateUserXp(currentUser.id, newXp, currentUser.authToken)
                        }
                    }
                } catch (e: Exception) {
                    Log.e("TaskActivity", "Error updating points and XP: ${e.message}")
                }
            }
        }
    }

    private fun showTaskFailedDialog() {
        Log.d("TaskActivity", "Showing failure dialog. Current index: $currentIndex, Total questions: ${currentQuestions.size}")
        TaskFailureDialogFragment().show(supportFragmentManager, "task_failure_dialog")
    }

    fun onNextQuestion() {
        Log.d("TaskActivity", "onNextQuestion called. Current index: $currentIndex, Total questions: ${currentQuestions.size}")
        // Move to next question
        if (currentIndex < currentQuestions.size - 1) {
            currentIndex++
            binding.viewPager.setCurrentItem(currentIndex, true)
        } else {
            // We're at the last question
            if (roundWrongIds.isNotEmpty()) {
                // Show failure dialog
                showTaskFailedDialog()
            } else {
                // All questions were correct, task is completed
                viewModel.completeTask()
            }
        }
    }

    fun resetForWrongQuestions() {
        // Only keep questions that were wrong in the last round
        currentQuestions = allQuestions.filter { it.id in roundWrongIds }.toMutableList()
        currentIndex = 0
        // Reset wrong question tracking for the next round
        wrongQuestionIds.clear()
        roundWrongIds.clear()
        // Force ViewPager to recreate all fragments
        taskPagerAdapter.submitList(emptyList())
        binding.viewPager.post {
            taskPagerAdapter.submitList(currentQuestions)
            binding.viewPager.setCurrentItem(0, false)
            updateQuestionNumber()
        }
    }

    private fun getTaskTitle(context: Context, courseId: String, taskId: String): String {
        val taskParser = com.nhlstenden.appdev.features.courses.TaskParser(context)
        val task = taskParser.loadAllCoursesOfTask(courseId).find { it.id == taskId }
        return task?.title ?: "Task"
    }

    companion object {
        private const val EXTRA_TASK_ID = "extra_task_id"

        fun newIntent(context: Context, taskId: String): Intent {
            return Intent(context, TaskActivity::class.java).apply {
                putExtra(EXTRA_TASK_ID, taskId)
            }
        }
    }
} 