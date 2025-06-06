package com.nhlstenden.appdev.features.task.fragments

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import androidx.fragment.app.Fragment
import com.nhlstenden.appdev.R
import com.nhlstenden.appdev.features.task.models.Question

class EditTextFragment : BaseTaskFragment() {
    private lateinit var questionText: TextView
    private lateinit var answerInput: EditText
    private lateinit var submitButton: Button
    private lateinit var nextButton: Button
    private lateinit var feedbackText: TextView

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_edit_text, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupViews(view)
        bindQuestion()
    }

    override fun setupViews(view: View) {
        questionText = view.findViewById(R.id.questionText)
        answerInput = view.findViewById(R.id.answerInput)
        submitButton = view.findViewById(R.id.submitButton)
        nextButton = view.findViewById(R.id.nextButton)
        feedbackText = view.findViewById(R.id.feedbackText)

        nextButton.visibility = View.GONE
        feedbackText.visibility = View.GONE

        submitButton.setOnClickListener {
            val userAnswer = answerInput.text.toString()
            val isCorrect = userAnswer.equals(question.correctText, ignoreCase = true)
            
            // Show feedback
            feedbackText.visibility = View.VISIBLE
            feedbackText.text = if (isCorrect) "Correct!" else "Incorrect. The correct answer was: ${question.correctText}"
            feedbackText.setTextColor(if (isCorrect) 0xFF4CAF50.toInt() else 0xFFF44336.toInt())
            
            // Disable input and submit button
            answerInput.isEnabled = false
            submitButton.visibility = View.GONE
            nextButton.visibility = View.VISIBLE
        }

        nextButton.setOnClickListener {
            val userAnswer = answerInput.text.toString()
            val isCorrect = userAnswer.equals(question.correctText, ignoreCase = true)
            onTaskComplete(isCorrect)
        }
    }

    override fun bindQuestion() {
        questionText.text = question.text
        answerInput.hint = question.correctText ?: "Enter your answer"
        answerInput.setText("")
        answerInput.isEnabled = true
        submitButton.visibility = View.VISIBLE
        nextButton.visibility = View.GONE
        feedbackText.visibility = View.GONE
    }

    companion object {
        private const val ARG_QUESTION = "arg_question"

        fun newInstance(question: Question): EditTextFragment {
            return EditTextFragment().apply {
                arguments = Bundle().apply {
                    putParcelable(ARG_QUESTION, question)
                }
            }
        }
    }
} 