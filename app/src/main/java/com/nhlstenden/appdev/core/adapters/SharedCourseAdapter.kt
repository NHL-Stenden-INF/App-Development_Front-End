package com.nhlstenden.appdev.core.adapters

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
import com.nhlstenden.appdev.core.utils.DifficultyFormatter
import android.util.Log

data class CourseItem(
    val id: String,
    val title: String,
    val description: String = "",
    val progressText: String = "",
    val progressPercentage: Int = 0,
    val imageResId: Int,
    val showDescription: Boolean = false,
    val showDifficultyStars: Boolean = false,
    val difficulty: Int = 0
)

class SharedCourseAdapter(
    private val onCourseClick: (String) -> Unit
) : ListAdapter<CourseItem, SharedCourseAdapter.ViewHolder>(DiffCallback) {

    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val courseImage: ImageView = view.findViewById(R.id.courseImage)
        val courseTitle: TextView = view.findViewById(R.id.courseTitle)
        val difficultyLevel: TextView = view.findViewById(R.id.difficultyLevel)
        val courseDescription: TextView = view.findViewById(R.id.courseDescription)
        val progressBar: NumberProgressBar = view.findViewById(R.id.progressBar)
        val root: View = view
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_course, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val course = getItem(position)
        
        holder.courseTitle.text = course.title
        holder.courseImage.setImageResource(course.imageResId)
        
        // Set progress bar
        holder.progressBar.progress = course.progressPercentage
        
        // Handle description visibility
        if (course.showDescription && course.description.isNotEmpty()) {
            holder.courseDescription.text = course.description
            holder.courseDescription.visibility = View.VISIBLE
        } else {
            holder.courseDescription.visibility = View.GONE
        }
        
        // Handle difficulty/progress text
        if (course.showDifficultyStars && course.difficulty > 0) {
            holder.difficultyLevel.text = DifficultyFormatter.formatStars(course.difficulty)
        } else {
            holder.difficultyLevel.text = course.progressText
        }
        
        // Debug logging
        Log.d("SharedCourseAdapter", 
            "Course: ${course.title}, Progress: ${course.progressPercentage}%, Text: ${course.progressText}")

        holder.root.setOnClickListener {
            onCourseClick(course.id)
        }
    }

    companion object {
        private val DiffCallback = object : DiffUtil.ItemCallback<CourseItem>() {
            override fun areItemsTheSame(oldItem: CourseItem, newItem: CourseItem): Boolean {
                return oldItem.id == newItem.id
            }

            override fun areContentsTheSame(oldItem: CourseItem, newItem: CourseItem): Boolean {
                return oldItem == newItem
            }
        }
    }
} 