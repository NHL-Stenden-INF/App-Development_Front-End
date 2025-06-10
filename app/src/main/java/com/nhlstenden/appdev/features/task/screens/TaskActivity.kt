package com.nhlstenden.appdev.features.task.screens

import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.LinearLayout
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.activity.viewModels
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.lifecycleScope
import androidx.viewpager2.widget.ViewPager2
import com.google.android.material.button.MaterialButton
import com.nhlstenden.appdev.R
import com.nhlstenden.appdev.core.repositories.AuthRepository
import com.nhlstenden.appdev.core.repositories.UserRepository
import com.nhlstenden.appdev.databinding.ActivityTaskBinding
import com.nhlstenden.appdev.features.courses.repositories.CourseRepositoryImpl
import com.nhlstenden.appdev.features.task.TaskCompleteListener
import com.nhlstenden.appdev.features.task.TaskFailureDialogFragment
import com.nhlstenden.appdev.features.task.adapters.TaskPagerAdapter
import com.nhlstenden.appdev.features.task.fragments.BaseTaskFragment
import com.nhlstenden.appdev.features.task.models.Question
import com.nhlstenden.appdev.features.task.viewmodels.TaskViewModel
import com.nhlstenden.appdev.features.home.repositories.StreakRepository
import com.nhlstenden.appdev.features.home.StreakManager
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.time.LocalDate
import org.json.JSONArray
import kotlinx.coroutines.withContext
import kotlinx.coroutines.CoroutineScope
import org.json.JSONObject

@AndroidEntryPoint
class TaskActivity : AppCompatActivity() {
    
    @Inject lateinit var authRepository: AuthRepository
    @Inject lateinit var userRepository: UserRepository
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
    lateinit var courseRepository: CourseRepositoryImpl

    private val streakManager = StreakManager()

    private lateinit var questionTextView: TextView
    private lateinit var optionsContainer: LinearLayout
    private lateinit var progressBar: ProgressBar
    private lateinit var progressText: TextView
    
    private var taskId: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityTaskBinding.inflate(layoutInflater)
        setContentView(binding.root)

        taskId = intent.getStringExtra(EXTRA_TASK_ID)
        if (taskId == null) {
            Toast.makeText(this, "Task ID not found", Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        initializeViews()
        setupViewPager()
        setupClickListeners()
        observeTaskState()
        startTask()
    }

    private fun initializeViews() {
        // Initialize UI components
        questionTextView = TextView(this)
        optionsContainer = LinearLayout(this)
        progressBar = binding.progressBar
        progressText = binding.taskProgress
    }

    private fun startTask() {
        val currentUser = authRepository.getCurrentUserSync()
        if (currentUser != null) {
            CoroutineScope(Dispatchers.IO).launch {
                try {
                    val attributesResult = userRepository.getUserAttributes(currentUser.id)
                    if (attributesResult.isFailure) {
                        withContext(Dispatchers.Main) {
                            Toast.makeText(this@TaskActivity, "Failed to get user data. Please try again.", Toast.LENGTH_SHORT).show()
                            finish()
                        }
                        return@launch
                    }

                    val profile = attributesResult.getOrThrow()
                    val currentBellPeppers = profile.optInt("bell_peppers", 0)
                    
                    Log.d("TaskActivity", "Starting task - Current bell peppers: $currentBellPeppers")
                    
                    if (currentBellPeppers <= 0) {
                        withContext(Dispatchers.Main) {
                            Toast.makeText(this@TaskActivity, "You need a bell pepper to start this task", Toast.LENGTH_SHORT).show()
                            finish()
                        }
                        return@launch
                    }

                    // Reduce bell peppers when starting task
                    val newBellPeppers = currentBellPeppers - 1
                    Log.d("TaskActivity", "Consuming bell pepper - New count: $newBellPeppers")
                    
                    val updateResult = userRepository.updateUserBellPeppers(currentUser.id, newBellPeppers)
                    Log.d("TaskActivity", "Bell pepper update result: ${updateResult.isSuccess}")
                    
                    if (updateResult.isFailure) {
                        withContext(Dispatchers.Main) {
                            Toast.makeText(this@TaskActivity, "Failed to start task. Please try again.", Toast.LENGTH_SHORT).show()
                            finish()
                        }
                        return@launch
                    }

                    withContext(Dispatchers.Main) {
                        this@TaskActivity.loadTasks()
                    }
                } catch (e: Exception) {
                    Log.e("TaskActivity", "Error starting task", e)
                    withContext(Dispatchers.Main) {
                        Toast.makeText(this@TaskActivity, "Error starting task. Please try again.", Toast.LENGTH_SHORT).show()
                        finish()
                    }
                }
            }
        }
    }

    private fun loadTasks() {
        val courseId = taskId?.substringBefore("_") ?: return
        binding.taskName.text = getTaskTitle(this, courseId, taskId ?: "")
        viewModel.loadTasks(taskId ?: "")
    }

    private fun showCurrentQuestion() {
        if (currentIndex < currentQuestions.size) {
            val question = currentQuestions[currentIndex]
            questionTextView.text = question.text
            
            // Clear previous options
            optionsContainer.removeAllViews()
            
            // Add new options
            question.options.forEach { option ->
                val optionButton = MaterialButton(this).apply {
                    text = option.text
                    setOnClickListener {
                        checkAnswer(option.isCorrect)
                    }
                    layoutParams = LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.MATCH_PARENT,
                        LinearLayout.LayoutParams.WRAP_CONTENT
                    ).apply {
                        setMargins(0, 8, 0, 8)
                    }
                    stateListAnimator = android.animation.AnimatorInflater.loadStateListAnimator(
                        context, R.animator.button_state_animator
                    )
                }
                optionsContainer.addView(optionButton)
            }
            
            // Update progress
            progressBar.max = currentQuestions.size
            progressBar.progress = currentIndex + 1
            progressText.text = "${currentIndex + 1}/${currentQuestions.size}"
        } else {
            // All questions were correct, task is completed
            onTaskCompleted()
        }
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
                        onTaskCompleted()
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
            Log.d("TaskActivity", "User exited task early - bell pepper was consumed and not returned")
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
                    val currentUser = authRepository.getCurrentUserSync()
                    val taskId = intent.getStringExtra(EXTRA_TASK_ID)
                    Log.d("TaskActivity", "Calling updateTaskProgress for userId=${currentUser?.id}, taskId=$taskId")
                    if (currentUser != null && taskId != null) {
                        val courseId = taskId.substringBefore("_")
                        lifecycleScope.launch(Dispatchers.IO) {
                            try {
                                // Update progress using repository
                                val progressUpdated = courseRepository.updateTaskProgress(
                                    currentUser.id.toString(),
                                    taskId,
                                    1 // Increment by 1
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
        val currentUser = authRepository.getCurrentUserSync()
        if (currentUser != null) {
            lifecycleScope.launch(Dispatchers.IO) {
                try {
                    // Get current points and XP
                    val attributesResult = userRepository.getUserAttributes(currentUser.id)
                    if (attributesResult.isSuccess) {
                        val userData = attributesResult.getOrThrow()
                        val currentPoints = userData.optInt("points", 0)
                        val currentXp = userData.optInt("xp", 0)
                        
                        // Update points and XP (XP is 1:1 with points)
                        val newPoints = currentPoints + pointsEarned
                        val newXp = currentXp + pointsEarned
                        
                        userRepository.updateUserPoints(currentUser.id, newPoints)
                        userRepository.updateUserXp(currentUser.id, newXp.toLong())
                    }
                } catch (e: Exception) {
                    Log.e("TaskActivity", "Error updating points and XP: ${e.message}")
                }
            }
        }
    }

    private fun showTaskFailedDialog() {
        val currentUser = authRepository.getCurrentUserSync()
        if (currentUser != null) {
            CoroutineScope(Dispatchers.IO).launch {
                try {
                    val attributesResult = userRepository.getUserAttributes(currentUser.id)
                    if (attributesResult.isSuccess) {
                        val profile = attributesResult.getOrThrow()
                        val currentBellPeppers = profile.optInt("bell_peppers", 0)
                        
                        Log.d("TaskActivity", "Task failed - Current bell peppers: $currentBellPeppers (bell pepper was consumed and not returned)")
                        
                        withContext(Dispatchers.Main) {
                            TaskFailureDialogFragment().show(supportFragmentManager, "task_failed")
                        }
                    } else {
                        withContext(Dispatchers.Main) {
                            Toast.makeText(this@TaskActivity, "Error checking bell peppers. Please try again.", Toast.LENGTH_SHORT).show()
                        }
                    }
                } catch (e: Exception) {
                    withContext(Dispatchers.Main) {
                        Toast.makeText(this@TaskActivity, "Error checking bell peppers. Please try again.", Toast.LENGTH_SHORT).show()
                    }
                }
            }
        }
    }

    private fun onTaskCompleted() {
        val currentUser = authRepository.getCurrentUserSync()
        if (currentUser != null) {
            CoroutineScope(Dispatchers.IO).launch {
                try {
                    val attributesResult = userRepository.getUserAttributes(currentUser.id)
                    if (attributesResult.isSuccess) {
                        val profile = attributesResult.getOrThrow()
                        val currentBellPeppers = profile.optInt("bell_peppers", 0)
                        
                        Log.d("TaskActivity", "Task completed - Current bell peppers: $currentBellPeppers")
                        
                        // Return the bell pepper on successful completion
                        val newBellPeppers = currentBellPeppers + 1
                        Log.d("TaskActivity", "Returning bell pepper - New count: $newBellPeppers")
                        
                        val updateResult = userRepository.updateUserBellPeppers(currentUser.id, newBellPeppers)
                        Log.d("TaskActivity", "Bell pepper return result: ${updateResult.isSuccess}")
                        
                        if (updateResult.isFailure) {
                            Log.e("TaskActivity", "Failed to return bell pepper")
                        }
                    }

                    withContext(Dispatchers.Main) {
                        showTaskCompletedDialog()
                    }
                } catch (e: Exception) {
                    Log.e("TaskActivity", "Error returning bell pepper", e)
                    withContext(Dispatchers.Main) {
                        showTaskCompletedDialog()
                    }
                }
            }
        }
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
        // First check and consume a bell pepper for the retry
        val currentUser = authRepository.getCurrentUserSync()
        if (currentUser != null) {
            CoroutineScope(Dispatchers.IO).launch {
                try {
                    val attributesResult = userRepository.getUserAttributes(currentUser.id)
                    if (attributesResult.isFailure) {
                        withContext(Dispatchers.Main) {
                            Toast.makeText(this@TaskActivity, "Failed to get user data. Please try again.", Toast.LENGTH_SHORT).show()
                            finish()
                        }
                        return@launch
                    }

                    val profile = attributesResult.getOrThrow()
                    val currentBellPeppers = profile.optInt("bell_peppers", 0)
                    
                    Log.d("TaskActivity", "Retry attempt - Current bell peppers: $currentBellPeppers")
                    
                    if (currentBellPeppers <= 0) {
                        withContext(Dispatchers.Main) {
                            Toast.makeText(this@TaskActivity, "You need a bell pepper to retry this task", Toast.LENGTH_SHORT).show()
                            finish()
                        }
                        return@launch
                    }

                    // Consume bell pepper for retry
                    val newBellPeppers = currentBellPeppers - 1
                    Log.d("TaskActivity", "Consuming bell pepper for retry - New count: $newBellPeppers")
                    
                    val updateResult = userRepository.updateUserBellPeppers(currentUser.id, newBellPeppers)
                    Log.d("TaskActivity", "Bell pepper retry consumption result: ${updateResult.isSuccess}")
                    
                    if (updateResult.isFailure) {
                        withContext(Dispatchers.Main) {
                            Toast.makeText(this@TaskActivity, "Failed to start retry. Please try again.", Toast.LENGTH_SHORT).show()
                            finish()
                        }
                        return@launch
                    }

                    withContext(Dispatchers.Main) {
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
                } catch (e: Exception) {
                    Log.e("TaskActivity", "Error during retry", e)
                    withContext(Dispatchers.Main) {
                        Toast.makeText(this@TaskActivity, "Error starting retry. Please try again.", Toast.LENGTH_SHORT).show()
                        finish()
                    }
                }
            }
        }
    }

    private fun getTaskTitle(context: Context, courseId: String, taskId: String): String {
        val taskParser = com.nhlstenden.appdev.features.courses.TaskParser(context)
        val task = taskParser.loadAllCoursesOfTask(courseId).find { it.id == taskId }
        return task?.title ?: "Task"
    }

    private fun showTaskCompletedDialog() {
        // Implementation of showTaskCompletedDialog method
    }

    private fun checkAnswer(isCorrect: Boolean) {
        // TODO: Implement answer checking logic
        // For now, just move to the next question
        onNextQuestion()
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