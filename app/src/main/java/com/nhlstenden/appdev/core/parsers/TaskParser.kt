package com.nhlstenden.appdev.core.parsers

import com.nhlstenden.appdev.features.courses.model.Task

interface TaskParser {
    fun loadAllTasksOfCourse(courseTitle: String): List<Task>
} 