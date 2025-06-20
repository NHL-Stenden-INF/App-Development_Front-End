package com.nhlstenden.appdev.features.task.fragments

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.nhlstenden.appdev.databinding.FragmentEditTextBinding
import com.nhlstenden.appdev.features.task.models.Question

class EditTextFragment : BaseTaskFragment() {
    private var _binding: FragmentEditTextBinding? = null
    private val binding get() = _binding!!

    private val editTextQuestion: Question.EditTextQuestion
        get() = question as? Question.EditTextQuestion
            ?: throw IllegalStateException("Question must be of type EditTextQuestion")

    var isCorrect: Boolean? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentEditTextBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun setupViews() {
        binding.submitButton.setOnClickListener {
            binding.submitButton.visibility = View.GONE
            binding.undoButton.visibility = View.GONE
            binding.space.visibility = View.GONE

            binding.nextButton.visibility = View.VISIBLE

            binding.BugReport.isEnabled = false
            binding.BugReport.isFocusable = false

            isCorrect = checkAnswer()
            if (isCorrect == true){
                binding.feedbackText.text = "Correct!"
                binding.feedbackText.setTextColor(0xFF00FF00.toInt())
            } else {
                binding.feedbackText.text = "Incorrect..."
                binding.feedbackText.setTextColor(0xFFFF0000.toInt())
                binding.BugReport.setText(editTextQuestion.correctText)
            }
        }

        binding.undoButton.setOnClickListener {
            binding.BugReport.setText(editTextQuestion.displayedText)
        }

        binding.nextButton.setOnClickListener {
            this.onTaskComplete(checkAnswer())
        }
    }

    override fun bindQuestion() {
        binding.submitButton.visibility = View.VISIBLE
        binding.undoButton.visibility = View.VISIBLE
        binding.space.visibility = View.VISIBLE

        binding.nextButton.visibility = View.GONE

        binding.questionText.text = editTextQuestion.question
        binding.BugReport.setText(editTextQuestion.displayedText)
        binding.BugReport.isEnabled = true
        binding.BugReport.isFocusable = true

        binding.feedbackText.text = ""
    }

    private fun checkAnswer(): Boolean {
        return binding.BugReport.text.replace("\\s".toRegex(), "") == editTextQuestion.correctText.replace("\\s".toRegex(), "")
    }

    companion object {
        fun newInstance(question: Question): EditTextFragment {
            return EditTextFragment().apply {
                arguments = Bundle().apply {
                    putParcelable(ARG_QUESTION, question)
                }
            }
        }
    }
}