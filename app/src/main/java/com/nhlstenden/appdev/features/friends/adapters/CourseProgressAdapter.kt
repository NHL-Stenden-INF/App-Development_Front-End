package com.nhlstenden.appdev.features.friends.adapters

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.nhlstenden.appdev.databinding.ItemCourseProgressBinding
import com.nhlstenden.appdev.features.friends.models.CourseProgress

class CourseProgressAdapter : ListAdapter<CourseProgress, CourseProgressAdapter.CourseProgressViewHolder>(CourseProgressDiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): CourseProgressViewHolder {
        val binding = ItemCourseProgressBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return CourseProgressViewHolder(binding)
    }

    override fun onBindViewHolder(holder: CourseProgressViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    inner class CourseProgressViewHolder(
        private val binding: ItemCourseProgressBinding
    ) : RecyclerView.ViewHolder(binding.root) {
        
        fun bind(courseProgress: CourseProgress) {
            binding.courseProgressName.text = courseProgress.courseName
            binding.courseProgressPercentage.text = "${courseProgress.progress}%"
            binding.courseProgressBar.progress = courseProgress.progress
            binding.courseProgressBar.max = 100
            binding.courseProgressDetails.text = "${courseProgress.tasksCompleted} of ${courseProgress.totalTasks} tasks completed"
        }
    }

    private class CourseProgressDiffCallback : DiffUtil.ItemCallback<CourseProgress>() {
        override fun areItemsTheSame(oldItem: CourseProgress, newItem: CourseProgress): Boolean {
            return oldItem.courseId == newItem.courseId
        }

        override fun areContentsTheSame(oldItem: CourseProgress, newItem: CourseProgress): Boolean {
            return oldItem == newItem
        }
    }
} 