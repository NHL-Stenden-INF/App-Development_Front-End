package com.nhlstenden.appdev.features.courses

import androidx.lifecycle.ViewModel
import com.nhlstenden.appdev.features.courses.model.Course
import com.nhlstenden.appdev.features.courses.model.Topic
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.launch

@HiltViewModel
class CourseViewModel @Inject constructor(
    private val courseRepository: CourseRepository
) : ViewModel() {
    private val _courses = MutableStateFlow<List<Course>>(emptyList())
    val courses: StateFlow<List<Course>> = _courses.asStateFlow()

    private val _topicsState = MutableStateFlow<TopicsState>(TopicsState.Loading)
    val topicsState: StateFlow<TopicsState> = _topicsState.asStateFlow()

    fun loadCourses() {
        viewModelScope.launch {
            _courses.value = courseRepository.getCourses()
        }
    }

    fun loadTopics(courseId: String) {
        _topicsState.value = TopicsState.Loading
        viewModelScope.launch {
            try {
                val topics = courseRepository.getTopics(courseId)
                _topicsState.value = TopicsState.Success(topics)
            } catch (e: Exception) {
                _topicsState.value = TopicsState.Error(e.message ?: "Failed to load topics")
            }
        }
    }

    sealed class TopicsState {
        object Loading : TopicsState()
        data class Success(val topics: List<Topic>) : TopicsState()
        data class Error(val message: String) : TopicsState()
    }
} 