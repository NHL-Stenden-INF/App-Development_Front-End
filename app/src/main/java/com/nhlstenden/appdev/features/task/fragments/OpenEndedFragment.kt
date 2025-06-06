package com.nhlstenden.appdev.features.task.fragments

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.TextView
import com.google.android.material.button.MaterialButton
import com.nhlstenden.appdev.R
import com.nhlstenden.appdev.features.task.models.Question
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class OpenEndedFragment : BaseTaskFragment() {
    private lateinit var answerInput: EditText
    private lateinit var submitButton: MaterialButton
    private lateinit var nextButton: MaterialButton
    private lateinit var feedbackText: TextView

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_open_ended, container, false)
    }

    override fun setupViews(view: View) {
        answerInput = view.findViewById(R.id.answerInput)
        submitButton = view.findViewById(R.id.submitButton)
        nextButton = view.findViewById(R.id.nextButton)
        feedbackText = view.findViewById(R.id.feedbackText)

        nextButton.visibility = View.GONE
        feedbackText.visibility = View.GONE

        submitButton.setOnClickListener {
            val answer = answerInput.text.toString()
            if (answer.isNotEmpty()) {
                val isCorrect = answer.equals(question.correctAnswer, ignoreCase = true)
                
                // Show feedback
                feedbackText.visibility = View.VISIBLE
                feedbackText.text = if (isCorrect) "Correct!" else "Incorrect. The correct answer was: ${question.correctAnswer}"
                feedbackText.setTextColor(if (isCorrect) 0xFF4CAF50.toInt() else 0xFFF44336.toInt())
                
                // Disable input and submit button
                answerInput.isEnabled = false
                submitButton.visibility = View.GONE
                nextButton.visibility = View.VISIBLE
            }
        }

        nextButton.setOnClickListener {
            val answer = answerInput.text.toString()
            val isCorrect = answer.equals(question.correctAnswer, ignoreCase = true)
            onTaskComplete(isCorrect)
        }
    }

    override fun bindQuestion() {
        answerInput.setText("")
        answerInput.hint = question.text
        answerInput.isEnabled = true
        submitButton.visibility = View.VISIBLE
        nextButton.visibility = View.GONE
        feedbackText.visibility = View.GONE
    }

    companion object {
        fun newInstance(question: Question): OpenEndedFragment {
            return OpenEndedFragment().apply {
                arguments = Bundle().apply {
                    putParcelable(ARG_QUESTION, question)
                }
            }
        }
    }
} 