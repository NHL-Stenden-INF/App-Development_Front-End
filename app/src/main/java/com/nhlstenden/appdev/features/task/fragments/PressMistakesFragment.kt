package com.nhlstenden.appdev.features.task.fragments

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.nhlstenden.appdev.databinding.FragmentPressMistakesBinding
import com.nhlstenden.appdev.features.task.models.Question
import com.google.android.flexbox.FlexboxLayoutManager
import com.google.android.flexbox.FlexWrap
import com.google.android.flexbox.JustifyContent
import com.nhlstenden.appdev.features.task.adapters.WordAdapter

class PressMistakesFragment : BaseTaskFragment() {
    private var _binding: FragmentPressMistakesBinding? = null
    private val binding get() = _binding!!
    private val pressMistakeQuestion: Question.PressMistakeQuestion
        get() = question as? Question.PressMistakeQuestion
            ?: throw IllegalStateException("Question must be of type PressMistakeQuestion")

    private lateinit var adapter: WordAdapter
    private val selectedPositions = mutableSetOf<Int>()

    override fun onCreate(savedInstanceState: Bundle?){
        super.onCreate(savedInstanceState)
        if (question !is Question.PressMistakeQuestion)
            throw IllegalArgumentException("Question must be of type PressMistakeQuestion")
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentPressMistakesBinding.inflate(inflater, container, false)

        val words = pressMistakeQuestion.displayedText.trim().split(" ")
        adapter = WordAdapter(words, selectedPositions, pressMistakeQuestion.mistakes, getMistakeCount())

        binding.recyclerView.adapter = adapter
        val layoutManager = FlexboxLayoutManager(requireContext()).apply {
            flexWrap = FlexWrap.WRAP
            justifyContent = JustifyContent.FLEX_START
        }
        binding.recyclerView.layoutManager = layoutManager

        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
    }

    override fun setupViews() {
        binding.submitButton.setOnClickListener {
            if (selectedPositions.size == getMistakeCount()) {
                binding.submitButton.visibility = View.GONE
                binding.nextButton.visibility = View.VISIBLE

                adapter.showAnswer(isAnswerCorrect())
            }
        }

        binding.nextButton.setOnClickListener {
            this.onTaskComplete(isAnswerCorrect())
        }
    }

    override fun bindQuestion() {
        binding.mistakesCounter.text = "Amount of mistakes in text: " + this.getMistakeCount().toString()
        binding.submitButton.visibility = View.VISIBLE
        binding.nextButton.visibility = View.GONE

        adapter.reset()
    }

    fun isAnswerCorrect(): Boolean {
        return pressMistakeQuestion.mistakes == selectedPositions
    }

    fun getMistakeCount(): Int {
        return pressMistakeQuestion.mistakes.size
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    companion object {
        fun newInstance(question: Question): PressMistakesFragment {
            return PressMistakesFragment().apply {
                arguments = Bundle().apply {
                    putParcelable(ARG_QUESTION, question)
                }
            }
        }
    }
}