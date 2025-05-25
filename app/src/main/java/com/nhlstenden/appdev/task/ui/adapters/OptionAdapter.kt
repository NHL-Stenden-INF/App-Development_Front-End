package com.nhlstenden.appdev.task.ui.adapters

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.nhlstenden.appdev.databinding.ItemOptionBinding
import com.nhlstenden.appdev.task.domain.models.Question

class OptionAdapter(
    private val onOptionSelected: (String) -> Unit
) : ListAdapter<Question.Option, OptionViewHolder>(OptionDiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): OptionViewHolder {
        val binding = ItemOptionBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return OptionViewHolder(binding, onOptionSelected)
    }

    override fun onBindViewHolder(holder: OptionViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    private class OptionDiffCallback : DiffUtil.ItemCallback<Question.Option>() {
        override fun areItemsTheSame(oldItem: Question.Option, newItem: Question.Option): Boolean {
            return oldItem.id == newItem.id
        }

        override fun areContentsTheSame(oldItem: Question.Option, newItem: Question.Option): Boolean {
            return oldItem == newItem
        }
    }
} 