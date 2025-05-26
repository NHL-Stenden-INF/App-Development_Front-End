package com.nhlstenden.appdev.courses.models

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.nhlstenden.appdev.R
import com.google.android.material.progressindicator.LinearProgressIndicator

data class Course(
    val id: String,
    val title: String,
    val level: String,
    val description: String,
    val imageResId: Int
)

class CourseAdapter(
    private val onClick: (Course) -> Unit
) : RecyclerView.Adapter<CourseAdapter.CourseViewHolder>() {

    private var courses: List<Course> = emptyList()

    class CourseViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val courseImage: ImageView = view.findViewById(R.id.courseImage)
        val courseTitle: TextView = view.findViewById(R.id.courseTitle)
        val difficultyLevel: TextView = view.findViewById(R.id.difficultyLevel)
        val courseDescription: TextView = view.findViewById(R.id.courseDescription)
        val progressBar: LinearProgressIndicator? = view.findViewById(R.id.progressBar)
        val progressPercentage: TextView? = view.findViewById(R.id.progressPercentage)

        fun bind(course: Course, onClick: (Course) -> Unit) {
            courseImage.setImageResource(course.imageResId)
            courseImage.contentDescription = "${course.title} course icon"
            courseTitle.text = course.title
            difficultyLevel.text = course.level
            courseDescription.text = course.description
            progressBar?.visibility = View.GONE
            progressPercentage?.visibility = View.GONE
            itemView.setOnClickListener { onClick(course) }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): CourseViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_course, parent, false)
        return CourseViewHolder(view)
    }

    override fun onBindViewHolder(holder: CourseViewHolder, position: Int) {
        holder.bind(courses[position], onClick)
    }

    override fun getItemCount() = courses.size

    fun submitList(newCourses: List<Course>) {
        courses = newCourses
        notifyDataSetChanged()
    }
} 