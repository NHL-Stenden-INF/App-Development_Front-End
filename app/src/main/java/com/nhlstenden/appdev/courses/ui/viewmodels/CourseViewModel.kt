package com.nhlstenden.appdev.courses.ui.viewmodels

import com.nhlstenden.appdev.courses.domain.models.Course
import com.nhlstenden.appdev.courses.domain.models.Topic
import com.nhlstenden.appdev.shared.ui.base.BaseViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject

@HiltViewModel
class CourseViewModel @Inject constructor() : BaseViewModel() {
    private val _courses = MutableStateFlow<List<Course>>(emptyList())
    val courses: StateFlow<List<Course>> = _courses.asStateFlow()

    private val _topicsState = MutableStateFlow<TopicsState>(TopicsState.Loading)
    val topicsState: StateFlow<TopicsState> = _topicsState.asStateFlow()

    fun loadCourses() {
        launchWithLoading {
            // TODO: Implement course loading from repository
            // For now, using dummy data
            _courses.value = listOf(
                Course(
                    id = "1",
                    title = "HTML",
                    description = "Learn the fundamentals of HTML markup language",
                    difficulty = "Beginner",
                    imageResId = com.nhlstenden.appdev.R.drawable.html_course,
                    topics = listOf(
                        Topic(
                            id = "1",
                            title = "HTML Basics",
                            description = "Introduction to HTML elements and structure",
                            difficulty = "Beginner",
                            progress = 0
                        ),
                        Topic(
                            id = "2",
                            title = "HTML Forms",
                            description = "Creating and handling forms in HTML",
                            difficulty = "Intermediate",
                            progress = 0
                        )
                    )
                ),
                Course(
                    id = "2",
                    title = "CSS",
                    description = "Master CSS styling and layout techniques",
                    difficulty = "Intermediate",
                    imageResId = com.nhlstenden.appdev.R.drawable.css_course,
                    topics = listOf(
                        Topic(
                            id = "1",
                            title = "CSS Selectors",
                            description = "Understanding CSS selectors and specificity",
                            difficulty = "Beginner",
                            progress = 0
                        ),
                        Topic(
                            id = "2",
                            title = "CSS Layout",
                            description = "Mastering CSS layout techniques",
                            difficulty = "Intermediate",
                            progress = 0
                        )
                    )
                ),
                Course(
                    id = "3",
                    title = "SQL",
                    description = "Learn database management with SQL",
                    difficulty = "Advanced",
                    imageResId = com.nhlstenden.appdev.R.drawable.sql_course,
                    topics = listOf(
                        Topic(
                            id = "1",
                            title = "SQL Basics",
                            description = "Introduction to SQL queries",
                            difficulty = "Beginner",
                            progress = 0
                        ),
                        Topic(
                            id = "2",
                            title = "Advanced Queries",
                            description = "Complex SQL queries and joins",
                            difficulty = "Advanced",
                            progress = 0
                        )
                    )
                )
            )
        }
    }

    fun loadTopics(courseId: String) {
        launchWithLoading {
            try {
                _topicsState.value = TopicsState.Loading
                // TODO: Replace with actual API call
                val course = _courses.value.find { it.id == courseId }
                val topics = course?.topics ?: emptyList()
                _topicsState.value = TopicsState.Success(topics)
            } catch (e: Exception) {
                _topicsState.value = TopicsState.Error(e.message ?: "Unknown error occurred")
            }
        }
    }

    sealed class TopicsState {
        object Loading : TopicsState()
        data class Success(val topics: List<Topic>) : TopicsState()
        data class Error(val message: String) : TopicsState()
    }
} 