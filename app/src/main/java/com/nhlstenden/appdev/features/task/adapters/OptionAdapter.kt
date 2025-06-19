package com.nhlstenden.appdev.features.task.adapters

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.nhlstenden.appdev.databinding.ItemOptionBinding
import com.nhlstenden.appdev.features.task.models.MultipleChoiceOption
import com.nhlstenden.appdev.features.task.models.Question

class OptionAdapter(
    private val onOptionSelected: (String) -> Unit
) : ListAdapter<MultipleChoiceOption, OptionViewHolder>(OptionDiffCallback()) {

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

    private class OptionDiffCallback : DiffUtil.ItemCallback<MultipleChoiceOption>() {
        override fun areItemsTheSame(oldItem: MultipleChoiceOption, newItem: MultipleChoiceOption): Boolean {
            return oldItem.id == newItem.id
        }

        override fun areContentsTheSame(oldItem: MultipleChoiceOption, newItem: MultipleChoiceOption): Boolean {
            return oldItem == newItem
        }
    }

    fun clear() {
        submitList(emptyList())
    }
} 