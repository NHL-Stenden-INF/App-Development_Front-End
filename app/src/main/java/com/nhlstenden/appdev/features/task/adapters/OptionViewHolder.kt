package com.nhlstenden.appdev.features.task.adapters

import androidx.recyclerview.widget.RecyclerView
import com.nhlstenden.appdev.databinding.ItemOptionBinding
import com.nhlstenden.appdev.features.task.models.MultipleChoiceOption

class OptionViewHolder(
    private val binding: ItemOptionBinding,
    private val onOptionSelected: (String) -> Unit
) : RecyclerView.ViewHolder(binding.root) {

    fun bind(multipleChoiceOption: MultipleChoiceOption) {
        binding.optionRadio.apply {
            text = multipleChoiceOption.text
            isChecked = false // Reset selection state
            setOnClickListener {
                onOptionSelected(multipleChoiceOption.id)
            }
        }
        binding.root.isSelected = false // Reset selection state
    }
} 