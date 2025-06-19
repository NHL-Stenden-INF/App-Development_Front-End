package com.nhlstenden.appdev.features.progress.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.nhlstenden.appdev.core.repositories.AuthRepository
import com.nhlstenden.appdev.features.courses.repositories.CourseRepositoryImpl
import com.nhlstenden.appdev.features.courses.model.Course
import com.nhlstenden.appdev.features.progress.models.CourseProgress
import com.nhlstenden.appdev.core.utils.ProgressCalculator
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import android.util.Log
import javax.inject.Inject

@HiltViewModel
class ProgressViewModel @Inject constructor(
    private val authRepository: AuthRepository,
    private val courseRepository: CourseRepositoryImpl
) : ViewModel() {

    private val _uiState = MutableStateFlow(ProgressUiState())
    val uiState: StateFlow<ProgressUiState> = _uiState.asStateFlow()

    private val _courseProgressList = MutableStateFlow<List<CourseProgress>>(emptyList())
    val courseProgressList: StateFlow<List<CourseProgress>> = _courseProgressList.asStateFlow()

    private val _overallProgress = MutableStateFlow(OverallProgress())
    val overallProgress: StateFlow<OverallProgress> = _overallProgress.asStateFlow()

    fun loadData() {
        viewModelScope.launch(Dispatchers.IO) {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null)
            
            try {
                val currentUser = authRepository.getCurrentUserSync()
                if (currentUser != null) {
                    val courses = courseRepository.getCourses(currentUser)
                    withContext(Dispatchers.Main) {
                        processCourseData(courses)
                    }
                } else {
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        error = "User not logged in"
                    )
                }
            } catch (e: RuntimeException) {
                Log.e("ProgressViewModel", "Error loading data", e)
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    error = e.message ?: "Unknown error occurred"
                )
            }
        }
    }

    private fun processCourseData(courses: List<Course>?) {
        courses?.let { courseList ->
            val progressCourses = courseList.mapNotNull { course ->
                if (course.progress == 0) {
                    Log.d("ProgressViewModel", "Not adding course: ${course.title}")
                    return@mapNotNull null
                }
                Log.d("ProgressViewModel", "Adding course: ${course.title}")

                CourseProgress(
                    id = course.id,
                    title = course.title,
                    completionStatus = "${course.progress}/${course.totalTasks}",
                    progressPercentage = ProgressCalculator.calculatePercentage(course.progress, course.totalTasks),
                    imageResId = course.imageResId
                )
            }

            val overallData = calculateOverallProgress(courseList)
            
            _courseProgressList.value = progressCourses
            _overallProgress.value = overallData
            _uiState.value = _uiState.value.copy(isLoading = false)
        }
    }

    private fun calculateOverallProgress(courses: List<Course>): OverallProgress {
        var totalTasks = 0
        var completedTasks = 0

        courses.forEach { course ->
            if (course.progress != 0) {
                totalTasks += course.totalTasks
                completedTasks += course.progress
            }
        }

        val remainingTasks = totalTasks - completedTasks
        val completionPercentage = ProgressCalculator.calculatePercentage(completedTasks, totalTasks)

        return OverallProgress(
            totalTasks = totalTasks,
            completedTasks = completedTasks,
            remainingTasks = remainingTasks,
            completionPercentage = completionPercentage
        )
    }
}

data class ProgressUiState(
    val isLoading: Boolean = false,
    val error: String? = null
)

data class OverallProgress(
    val totalTasks: Int = 0,
    val completedTasks: Int = 0,
    val remainingTasks: Int = 0,
    val completionPercentage: Int = 0
) 