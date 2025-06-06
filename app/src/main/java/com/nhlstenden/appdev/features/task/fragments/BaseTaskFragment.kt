package com.nhlstenden.appdev.features.task.fragments

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import com.nhlstenden.appdev.features.task.models.Question
import com.nhlstenden.appdev.features.task.TaskCompleteListener
import com.nhlstenden.appdev.features.task.TaskFailureDialogFragment

abstract class BaseTaskFragment : Fragment() {
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
        setupViews(view)
        bindQuestion()
    }

    override fun onResume() {
        super.onResume()
        // Reset the fragment state when it becomes visible
        bindQuestion()
    }

    protected abstract fun setupViews(view: View)
    protected abstract fun bindQuestion()

    protected fun onTaskComplete(isCorrect: Boolean) {
        taskCompleteListener?.onTaskComplete(isCorrect)
        if (isCorrect) {
            taskCompleteListener?.onTaskCompleted(question)
        }
    }

    fun onTaskFailed() {
        Log.d("BaseTaskFragment", "Task failed, showing failure dialog")
        TaskFailureDialogFragment().show(childFragmentManager, "task_failure_dialog")
    }

    fun resetTask() {
        Log.d("BaseTaskFragment", "Resetting task state")
        bindQuestion()
    }

    companion object {
        const val ARG_QUESTION = "arg_question"
    }
} 