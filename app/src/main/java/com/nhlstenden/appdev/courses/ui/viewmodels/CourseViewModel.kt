package com.nhlstenden.appdev.courses.ui.viewmodels

import androidx.lifecycle.ViewModel
import com.nhlstenden.appdev.courses.domain.models.Course
import com.nhlstenden.appdev.courses.domain.models.Topic
import com.nhlstenden.appdev.courses.parser.CourseParser
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject
import androidx.lifecycle.viewModelScope
import com.nhlstenden.appdev.courses.domain.repositories.CourseRepository
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
            _courses.value = listOf(
                Course(
                    id = "1",
                    title = "HTML",
                    description = "Learn the fundamentals of HTML markup language",
                    difficulty = "Beginner",
                    imageResId = com.nhlstenden.appdev.R.drawable.html_course,
                    topics = emptyList()
                ),
                Course(
                    id = "2",
                    title = "CSS",
                    description = "Master CSS styling and layout techniques",
                    difficulty = "Intermediate",
                    imageResId = com.nhlstenden.appdev.R.drawable.css_course,
                    topics = emptyList()
                ),
                Course(
                    id = "3",
                    title = "SQL",
                    description = "Learn database management with SQL",
                    difficulty = "Advanced",
                    imageResId = com.nhlstenden.appdev.R.drawable.sql_course,
                    topics = emptyList()
                )
            )
        }
    }

    fun loadTopics() {
        _topicsState.value = TopicsState.Loading
        viewModelScope.launch {
            try {
                val topics = courseRepository.getTopics()
                _topicsState.value = TopicsState.Success(topics)
            } catch (e: Exception) {
                _topicsState.value = TopicsState.Error(e.message ?: "Failed to load topics")
            }
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