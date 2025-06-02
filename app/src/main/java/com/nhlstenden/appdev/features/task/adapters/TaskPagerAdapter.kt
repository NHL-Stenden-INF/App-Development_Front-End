package com.nhlstenden.appdev.features.task.adapters

import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import androidx.viewpager2.adapter.FragmentStateAdapter
import com.nhlstenden.appdev.features.task.models.Question
import com.nhlstenden.appdev.features.task.models.QuestionType
import com.nhlstenden.appdev.features.task.TaskCompleteListener
import com.nhlstenden.appdev.features.task.fragments.*

class TaskPagerAdapter(
    fragmentActivity: FragmentActivity,
    private val taskCompleteListener: TaskCompleteListener
) : FragmentStateAdapter(fragmentActivity) {

    private var questions: List<Question> = emptyList()

    fun submitList(newQuestions: List<Question>) {
        questions = newQuestions
        notifyDataSetChanged()
    }

    override fun getItemCount(): Int = questions.size

    override fun createFragment(position: Int): Fragment {
        val question = questions[position]
        val fragment = when (question.type) {
            QuestionType.MULTIPLE_CHOICE -> MultipleChoiceFragment.newInstance(question)
            QuestionType.TRUE_FALSE -> TrueFalseFragment.newInstance(question)
            QuestionType.OPEN_ENDED -> OpenEndedFragment.newInstance(question)
            QuestionType.FLIP_CARD -> FlipCardFragment.newInstance(question)
            QuestionType.PRESS_MISTAKES -> PressMistakesFragment.newInstance(question)
            QuestionType.EDIT_TEXT -> EditTextFragment.newInstance(question)
            else -> throw IllegalArgumentException("Unsupported question type: ${question.type}")
        }
        
        if (fragment is BaseTaskFragment) {
            fragment.taskCompleteListener = taskCompleteListener
        }
        
        return fragment
    }
} 