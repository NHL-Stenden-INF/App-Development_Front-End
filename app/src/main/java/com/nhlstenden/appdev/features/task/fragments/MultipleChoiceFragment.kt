package com.nhlstenden.appdev.features.task.fragments

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.RadioButton
import android.widget.RadioGroup
import android.widget.TextView
import com.google.android.material.button.MaterialButton
import com.nhlstenden.appdev.R
import com.nhlstenden.appdev.features.task.models.Question
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MultipleChoiceFragment : BaseTaskFragment() {
    private lateinit var questionText: TextView
    private lateinit var optionsGroup: LinearLayout
    private lateinit var submitButton: MaterialButton
    private lateinit var nextButton: MaterialButton
    private lateinit var feedbackText: TextView
    private var optionButtons: List<MaterialButton> = emptyList()
    private var shuffledOptions: List<Question.Option> = emptyList()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_multiple_choice, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupViews(view)
        bindQuestion() // Ensure UI is always reset on fragment recreation
    }

    override fun setupViews(view: View) {
        questionText = view.findViewById(R.id.questionText)
        optionsGroup = view.findViewById(R.id.optionsGroup)
        submitButton = view.findViewById(R.id.submitButton)
        nextButton = view.findViewById(R.id.nextButton)
        feedbackText = view.findViewById(R.id.feedbackText)

        nextButton.visibility = View.GONE
        feedbackText.visibility = View.GONE

        // Remove all views from optionsGroup
        optionsGroup.removeAllViews()
        // Add four MaterialButtons for options
        optionButtons = List(4) { i ->
            MaterialButton(requireContext()).apply {
                layoutParams = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
                ).apply { setMargins(0, 8, 0, 8) }
                isCheckable = true
                isClickable = true
                setOnClickListener {
                    if (optionButtons.any { it.isEnabled }) {
                        optionButtons.forEachIndexed { j, btn ->
                            btn.isChecked = false
                            btn.setBackgroundColor(0xFFEEEEEE.toInt())
                        }
                        this.isChecked = true
                        this.setBackgroundColor(0xFF2196F3.toInt()) // blue for selected
                    }
                }
            }.also { optionsGroup.addView(it) }
        }

        submitButton.setOnClickListener {
            val selectedIndex = optionButtons.indexOfFirst { it.isChecked }
            if (selectedIndex != -1) {
                val selectedOption = shuffledOptions[selectedIndex]
                val isCorrect = selectedOption.isCorrect
                
                // Feedback coloring
                optionButtons.forEachIndexed { i, btn ->
                    btn.isEnabled = false
                    if (i == selectedIndex) {
                        btn.setBackgroundColor(
                            if (isCorrect) 0xFF4CAF50.toInt() else 0xFFF44336.toInt()
                        )
                        btn.setTextColor(0xFFFFFFFF.toInt()) // White text for selected button
                    } else if (shuffledOptions[i].isCorrect) {
                        btn.setBackgroundColor(0xFF4CAF50.toInt())
                        btn.setTextColor(0xFFFFFFFF.toInt()) // White text for correct answer
                    } else {
                        btn.setTextColor(0xFF757575.toInt()) // Gray text for unselected incorrect answers
                    }
                }

                // Show feedback
                feedbackText.visibility = View.VISIBLE
                feedbackText.text = if (isCorrect) "Correct!" else "Incorrect. The correct answer was: ${shuffledOptions.find { it.isCorrect }?.text}"
                feedbackText.setTextColor(if (isCorrect) 0xFF4CAF50.toInt() else 0xFFF44336.toInt())

                submitButton.visibility = View.GONE
                nextButton.visibility = View.VISIBLE
            }
        }

        nextButton.setOnClickListener {
            val selectedIndex = optionButtons.indexOfFirst { it.isChecked }
            if (selectedIndex != -1) {
                val selectedOption = shuffledOptions[selectedIndex]
                onTaskComplete(selectedOption.isCorrect)
            }
        }
    }

    override fun bindQuestion() {
        questionText.text = question.text
        shuffledOptions = question.options.shuffled()
        
        // Reset UI state
        optionButtons.forEach { btn ->
            btn.isEnabled = true
            btn.isChecked = false
            btn.setBackgroundColor(0xFFEEEEEE.toInt())
            btn.setTextColor(0xFF000000.toInt())
        }
        
        // Set option texts
        optionButtons.forEachIndexed { i, btn ->
            btn.text = shuffledOptions[i].text
        }

        submitButton.visibility = View.VISIBLE
        nextButton.visibility = View.GONE
        feedbackText.visibility = View.GONE
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