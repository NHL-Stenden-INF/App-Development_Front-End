package com.nhlstenden.appdev.task.ui.fragments

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import com.google.android.material.button.MaterialButton
import com.nhlstenden.appdev.R
import com.nhlstenden.appdev.task.domain.models.Question

class TrueFalseFragment : BaseTaskFragment() {
    private lateinit var questionText: TextView
    private lateinit var trueButton: MaterialButton
    private lateinit var falseButton: MaterialButton

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

        trueButton.setOnClickListener {
            checkAnswer(true)
        }

        falseButton.setOnClickListener {
            checkAnswer(false)
        }
    }

    override fun bindQuestion() {
        questionText.text = question.text
    }

    private fun checkAnswer(selectedAnswer: Boolean) {
        val correctAnswer = question.correctAnswer?.toBoolean() ?: false
        taskCompleteListener?.onTaskComplete(selectedAnswer == correctAnswer)
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