package com.nhlstenden.appdev.features.courses.viewmodels

import androidx.lifecycle.ViewModel
import com.nhlstenden.appdev.features.courses.models.Course
import com.nhlstenden.appdev.features.courses.repositories.CoursesRepositoryImpl
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
    private val coursesRepository: CoursesRepositoryImpl
) : ViewModel() {
    private val _courses = MutableStateFlow<List<Course>>(emptyList())
    val courses: StateFlow<List<Course>> = _courses.asStateFlow()

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    private val _selectedStars = MutableStateFlow<Int?>(null)
    val selectedStars: StateFlow<Int?> = _selectedStars.asStateFlow()

    private val _filteredCourses = MutableStateFlow<List<Course>>(emptyList())
    val filteredCourses: StateFlow<List<Course>> = _filteredCourses.asStateFlow()

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
            _courses.value = coursesRepository.getCoursesWithoutProgress()
        }
    }

    fun loadCoursesWithProgress(user: User) {
        viewModelScope.launch {
            _courses.value = coursesRepository.getCourses(user) ?: emptyList()
        }
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