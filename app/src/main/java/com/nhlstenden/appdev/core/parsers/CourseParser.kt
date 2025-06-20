package com.nhlstenden.appdev.core.parsers

import com.nhlstenden.appdev.features.courses.model.Course

interface CourseParser {
    fun loadAllCourses(): List<Course>
    fun loadCourseByTitle(courseTitle: String): Course?
} 