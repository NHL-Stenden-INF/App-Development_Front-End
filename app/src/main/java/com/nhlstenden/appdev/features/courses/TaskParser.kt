package com.nhlstenden.appdev.features.courses

import android.content.Context
import android.util.Log
import com.nhlstenden.appdev.features.courses.model.Task
import org.w3c.dom.Element
import java.io.InputStream
import javax.xml.parsers.DocumentBuilderFactory

class TaskParser(private val context: Context) {
    fun loadAllCoursesOfTask(courseTitle: String): List<Task> {
        val courseIdentifier: String = courseTitle
            .lowercase()
            .trim()
        val resourceId = context.resources.getIdentifier("${courseIdentifier}_tasks", "raw", context.packageName)

        if (resourceId == 0) {
            Log.e("CourseParser", "No ${courseIdentifier}_tasks.xml resource found")
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

    private fun parseCoursesXml(inputStream: InputStream): List<Task> {
        val document = DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(inputStream)
        val tasksElement = document.documentElement
        val taskElements = tasksElement.getElementsByTagName("task")

        return List(taskElements.length) { index ->
            val taskElement = taskElements.item(index) as Element
            parseTask(taskElement)
        }
    }

    private fun parseTask(taskElement: Element): Task {
        val title = taskElement.getElementsByTagName("title").item(0).textContent
        val description = taskElement.getElementsByTagName("description").item(0).textContent
        val difficulty = taskElement.getElementsByTagName("difficulty").item(0).textContent

        val id = title
            .lowercase()
            .replace(' ', '_' )

        return Task(
            id = id,
            title = title,
            description = description,
            difficulty = difficulty,
        )
    }
}