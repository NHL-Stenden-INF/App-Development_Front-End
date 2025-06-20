package com.nhlstenden.appdev.features.course.viewmodels

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.nhlstenden.appdev.features.courses.model.Task
import com.nhlstenden.appdev.features.courses.model.Course
import com.nhlstenden.appdev.features.course.repositories.CourseRepository
import com.nhlstenden.appdev.core.parsers.CourseParser
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
    private val courseRepository: CourseRepository,
    private val courseParser: CourseParser
) : ViewModel() {

    private val _tasksState = MutableLiveData<TasksState>(TasksState.Loading)
    val tasksState: LiveData<TasksState> = _tasksState

    private val _courseProgress = MutableStateFlow<Map<String, Int>>(emptyMap())
    val courseProgress: StateFlow<Map<String, Int>> = _courseProgress.asStateFlow()

    private val _courseInfo = MutableLiveData<Course?>()
    val courseInfo: LiveData<Course?> = _courseInfo

    fun loadTasks(courseId: String, user: User) {
        _tasksState.value = TasksState.Loading
        viewModelScope.launch {
            try {
                val tasks = courseRepository.getTasks(courseId)
                val progress = courseRepository.getCourseProgress(user.id.toString(), courseId)
                _courseProgress.value = progress
                _tasksState.value = TasksState.Success(tasks)
            } catch (e: Exception) {
                _tasksState.value = TasksState.Error(e.message ?: "Failed to load tasks")
            }
        }
    }

    fun loadCourseInfo(courseId: String) {
        viewModelScope.launch {
            try {
                val course = courseParser.loadCourseByTitle(courseId)
                _courseInfo.value = course
            } catch (e: Exception) {
                // Log error but don't fail the whole UI
                _courseInfo.value = null
            }
        }
    }

    sealed class TasksState {
        object Loading : TasksState()
        data class Success(val tasks: List<Task>) : TasksState()
        data class Error(val message: String) : TasksState()
    }
} 