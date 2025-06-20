package com.nhlstenden.appdev.features.courses.adapters

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.daimajia.numberprogressbar.NumberProgressBar
import com.nhlstenden.appdev.R
import com.nhlstenden.appdev.features.courses.model.Course
import com.nhlstenden.appdev.core.utils.ProgressCalculator

class CourseAdapter(
    private val onClick: (Course) -> Unit
) : ListAdapter<Course, CourseAdapter.CourseViewHolder>(DiffCallback) {
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): CourseViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_course, parent, false)
        return CourseViewHolder(view, onClick)
    }
    override fun onBindViewHolder(holder: CourseViewHolder, position: Int) {
        holder.bind(getItem(position))
    }
    class CourseViewHolder(
        itemView: View,
        private val onClick: (Course) -> Unit
    ) : RecyclerView.ViewHolder(itemView) {
        private val courseImage: ImageView = itemView.findViewById(R.id.courseImage)
        private val courseTitle: TextView = itemView.findViewById(R.id.courseTitle)
        private val courseDescription: TextView = itemView.findViewById(R.id.courseDescription)
        private val difficultyLevel: TextView = itemView.findViewById(R.id.difficultyLevel)
        private val progressBar: NumberProgressBar = itemView.findViewById(R.id.progressBar)

        fun bind(course: Course) {
            courseTitle.text = course.title
            courseDescription.text = course.description
            val stars = "★".repeat(course.difficulty) + "☆".repeat(5 - course.difficulty)
            difficultyLevel.text = stars
            courseImage.setImageResource(course.imageResId)
            progressBar.progress = ProgressCalculator.calculatePercentage(course.progress, course.totalTasks)
            itemView.setOnClickListener { onClick(course) }
        }
    }
    companion object {
        val DiffCallback = object : DiffUtil.ItemCallback<Course>() {
            override fun areItemsTheSame(oldItem: Course, newItem: Course): Boolean = oldItem.id == newItem.id
            override fun areContentsTheSame(oldItem: Course, newItem: Course): Boolean = oldItem == newItem
        }
    }
} 