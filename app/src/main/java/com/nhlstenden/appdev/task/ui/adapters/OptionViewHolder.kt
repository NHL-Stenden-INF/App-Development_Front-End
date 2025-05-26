package com.nhlstenden.appdev.task.ui.adapters

import android.view.View
import androidx.recyclerview.widget.RecyclerView
import com.nhlstenden.appdev.databinding.ItemOptionBinding
import com.nhlstenden.appdev.task.domain.models.Question

class OptionViewHolder(
    private val binding: ItemOptionBinding,
    private val onOptionSelected: (String) -> Unit
) : RecyclerView.ViewHolder(binding.root) {

    fun bind(option: Question.Option) {
        binding.optionRadio.apply {
            text = option.text
            isChecked = false // Reset selection state
            setOnClickListener {
                onOptionSelected(option.id)
            }
        }
        binding.root.isSelected = false // Reset selection state
    }
} 