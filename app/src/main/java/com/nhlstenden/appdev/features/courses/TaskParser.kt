package com.nhlstenden.appdev.features.courses

import android.content.Context
import android.util.Log
import com.nhlstenden.appdev.features.courses.model.Topic
import org.w3c.dom.Element
import java.io.InputStream
import javax.xml.parsers.DocumentBuilderFactory

class TaskParser(private val context: Context) {
    fun loadAllCoursesOfTopic(courseTitle: String): List<Topic> {
        val courseIdentifier: String = courseTitle
            .lowercase()
            .trim()
        val resourceId = context.resources.getIdentifier("${courseIdentifier}_topic", "raw", context.packageName)

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

    private fun parseCoursesXml(inputStream: InputStream): List<Topic> {
        val document = DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(inputStream)
        val topicsElement = document.documentElement
        val topicElements = topicsElement.getElementsByTagName("topic")

        return List(topicElements.length) { index ->
            val topicElement = topicElements.item(index) as Element
            parseTopic(topicElement)
        }
    }

    private fun parseTopic(topicElement: Element): Topic {
        val title = topicElement.getElementsByTagName("title").item(0).textContent
        val description = topicElement.getElementsByTagName("description").item(0).textContent
        val difficulty = topicElement.getElementsByTagName("difficulty").item(0).textContent

        return Topic(
            title = title,
            description = description,
            difficulty = difficulty,
            progress = 0
        )
    }
}