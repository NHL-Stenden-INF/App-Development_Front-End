package com.nhlstenden.appdev.features.task.fragments

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import com.google.android.material.button.MaterialButton
import com.nhlstenden.appdev.R
import com.nhlstenden.appdev.features.task.models.Question

class TrueFalseFragment : BaseTaskFragment() {
    private lateinit var questionText: TextView
    private lateinit var trueButton: MaterialButton
    private lateinit var falseButton: MaterialButton
    private lateinit var nextButton: MaterialButton
    private lateinit var feedbackText: TextView
    private var hasAnswered = false

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_true_false, container, false)
    }

    override fun setupViews(view: View) {
        questionText = view.findViewById(R.id.questionText)
        trueButton = view.findViewById(R.id.trueButton)
        falseButton = view.findViewById(R.id.falseButton)
        nextButton = view.findViewById(R.id.nextButton)
        feedbackText = view.findViewById(R.id.feedbackText)

        nextButton.visibility = View.GONE
        feedbackText.visibility = View.GONE

        trueButton.setOnClickListener {
            if (!hasAnswered) {
                checkAnswer(true)
            }
        }

        falseButton.setOnClickListener {
            if (!hasAnswered) {
                checkAnswer(false)
            }
        }

        nextButton.setOnClickListener {
            val correctAnswer = question.correctAnswer?.toBoolean() ?: false
            val isCorrect = (trueButton.isChecked && correctAnswer) || (falseButton.isChecked && !correctAnswer)
            onTaskComplete(isCorrect)
        }
    }

    override fun bindQuestion() {
        questionText.text = question.text
        hasAnswered = false
        trueButton.isEnabled = true
        falseButton.isEnabled = true
        trueButton.isChecked = false
        falseButton.isChecked = false
        trueButton.setBackgroundColor(0xFFEEEEEE.toInt())
        falseButton.setBackgroundColor(0xFFEEEEEE.toInt())
        nextButton.visibility = View.GONE
        feedbackText.visibility = View.GONE
    }

    private fun checkAnswer(selectedAnswer: Boolean) {
        if (hasAnswered) return
        hasAnswered = true
        
        val correctAnswer = question.correctAnswer?.toBoolean() ?: false
        val isCorrect = selectedAnswer == correctAnswer
        
        // Disable both buttons
        trueButton.isEnabled = false
        falseButton.isEnabled = false
        
        // Color the buttons based on the answer
        if (isCorrect) {
            if (selectedAnswer) {
                trueButton.setBackgroundColor(0xFF4CAF50.toInt()) // Green for correct
            } else {
                falseButton.setBackgroundColor(0xFF4CAF50.toInt()) // Green for correct
            }
        } else {
            if (selectedAnswer) {
                trueButton.setBackgroundColor(0xFFF44336.toInt()) // Red for incorrect
                falseButton.setBackgroundColor(0xFF4CAF50.toInt()) // Green for correct answer
            } else {
                falseButton.setBackgroundColor(0xFFF44336.toInt()) // Red for incorrect
                trueButton.setBackgroundColor(0xFF4CAF50.toInt()) // Green for correct answer
            }
        }

        // Show feedback
        feedbackText.visibility = View.VISIBLE
        feedbackText.text = if (isCorrect) "Correct!" else "Incorrect. The correct answer was: ${if (correctAnswer) "True" else "False"}"
        feedbackText.setTextColor(if (isCorrect) 0xFF4CAF50.toInt() else 0xFFF44336.toInt())
        
        // Show next button
        nextButton.visibility = View.VISIBLE
    }

    companion object {
        fun newInstance(question: Question): TrueFalseFragment {
            return TrueFalseFragment().apply {
                arguments = Bundle().apply {
                    putParcelable(ARG_QUESTION, question)
                }
            }
        }
    }
} 