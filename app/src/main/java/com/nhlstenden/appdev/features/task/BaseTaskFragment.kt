package com.nhlstenden.appdev.features.task

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import com.nhlstenden.appdev.features.task.models.Question
import com.nhlstenden.appdev.features.task.models.TaskState

abstract class BaseTaskFragment : Fragment() {
    protected var currentIndex = 0
    protected val correctQuestionIds = mutableSetOf<String>()
    protected val incorrectQuestionIds = mutableSetOf<String>()
    protected val currentQuestions = mutableListOf<Question>()
    protected val questions = mutableListOf<Question>()
    protected var taskState: TaskState = TaskState.Initial

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(getLayoutId(), container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupUI()
        loadQuestions()
    }

    protected abstract fun getLayoutId(): Int
    protected abstract fun setupUI()
    protected abstract fun updateUI()
    protected abstract fun loadQuestions()

    protected fun onTaskFailed() {
        Log.d("BaseTaskFragment", "Task failed, showing failure dialog")
        taskState = TaskState.Failed
        showTaskFailureDialog()
    }

    private fun showTaskFailureDialog() {
        TaskFailureDialogFragment().show(childFragmentManager, "task_failure_dialog")
    }

    fun resetTask() {
        Log.d("BaseTaskFragment", "Resetting task state")
        currentIndex = 0
        correctQuestionIds.clear()
        incorrectQuestionIds.clear()
        currentQuestions.clear()
        currentQuestions.addAll(questions)
        taskState = TaskState.Initial
        updateUI()
    }

    protected fun onTaskComplete() {
        taskState = TaskState.Completed
        updateUI()
    }
} 