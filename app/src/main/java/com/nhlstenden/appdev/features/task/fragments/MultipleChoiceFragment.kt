package com.nhlstenden.appdev.features.task.fragments

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.google.android.material.button.MaterialButton
import com.nhlstenden.appdev.databinding.FragmentMultipleChoiceBinding
import com.nhlstenden.appdev.features.task.models.MultipleChoiceOption
import com.nhlstenden.appdev.features.task.models.Question
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MultipleChoiceFragment : BaseTaskFragment() {

    private var _binding: FragmentMultipleChoiceBinding? = null
    private val binding get() = _binding!!
    // Enforces the multipleChoiceQuestion type onto this variable based off of the question variable
    private val multipleChoiceQuestion: Question.MultipleChoiceQuestion
        get() = question as? Question.MultipleChoiceQuestion
            ?: throw IllegalStateException("Question must be of type MultipleChoiceQuestion")

    private var optionButtons: List<MaterialButton> = emptyList()
    private var shuffledOptions: List<MultipleChoiceOption> = emptyList()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (question !is Question.MultipleChoiceQuestion)
            throw IllegalArgumentException("Question of type MultipleChoiceQuestion is required")
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentMultipleChoiceBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
    }

    override fun setupViews() {
        binding.nextButton.visibility = View.GONE
        binding.feedbackText.visibility = View.GONE

        binding.optionsGroup.removeAllViews()

        optionButtons = List(4) { i ->
            MaterialButton(requireContext()).apply {
                layoutParams = ViewGroup.MarginLayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT
                ).apply { setMargins(0, 8, 0, 8) }

                isCheckable = true
                isClickable = true

                setOnClickListener {
                    if (optionButtons.any { it.isEnabled }) {
                        optionButtons.forEach { btn ->
                            btn.isChecked = false
                            btn.setBackgroundColor(0xFFEEEEEE.toInt())
                        }
                        this.isChecked = true
                        this.setBackgroundColor(0xFF2196F3.toInt())
                    }
                }
            }.also { binding.optionsGroup.addView(it) }
        }

        binding.submitButton.setOnClickListener {
            val selectedIndex = optionButtons.indexOfFirst { it.isChecked }
            if (selectedIndex != -1) {
                val selectedOption = shuffledOptions[selectedIndex]
                val isCorrect = selectedOption.isCorrect

                optionButtons.forEachIndexed { i, btn ->
                    btn.isEnabled = false
                    if (i == selectedIndex) {
                        btn.setBackgroundColor(
                            if (isCorrect) 0xFF4CAF50.toInt() else 0xFFF44336.toInt()
                        )
                        btn.setTextColor(0xFFFFFFFF.toInt())
                    } else if (shuffledOptions[i].isCorrect) {
                        btn.setBackgroundColor(0xFF4CAF50.toInt())
                        btn.setTextColor(0xFFFFFFFF.toInt())
                    } else {
                        btn.setTextColor(0xFF757575.toInt())
                    }
                }

                binding.feedbackText.visibility = View.VISIBLE
                binding.feedbackText.text =
                    if (isCorrect) "Correct!" else "Incorrect. The correct answer was: ${shuffledOptions.find { it.isCorrect }?.text}"
                binding.feedbackText.setTextColor(
                    if (isCorrect) 0xFF4CAF50.toInt() else 0xFFF44336.toInt()
                )

                binding.submitButton.visibility = View.GONE
                binding.nextButton.visibility = View.VISIBLE
            }
        }

        binding.nextButton.setOnClickListener {
            val selectedIndex = optionButtons.indexOfFirst { it.isChecked }
            if (selectedIndex != -1) {
                val selectedOption = shuffledOptions[selectedIndex]
                this.onTaskComplete(selectedOption.isCorrect)
            }
        }
    }

    override fun bindQuestion() {
        binding.questionText.text = multipleChoiceQuestion.question
        shuffledOptions = multipleChoiceQuestion.options.shuffled()

        optionButtons.forEach { btn ->
            btn.isEnabled = true
            btn.isChecked = false
            btn.setBackgroundColor(0xFFEEEEEE.toInt())
            btn.setTextColor(0xFF000000.toInt())
        }

        optionButtons.forEachIndexed { i, btn ->
            btn.text = shuffledOptions[i].text
        }

        binding.submitButton.visibility = View.VISIBLE
        binding.nextButton.visibility = View.GONE
        binding.feedbackText.visibility = View.GONE
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
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
