package com.nhlstenden.appdev.task.ui.fragments

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.RadioButton
import android.widget.RadioGroup
import android.widget.TextView
import com.google.android.material.button.MaterialButton
import com.nhlstenden.appdev.R
import com.nhlstenden.appdev.task.domain.models.Question
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MultipleChoiceFragment : BaseTaskFragment() {
    private lateinit var questionText: TextView
    private lateinit var optionsGroup: RadioGroup
    private lateinit var submitButton: MaterialButton

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_multiple_choice, container, false)
    }

    override fun setupViews(view: View) {
        questionText = view.findViewById(R.id.questionText)
        optionsGroup = view.findViewById(R.id.optionsGroup)
        submitButton = view.findViewById(R.id.submitButton)

        submitButton.setOnClickListener {
            val selectedId = optionsGroup.checkedRadioButtonId
            if (selectedId != -1) {
                val selectedOption = view.findViewById<RadioButton>(selectedId)
                val isCorrect = selectedOption.text.toString() == question.correctAnswer
                onTaskComplete(isCorrect)
            }
        }
    }

    override fun bindQuestion() {
        questionText.text = question.text
        
        // Clear existing options
        optionsGroup.removeAllViews()
        
        // Add new options
        question.options.forEach { option ->
            val radioButton = RadioButton(requireContext()).apply {
                text = option.text
                id = View.generateViewId()
            }
            optionsGroup.addView(radioButton)
        }
    }

    companion object {
        fun newInstance(question: Question): MultipleChoiceFragment {
            return MultipleChoiceFragment().apply {
                arguments = Bundle().apply {
                    putParcelable(ARG_QUESTION, question)
                }
            }
        }
    }
} 