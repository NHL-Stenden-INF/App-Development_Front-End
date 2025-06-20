package com.nhlstenden.appdev.features.task.adapters

import android.util.Log
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import androidx.viewpager2.adapter.FragmentStateAdapter
import com.nhlstenden.appdev.features.task.models.Question
import com.nhlstenden.appdev.features.task.TaskCompleteListener
import com.nhlstenden.appdev.features.task.fragments.*

class TaskPagerAdapter(
    fragmentActivity: FragmentActivity,
    private val taskCompleteListener: TaskCompleteListener
) : FragmentStateAdapter(fragmentActivity) {

    private var questions: List<Question> = emptyList()
    private var fragmentCache: MutableMap<Int, Fragment> = mutableMapOf()

    fun submitList(newQuestions: List<Question>) {
        // Clear the fragment cache
        fragmentCache.clear()
        questions = newQuestions
        notifyDataSetChanged()
    }

    override fun getItemCount(): Int = questions.size

    override fun createFragment(position: Int): Fragment {
        val question = questions[position]
        val fragment = when (question) {
            is Question.MultipleChoiceQuestion -> MultipleChoiceFragment.newInstance(question)
            is Question.PressMistakeQuestion -> PressMistakesFragment.newInstance(question)
            is Question.FlipCardQuestion -> FlipCardFragment.newInstance(question)
            is Question.EditTextQuestion -> EditTextFragment.newInstance(question)
            else -> throw IllegalArgumentException("Unsupported question type: ${question::class.simpleName}")
        }

        fragment.taskCompleteListener = taskCompleteListener
        
        return fragment
    }

    override fun getItemId(position: Int): Long {
        // Generate a unique ID for each question to force recreation
        return questions[position].id.hashCode().toLong()
    }

    override fun containsItem(itemId: Long): Boolean {
        // Always return false to force recreation of fragments
        return false
    }
} 