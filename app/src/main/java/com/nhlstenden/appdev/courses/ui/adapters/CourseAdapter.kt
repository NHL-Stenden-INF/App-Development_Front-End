package com.nhlstenden.appdev.courses.ui.adapters

import android.view.LayoutInflater
import android.view.ViewGroup
import com.nhlstenden.appdev.courses.domain.models.Course
import com.nhlstenden.appdev.databinding.ItemCourseBinding
import com.nhlstenden.appdev.shared.ui.components.BaseListAdapter
import com.nhlstenden.appdev.shared.ui.components.SimpleDiffCallback

class CourseAdapter(
    private val onCourseClick: (Course) -> Unit
) : BaseListAdapter<Course, ItemCourseBinding>(SimpleDiffCallback()) {

    override fun getViewBinding(inflater: LayoutInflater, parent: ViewGroup): ItemCourseBinding {
        return ItemCourseBinding.inflate(inflater, parent, false)
    }

    override fun onBindViewHolder(holder: BaseViewHolder<ItemCourseBinding>, item: Course) {
        with(holder.binding) {
            courseTitle.text = item.title
            courseDescription.text = item.description
            difficultyLevel.text = item.difficulty
            courseImage.setImageResource(item.imageResId)
            
            root.setOnClickListener { onCourseClick(item) }
        }
    }
} 