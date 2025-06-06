package com.nhlstenden.appdev.features.courses

import androidx.lifecycle.ViewModel
import com.nhlstenden.appdev.features.courses.model.Course
import com.nhlstenden.appdev.features.courses.model.Task
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.launch
import com.nhlstenden.appdev.core.models.User

@HiltViewModel
class CourseViewModel @Inject constructor(
    private val courseRepository: CourseRepository
) : ViewModel() {
    private val _courses = MutableStateFlow<List<Course>>(emptyList())
    val courses: StateFlow<List<Course>> = _courses.asStateFlow()

    private val _tasksState = MutableStateFlow<TasksState>(TasksState.Loading)
    val tasksState: StateFlow<TasksState> = _tasksState.asStateFlow()

    private val _courseProgress = MutableStateFlow(0)
    val courseProgress: StateFlow<Int> = _courseProgress.asStateFlow()

    fun loadCourses() {
        viewModelScope.launch {
            _courses.value = courseRepository.getCoursesWithoutProgress()
        }
    }

    fun loadCoursesWithProgress(user: User) {
        viewModelScope.launch {
            _courses.value = courseRepository.getCourses(user) ?: emptyList()
        }
    }

    fun loadTasks(courseId: String, user: User) {
        _tasksState.value = TasksState.Loading
        viewModelScope.launch {
            try {
                val tasks = courseRepository.getTasks(courseId)
                val courses = courseRepository.getCourses(user)
                val course = courses?.find { it.id == courseId }
                _courseProgress.value = course?.progress ?: 0
                _tasksState.value = TasksState.Success(tasks)
            } catch (e: Exception) {
                _tasksState.value = TasksState.Error(e.message ?: "Failed to load tasks")
            }
        }
    }

    sealed class TasksState {
        object Loading : TasksState()
        data class Success(val tasks: List<Task>) : TasksState()
        data class Error(val message: String) : TasksState()
    }
} 