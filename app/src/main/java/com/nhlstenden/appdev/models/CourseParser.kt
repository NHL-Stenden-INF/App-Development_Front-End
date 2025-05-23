package com.nhlstenden.appdev.models

import android.content.Context
import android.util.Log
import com.nhlstenden.appdev.CourseTopicsFragment.Topic
import org.w3c.dom.Element
import java.io.InputStream
import javax.xml.parsers.DocumentBuilderFactory

class CourseParser(private val context: Context) {
    
    data class Course(
        val title: String,
        val description: String,
        val topics: List<Topic>
    )
    
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
        val courses = mutableListOf<Course>()
        
        try {
            val factory = DocumentBuilderFactory.newInstance()
            val builder = factory.newDocumentBuilder()
            val document = builder.parse(inputStream)
            document.documentElement.normalize()
            
            val courseNodes = document.getElementsByTagName("course")
            for (i in 0 until courseNodes.length) {
                val courseElement = courseNodes.item(i) as Element
                val title = courseElement.getElementsByTagName("title").item(0).textContent
                val description = courseElement.getElementsByTagName("description").item(0).textContent
                
                val topicsNodeList = courseElement.getElementsByTagName("topic")
                val topics = mutableListOf<Topic>()
                
                for (j in 0 until topicsNodeList.length) {
                    val topicElement = topicsNodeList.item(j) as Element
                    val topicTitle = topicElement.getElementsByTagName("title").item(0).textContent
                    val difficulty = topicElement.getElementsByTagName("difficulty").item(0).textContent
                    val topicDescription = topicElement.getElementsByTagName("description").item(0).textContent
                    val defaultProgress = topicElement.getElementsByTagName("default_progress").item(0).textContent.toInt()
                    
                    topics.add(Topic(topicTitle, difficulty, topicDescription, defaultProgress))
                }
                
                courses.add(Course(title, description, topics))
            }
            
        } catch (e: Exception) {
            Log.e("CourseParser", "Error parsing XML", e)
        } finally {
            inputStream.close()
        }
        
        return courses
    }
} 