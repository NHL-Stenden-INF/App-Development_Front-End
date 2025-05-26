package com.nhlstenden.appdev.courses.parser

import android.content.Context
import android.util.Log
import com.nhlstenden.appdev.courses.domain.models.Course
import com.nhlstenden.appdev.courses.domain.models.Topic
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
    
    fun loadTopicsByCourseId(courseId: String): List<Topic> {
        return loadAllCourses().find { it.id == courseId }?.topics ?: emptyList()
    }
    
    fun loadCourseById(courseId: String): Course? {
        return loadAllCourses().find { it.id == courseId }
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
        val id = courseElement.getElementsByTagName("id").item(0).textContent
        val title = courseElement.getElementsByTagName("title").item(0).textContent
        val description = courseElement.getElementsByTagName("description").item(0).textContent
        val difficulty = courseElement.getElementsByTagName("difficulty").item(0).textContent
        val imageResName = courseElement.getElementsByTagName("image").item(0).textContent
        val imageResId = context.resources.getIdentifier(imageResName, "drawable", context.packageName)
        
        val topicsElement = courseElement.getElementsByTagName("topics").item(0) as Element
        val topicElements = topicsElement.getElementsByTagName("topic")
        val topics = List(topicElements.length) { index ->
            val topicElement = topicElements.item(index) as Element
            parseTopic(topicElement)
        }
        
        return Course(
            id = id,
            title = title,
            description = description,
            difficulty = difficulty,
            imageResId = imageResId,
            topics = topics
        )
    }
    
    private fun parseTopic(topicElement: Element): Topic {
        val id = topicElement.getAttribute("id")
        val title = topicElement.getElementsByTagName("title").item(0).textContent
        val description = topicElement.getElementsByTagName("description").item(0).textContent
        val difficulty = topicElement.getElementsByTagName("difficulty").item(0).textContent
        val progress = topicElement.getElementsByTagName("progress").item(0).textContent.toIntOrNull() ?: 0
        
        return Topic(
            id = id,
            title = title,
            description = description,
            difficulty = difficulty,
            progress = progress
        )
    }
} 