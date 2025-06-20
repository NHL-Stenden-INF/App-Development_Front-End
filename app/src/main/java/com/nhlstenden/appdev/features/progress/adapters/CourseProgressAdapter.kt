package com.nhlstenden.appdev.features.progress.adapters

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
import com.nhlstenden.appdev.features.progress.models.CourseProgress

class CourseProgressAdapter(
    private val onCourseClick: (String) -> Unit
) : ListAdapter<CourseProgress, CourseProgressAdapter.ViewHolder>(DiffCallback) {

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
        holder.difficultyLevel.text = course.completionStatus
        holder.progressBar.progress = course.progressPercentage
        holder.courseImage.setImageResource(course.imageResId)
        
        // Hide description to match home screen layout (DRY principle)
        holder.courseDescription.visibility = View.GONE
        
        // Debug logging
        android.util.Log.d("CourseProgressAdapter", 
            "Setting progress for ${course.title}: ${course.progressPercentage}% (${course.completionStatus})")

        holder.root.setOnClickListener {
            onCourseClick(course.id)
        }
    }

    companion object {
        private val DiffCallback = object : DiffUtil.ItemCallback<CourseProgress>() {
            override fun areItemsTheSame(oldItem: CourseProgress, newItem: CourseProgress): Boolean {
                return oldItem.id == newItem.id
            }

            override fun areContentsTheSame(oldItem: CourseProgress, newItem: CourseProgress): Boolean {
                return oldItem == newItem
            }
        }
    }
} 