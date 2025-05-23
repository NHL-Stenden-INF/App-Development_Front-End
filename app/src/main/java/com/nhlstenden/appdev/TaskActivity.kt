package com.nhlstenden.appdev

import android.app.Dialog
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.Button
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import androidx.viewpager2.adapter.FragmentStateAdapter
import androidx.viewpager2.widget.ViewPager2
import com.google.android.material.button.MaterialButton
import com.nhlstenden.appdev.CourseTopicsFragment.Topic
import com.nhlstenden.appdev.models.QuestionParser
import com.nhlstenden.appdev.models.UserManager
import java.io.Serializable
import org.json.JSONArray

class TaskActivity : AppCompatActivity(), OnTaskCompleteListener {
    private lateinit var taskName: TextView
    private lateinit var taskProgress: TextView
    private lateinit var exitButton: MaterialButton
    private var questions: List<Question> = listOf()
    private var failedQuestions: MutableList<Question> = mutableListOf()
    
    // Track correctly answered questions and points
    private var correctAnswersCount: Int = 0
    private val POINTS_PER_CORRECT_ANSWER = 10
    private val COMPLETION_BONUS = 20

    private lateinit var viewPager: ViewPager2
    private lateinit var taskPagerAdapter: TaskPagerAdapter

    private lateinit var topicData: Topic
    private var activeQuestion: Int = 0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContentView(R.layout.activity_task)

        taskName = findViewById(R.id.taskName)
        taskProgress = findViewById(R.id.taskProgress)
        viewPager = findViewById(R.id.questionViewPager)
        exitButton = findViewById(R.id.exitButton)

        topicData = (intent.getSerializableExtra("TOPIC_DATA") as? Topic)!!
        taskName.text = topicData.title
        
        // Load questions from XML based on topic
        loadQuestionsForTopic(topicData.title)
        
        // Configure ViewPager after questions are loaded
        setupViewPager()
        
        updateTaskProgress()

        exitButton.setOnClickListener {
            val dialog = EndTaskDialogFragment()
            dialog.show(supportFragmentManager, "popup")
        }

        // Reset correct answers count
        correctAnswersCount = 0
    }
    
    private fun loadQuestionsForTopic(topicTitle: String) {
        val questionParser = QuestionParser(this)
        questions = questionParser.loadQuestionsForTopic(topicTitle)
        
        // If no questions were loaded, use fallback hardcoded questions
        if (questions.isEmpty()) {
            Log.w("TaskActivity", "No questions found for topic $topicTitle, using fallback questions")
            questions = getFallbackQuestions()
        }
        
        Log.d("TaskActivity", "Loaded ${questions.size} questions for topic $topicTitle")
    }
    
    private fun getFallbackQuestions(): List<Question> {
        return listOf(
            Question.MultipleChoiceQuestion(
                "What does HTML stand for?",
                listOf(
                    Option("Hyper Text Markup Language", true),
                    Option("High Tech Modern Language", false),
                    Option("Hyperlinks Text Mode Language", false),
                    Option("Home Tool Markup Language", false)
                )
            ),
            Question.MultipleChoiceQuestion(
                "Which HTML tag is used to define an unordered list?",
                listOf(
                    Option("<ul>", true),
                    Option("<ol>", false),
                    Option("<li>", false),
                    Option("<list>", false)
                )
            )
        )
    }
    
    private fun setupViewPager() {
        taskPagerAdapter = TaskPagerAdapter(this)
        viewPager.adapter = taskPagerAdapter
        viewPager.offscreenPageLimit = 1
        viewPager.overScrollMode = View.OVER_SCROLL_NEVER
        viewPager.isUserInputEnabled = false

        viewPager.setPageTransformer { page, position ->
            val scale = if (position < 0) 1f + position * 0.1f else 1f - position * 0.1f
            page.scaleX = scale
            page.scaleY = scale
        }
    }

    override fun onTaskCompleted(question: Question, hasSucceeded: Boolean) {
        if (!hasSucceeded) {
            failedQuestions.add(question)
        } else {
            // Increment correct answers count
            correctAnswersCount++
        }
        activeQuestion++

        if (activeQuestion >= questions.size)
        {
            if (failedQuestions.isNotEmpty()) {
                activeQuestion = 0
                // Reshuffle options for each failed question
                questions = failedQuestions.map { q ->
                    when (q) {
                        is Question.MultipleChoiceQuestion -> {
                            val shuffledOptions = q.options.shuffled()
                            Question.MultipleChoiceQuestion(q.question, shuffledOptions)
                        }
                        else -> q
                    }
                }
                failedQuestions = mutableListOf()
                // Reset correct answers count for the new round
                correctAnswersCount = 0
                // Randomize id list to ensure new views
                questionIds = questions.map { fragmentIdSeed++ }
                taskPagerAdapter.notifyDataSetChanged()
                viewPager.setCurrentItem(0, false)
            }
            else {
                // Calculate points to award
                val pointsEarned = calculatePointsEarned()
                
                // Update user points in both local state and Supabase
                updateUserPoints(pointsEarned)

                val intent = Intent(this, TaskCompleteActivity::class.java)
                intent.putExtra("TOPIC_DATA", topicData)
                intent.putExtra("POINTS_EARNED", pointsEarned)
                
                // Get user data from UserManager singleton (always up-to-date)
                val userData = UserManager.getCurrentUser()
                intent.putExtra("USER_DATA", userData)
                startActivity(intent)
                finish()
                return
            }

        }

        viewPager.currentItem = activeQuestion
        updateTaskProgress()
    }
    
    private fun calculatePointsEarned(): Int {
        // Base points from correct answers
        val answerPoints = correctAnswersCount * POINTS_PER_CORRECT_ANSWER
        
        // Bonus for completing the task without incorrect answers
        val completionBonus = if (failedQuestions.isEmpty()) COMPLETION_BONUS else 0
        
        return answerPoints + completionBonus
    }
    
    private fun updateUserPoints(pointsToAdd: Int) {
        // Get current user
        val currentUser = UserManager.getCurrentUser() ?: return
        
        // Create updated user with new points
        val updatedPoints = currentUser.points + pointsToAdd
        val updatedUser = currentUser.copy(points = updatedPoints)
        
        // Update local user data
        UserManager.setCurrentUser(updatedUser)
        
        // Update points in Supabase
        Thread {
            try {
                val supabaseClient = SupabaseClient()
                val response = supabaseClient.updateUserPoints(
                    currentUser.id.toString(), 
                    updatedPoints,
                    currentUser.authToken
                )
                
                if (response.code != 200 && response.code != 204) {
                    Log.e("TaskActivity", "Failed to update points: ${response.code}")
                } else {
                    Log.d("TaskActivity", "Points updated successfully! New value: $updatedPoints")
                    // Refresh local user data from Supabase to ensure consistency
                    val refreshResponse = supabaseClient.getUserAttributes(currentUser.id.toString())
                    if (refreshResponse.code == 200) {
                        val userResponse = JSONArray(refreshResponse.body?.string())
                        val refreshedPoints = userResponse.getJSONObject(0).getInt("points")
                        val refreshedUser = currentUser.copy(points = refreshedPoints)
                        UserManager.setCurrentUser(refreshedUser)
                        Log.d("TaskActivity", "Local user data refreshed from Supabase. New points: $refreshedPoints")
                    }
                }
            } catch (e: Exception) {
                Log.e("TaskActivity", "Error updating points: ${e.message}")
            }
        }.start()
    }

    fun updateTaskProgress(){
        taskProgress.text = "${activeQuestion + 1} of ${questions.size}"
    }

    private var fragmentIdSeed = 0L
    private var questionIds: List<Long> = mutableListOf()
    
    private fun updateQuestionIds() {
        questionIds = questions.map { fragmentIdSeed++ }
    }

    private inner class TaskPagerAdapter(activity: FragmentActivity) : FragmentStateAdapter(activity) {
        init {
            updateQuestionIds()
        }
        
        override fun getItemCount(): Int = questions.size

        override fun createFragment(position: Int): Fragment {
            val question = questions[position]
            return when (question) {
                is Question.MultipleChoiceQuestion -> MultipleChoiceFragment.newInstance(question)
                // is Question.FlipCardQuestion -> FlipCardFragment().newInstance(question)
                // is Question.PressMistakesQuestion -> PressMistakesFragment().newInstance(question)
                // is Question.EditTextQuestion -> EditTextFragment().newInstance(question)
                else -> MultipleChoiceFragment()
            }
        }

        override fun getItemId(position: Int): Long {
            return questionIds[position]
        }

        override fun containsItem(itemId: Long): Boolean {
            return questionIds.contains(itemId)
        }
    }

    sealed class Question(): Serializable {
        data class MultipleChoiceQuestion(
            val question: String,
            val options: List<Option>,
        ) : Question(), Serializable

        data class FlipCardQuestion(
            val frontBackPair: Pair<String, String>
        ) : Question(), Serializable

        data class PressMistakesQuestion(
            val question: String,
            val sentence: String,
            val mistakes: List<String>
        ) : Question(), Serializable

        data class EditTextQuestion(
            val question: String,
            val incorrectText: String,
            val correctText: String
        ) : Question(), Serializable
    }
}

