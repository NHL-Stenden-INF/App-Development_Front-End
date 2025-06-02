package com.nhlstenden.appdev.features.task.fragments

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import com.google.android.material.button.MaterialButton
import com.nhlstenden.appdev.R
import com.nhlstenden.appdev.features.task.models.Question
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class PressMistakesFragment : BaseTaskFragment() {
    private lateinit var questionText: TextView
    private lateinit var submitButton: MaterialButton
    private var mistakesCount = 0

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_press_mistakes, container, false)
    }

    override fun setupViews(view: View) {
        questionText = view.findViewById(R.id.questionText)
        submitButton = view.findViewById(R.id.submitButton)

        submitButton.setOnClickListener {
            mistakesCount++
            val isCorrect = mistakesCount == (question.mistakes ?: 0)
            onTaskComplete(isCorrect)
        }
    }

    override fun bindQuestion() {
        questionText.text = question.text
        mistakesCount = 0
    }

    companion object {
        private const val ARG_QUESTION = "question"

        fun newInstance(question: Question): PressMistakesFragment {
            return PressMistakesFragment().apply {
                arguments = Bundle().apply {
                    putParcelable(ARG_QUESTION, question)
                }
            }
        }
    }
} 