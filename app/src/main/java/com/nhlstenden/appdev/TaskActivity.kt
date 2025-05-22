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
import com.nhlstenden.appdev.CourseTopicsFragment.Topic
import java.io.Serializable

class TaskActivity : AppCompatActivity(), OnTaskCompleteListener {
    private lateinit var taskName: TextView
    private lateinit var taskProgress: TextView
    private lateinit var exitButton: TextView
    private var questions: List<Question> = listOf(
        Question.MultipleChoiceQuestion(
            "What does OOP stand for?",
            listOf<Option>(
                Option("Object Oriented Programming", true),
                Option("Omg Obviously Porkchops", false),
                Option("OOP", false),
                Option("Obstructive Obedient People", false)
            ),
        ),
        Question.MultipleChoiceQuestion(
            "What does HTML stand for?",
            listOf<Option>(
                Option("Hyper-Text Markup Language", true),
                Option("Heiko, Theo, Mayo and Leo", false),
                Option("High-Transfer Marking Language", false),
                Option("High-Temperature Machine Learning", false)
            )
        )
    )
    private var failedQuestions: MutableList<Question> = mutableListOf()

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

        topicData = (intent.getSerializableExtra("TOPIC_DATA") as? Topic)!!
        taskName.text = topicData.title
        updateTaskProgress()

        exitButton.setOnClickListener {
            var dialog = EndTaskDialogFragment()
            dialog.show(supportFragmentManager, "popup")
        }

    }

    override fun onTaskCompleted(question: Question, hasSucceeded: Boolean) {
        if (!hasSucceeded) {
            failedQuestions.add(question)
        }
        activeQuestion++

        if (activeQuestion >= questions.size)
        {
            if (failedQuestions.isNotEmpty()) {
                activeQuestion = 0
                questions = failedQuestions
                failedQuestions = mutableListOf()
                // Randomize id list to ensure new views
                questionIds = questions.map { fragmentIdSeed++ }
                taskPagerAdapter.notifyDataSetChanged()
                viewPager.setCurrentItem(0, false)
            }
            else {
                // TODO: Make it save completion in the database

                val intent = Intent(this, TaskCompleteActivity::class.java)
                intent.putExtra("TOPIC_DATA", topicData)
                intent.putExtra("USER_DATA", this.intent.getParcelableExtra("USER_DATA", User::class.java))
                startActivity(intent)
                finish()
                return
            }

        }

        viewPager.currentItem = activeQuestion
        updateTaskProgress()
    }

    fun updateTaskProgress(){
        taskProgress.text = "${activeQuestion + 1} of ${questions.size}"
    }

    private var fragmentIdSeed = 0L
    private var questionIds: List<Long> = questions.map { fragmentIdSeed++ }

    private inner class TaskPagerAdapter(activity: FragmentActivity) : FragmentStateAdapter(activity) {
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

