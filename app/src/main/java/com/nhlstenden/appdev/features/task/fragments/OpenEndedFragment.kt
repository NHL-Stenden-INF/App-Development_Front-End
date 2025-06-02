package com.nhlstenden.appdev.features.task.fragments

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import com.google.android.material.button.MaterialButton
import com.nhlstenden.appdev.R
import com.nhlstenden.appdev.features.task.models.Question
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class OpenEndedFragment : BaseTaskFragment() {
    private lateinit var answerInput: EditText
    private lateinit var submitButton: MaterialButton

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

        submitButton.setOnClickListener {
            val answer = answerInput.text.toString()
            if (answer.isNotEmpty()) {
                val isCorrect = answer.equals(question.correctAnswer, ignoreCase = true)
                onTaskComplete(isCorrect)
            }
        }
    }

    override fun bindQuestion() {
        answerInput.setText("")
        answerInput.hint = question.text
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