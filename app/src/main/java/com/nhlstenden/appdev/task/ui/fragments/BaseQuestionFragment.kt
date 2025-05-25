package com.nhlstenden.appdev.task.ui.fragments

import android.os.Bundle
import android.view.View
import androidx.fragment.app.Fragment
import com.nhlstenden.appdev.task.domain.models.Question
import com.nhlstenden.appdev.task.listener.TaskCompleteListener

abstract class BaseQuestionFragment : Fragment() {
    protected lateinit var question: Question
    var taskCompleteListener: TaskCompleteListener? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        arguments?.let {
            question = it.getParcelable(ARG_QUESTION, Question::class.java) ?: throw IllegalArgumentException("Question is required")
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupQuestion()
    }

    protected abstract fun setupQuestion()

    companion object {
        const val ARG_QUESTION = "arg_question"
    }
} 