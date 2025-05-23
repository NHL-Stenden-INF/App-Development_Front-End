package com.nhlstenden.appdev.models

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.nhlstenden.appdev.R

data class Course(
    val title: String,
    val level: String,
    val description: String,
    val imageResId: Int
)

class CourseAdapter(
    private val courses: List<Course>,
    private val onCourseClick: (Course) -> Unit
) : RecyclerView.Adapter<CourseAdapter.CourseViewHolder>() {

    class CourseViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val courseImage: ImageView = view.findViewById(R.id.courseImage)
        val courseTitle: TextView = view.findViewById(R.id.courseTitle)
        val difficultyLevel: TextView = view.findViewById(R.id.difficultyLevel)
        val courseDescription: TextView = view.findViewById(R.id.courseDescription)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): CourseViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_task, parent, false)
        return CourseViewHolder(view)
    }

    override fun onBindViewHolder(holder: CourseViewHolder, position: Int) {
        val course = courses[position]
        holder.apply {
            courseImage.setImageResource(course.imageResId)
            courseImage.contentDescription = "${course.title} course icon"
            courseTitle.text = course.title
            difficultyLevel.text = course.level
            courseDescription.text = course.description
            
            itemView.setOnClickListener {
                onCourseClick(course)
            }
        }
    }

    override fun getItemCount() = courses.size
} 