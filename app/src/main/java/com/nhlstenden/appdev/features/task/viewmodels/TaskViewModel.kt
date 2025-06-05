package com.nhlstenden.appdev.features.task.viewmodels

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.nhlstenden.appdev.core.repositories.TaskRepository
import com.nhlstenden.appdev.features.task.models.Question
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class TaskViewModel @Inject constructor(
    private val taskRepository: TaskRepository
) : ViewModel() {

    private val _taskState = MutableLiveData<TaskState>()
    val taskState: LiveData<TaskState> = _taskState

    private val _currentPage = MutableLiveData<Int>()
    val currentPage: LiveData<Int> = _currentPage

    private val _error = MutableLiveData<String>()
    val error: LiveData<String> = _error

    fun loadTasks(taskId: String) {
        viewModelScope.launch {
            _taskState.value = TaskState.Loading
            try {
                val questions = taskRepository.getQuestionsForTask(taskId)
                _taskState.value = TaskState.Success(questions)
            } catch (e: Exception) {
                _taskState.value = TaskState.Error(e.message ?: "Failed to load tasks")
            }
        }
    }

    fun completeTask() {
        _taskState.value = TaskState.Completed
    }

    fun setCurrentPage(page: Int) {
        _currentPage.value = page
    }

    fun onQuestionCompleted(question: Question) {
        val currentState = _taskState.value
        if (currentState is TaskState.Success) {
            val updatedQuestions = currentState.questions.map { q ->
                if (q.id == question.id) question else q
            }
            _taskState.value = TaskState.Success(updatedQuestions)
        }
    }

    sealed class TaskState {
        object Loading : TaskState()
        data class Success(val questions: List<Question>) : TaskState()
        data class Error(val message: String) : TaskState()
        object Completed : TaskState()
    }
} 