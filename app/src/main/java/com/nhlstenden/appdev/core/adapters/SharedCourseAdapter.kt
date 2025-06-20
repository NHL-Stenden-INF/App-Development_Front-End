package com.nhlstenden.appdev.core.adapters

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import androidx.core.content.ContextCompat
import com.daimajia.numberprogressbar.NumberProgressBar
import com.google.android.material.card.MaterialCardView
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
    val difficulty: Int = 0,
    val isCompleted: Boolean = false,
    val isLocked: Boolean = false
) {
    companion object {
        fun isProgressComplete(progressPercentage: Int): Boolean = progressPercentage >= 100
    }
}

class SharedCourseAdapter(
    private val onCourseClick: (String) -> Unit
) : ListAdapter<CourseItem, SharedCourseAdapter.ViewHolder>(DiffCallback) {

    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val courseImage: ImageView = view.findViewById(R.id.courseImage)
        val courseTitle: TextView = view.findViewById(R.id.courseTitle)
        val difficultyLevel: TextView = view.findViewById(R.id.difficultyLevel)
        val courseDescription: TextView = view.findViewById(R.id.courseDescription)
        val progressBar: NumberProgressBar = view.findViewById(R.id.progressBar)
        val lockIcon: ImageView = view.findViewById(R.id.lockIcon)
        val cardView: MaterialCardView = view as MaterialCardView
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
        
        // Handle completion and lock states
        when {
            course.isCompleted -> {
                holder.lockIcon.visibility = View.GONE
                holder.itemView.alpha = 1.0f
                // Set completed course background color - subtle green tint
                holder.cardView.setCardBackgroundColor(
                    ContextCompat.getColor(holder.itemView.context, R.color.course_completed_bg)
                )
                // Optionally change progress text color to match
                holder.difficultyLevel.setTextColor(
                    ContextCompat.getColor(holder.itemView.context, R.color.course_completed_text)
                )
            }
            course.isLocked -> {
                holder.lockIcon.visibility = View.VISIBLE
                holder.itemView.alpha = 0.6f
                // Reset to default card background
                holder.cardView.setCardBackgroundColor(
                    ContextCompat.getColor(holder.itemView.context, R.color.cardBackground)
                )
                holder.difficultyLevel.setTextColor(
                    ContextCompat.getColor(holder.itemView.context, R.color.textSecondary)
                )
            }
            else -> {
                holder.lockIcon.visibility = View.GONE
                holder.itemView.alpha = 1.0f
                // Reset to default card background
                holder.cardView.setCardBackgroundColor(
                    ContextCompat.getColor(holder.itemView.context, R.color.cardBackground)
                )
                holder.difficultyLevel.setTextColor(
                    ContextCompat.getColor(holder.itemView.context, android.R.color.white)
                )
            }
        }
        
        // Debug logging
        Log.d("SharedCourseAdapter", 
            "Course: ${course.title}, Progress: ${course.progressPercentage}%, Completed: ${course.isCompleted}, Text: ${course.progressText}")

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