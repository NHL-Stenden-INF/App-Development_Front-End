package com.nhlstenden.appdev.features.course.utils

import android.content.Context
import android.util.Log
import com.nhlstenden.appdev.features.courses.models.Course
import org.w3c.dom.Element
import java.io.InputStream
import javax.xml.parsers.DocumentBuilderFactory

class CourseParser(private val context: Context) {
    fun loadAllCourses(): List<Course> {
        val resourceId = context.resources.getIdentifier("courses", "raw", context.packageName)
        
        if (resourceId == 0) {
            Log.e("CourseParser", "No courses.xml resource found")
            return emptyList()
        }
        
        return try {
            val inputStream = context.resources.openRawResource(resourceId)
            parseCoursesXml(inputStream)
        } catch (e: Exception) {
            Log.e("CourseParser", "Error loading courses", e)
            emptyList()
        }
    }
    
    fun loadCourseByTitle(courseTitle: String): Course? {
        return loadAllCourses().find { it.title.equals(courseTitle, ignoreCase = true) }
    }
    
    private fun parseCoursesXml(inputStream: InputStream): List<Course> {
        val document = DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(inputStream)
        val coursesElement = document.documentElement
        val courseElements = coursesElement.getElementsByTagName("course")
        
        return List(courseElements.length) { index ->
            val courseElement = courseElements.item(index) as Element
            parseCourse(courseElement)
        }
    }
    
    private fun parseCourse(courseElement: Element): Course {
        val title = courseElement.getElementsByTagName("title").item(0).textContent
        val description = courseElement.getElementsByTagName("description").item(0).textContent
        val difficultyText = courseElement.getElementsByTagName("difficulty").item(0).textContent

        // Map textual difficulty to star rating (1 easy .. 5 hard)
        val difficulty = when (difficultyText.lowercase()) {
            "beginner" -> 1
            "intermediate" -> 2
            "advanced" -> 3
            "expert" -> 4
            "master" -> 5
            else -> 1
        }

        val imageResName = courseElement.getElementsByTagName("image").item(0).textContent
        val imageResId = context.resources.getIdentifier(imageResName, "drawable", context.packageName)

        val id = title
            .lowercase()
            .replace(' ', '_' )

        return Course(
            id = id,
            title = title,
            description = description,
            difficulty = difficulty,
            imageResId = imageResId,
            progress = 0,
            totalTasks = 0,
        )
    }
} 