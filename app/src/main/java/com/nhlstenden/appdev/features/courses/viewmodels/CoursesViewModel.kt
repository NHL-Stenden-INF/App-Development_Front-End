package com.nhlstenden.appdev.features.courses.viewmodels

import androidx.lifecycle.ViewModel
import com.nhlstenden.appdev.features.courses.model.Course
import com.nhlstenden.appdev.features.courses.repositories.CoursesRepository
import com.nhlstenden.appdev.core.repositories.AuthRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.launch
import com.nhlstenden.appdev.core.models.User

@HiltViewModel
class CoursesViewModel @Inject constructor(
    private val coursesRepository: CoursesRepository,
    private val authRepository: AuthRepository
) : ViewModel() {
    private val _courses = MutableStateFlow<List<Course>>(emptyList())
    val courses: StateFlow<List<Course>> = _courses.asStateFlow()

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    private val _selectedStars = MutableStateFlow<Int?>(null)
    val selectedStars: StateFlow<Int?> = _selectedStars.asStateFlow()

    private val _filteredCourses = MutableStateFlow<List<Course>>(emptyList())
    val filteredCourses: StateFlow<List<Course>> = _filteredCourses.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()

    init {
        viewModelScope.launch {
            // Combine search query and difficulty filter
            kotlinx.coroutines.flow.combine(
                _courses,
                _searchQuery,
                _selectedStars
            ) { courses, query, stars ->
                courses.filter { course ->
                    val matchesSearch = query.isEmpty() || 
                        course.title.contains(query, ignoreCase = true)
                    
                    val matchesStars = stars == null || course.difficulty == stars
                    
                    matchesSearch && matchesStars
                }
            }.collect { filtered ->
                _filteredCourses.value = filtered
            }
        }
    }

    fun loadCourses() {
        viewModelScope.launch {
            _isLoading.value = true
            _error.value = null
            try {
                _courses.value = coursesRepository.getCoursesWithoutProgress()
            } catch (e: Exception) {
                _error.value = e.message ?: "Failed to load courses"
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun loadCoursesWithProgress() {
        viewModelScope.launch {
            _isLoading.value = true
            _error.value = null
            try {
                val currentUser = authRepository.getCurrentUserSync()
                if (currentUser != null) {
                    _courses.value = coursesRepository.getCourses(currentUser) ?: emptyList()
                } else {
                    _error.value = "User not authenticated"
                }
            } catch (e: Exception) {
                _error.value = e.message ?: "Failed to load courses"
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun refreshCourses() {
        loadCoursesWithProgress()
    }

    fun updateSearchQuery(query: String) {
        _searchQuery.value = query
    }

    fun setStarFilter(stars: Int?) {
        _selectedStars.value = stars
    }

    fun clearFilters() {
        _searchQuery.value = ""
        _selectedStars.value = null
    }
} 