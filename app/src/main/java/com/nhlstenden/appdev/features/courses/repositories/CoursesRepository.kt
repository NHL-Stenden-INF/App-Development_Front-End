package com.nhlstenden.appdev.features.courses.repositories

import com.nhlstenden.appdev.core.models.User
import com.nhlstenden.appdev.features.courses.models.Course

interface CoursesRepository {
    suspend fun getCourses(user: User): List<Course>?
    suspend fun getCoursesWithoutProgress(): List<Course>
} 