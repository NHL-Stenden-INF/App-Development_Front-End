package com.nhlstenden.appdev.courses.ui.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.nhlstenden.appdev.courses.domain.models.Topic
import com.nhlstenden.appdev.courses.domain.repositories.CourseRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class CourseTopicsViewModel @Inject constructor(
    private val courseRepository: CourseRepository
) : ViewModel() {

    private val _topicsState = MutableStateFlow<TopicsState>(TopicsState.Loading)
    val topicsState: StateFlow<TopicsState> = _topicsState.asStateFlow()

    fun loadTopics() {
        viewModelScope.launch {
            _topicsState.value = TopicsState.Loading
            try {
                val topics = courseRepository.getTopics()
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