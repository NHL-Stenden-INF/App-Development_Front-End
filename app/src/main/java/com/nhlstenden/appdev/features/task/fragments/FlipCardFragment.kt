package com.nhlstenden.appdev.features.task.fragments

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.nhlstenden.appdev.databinding.FragmentFlipCardBinding
import com.nhlstenden.appdev.features.task.models.Question

class FlipCardFragment : BaseQuestionFragment() {
    private var _binding: FragmentFlipCardBinding? = null
    private val binding get() = _binding!!
    private var isShowingFront = true

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentFlipCardBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupQuestion()
    }

    override fun setupQuestion() {
        question.front?.let { front ->
            question.back?.let { back ->
                binding.cardView.apply {
                    setOnClickListener {
                        flipCard(front, back)
                    }
                }
                showFront(front)
            }
        }
    }

    private fun flipCard(front: String, back: String) {
        isShowingFront = !isShowingFront
        if (isShowingFront) {
            showFront(front)
        } else {
            showBack(back)
        }
    }

    private fun showFront(text: String) {
        binding.cardText.text = text
        binding.cardText.rotationY = 0f
    }

    private fun showBack(text: String) {
        binding.cardText.text = text
        binding.cardText.rotationY = 180f
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    companion object {
        private const val ARG_QUESTION = "question"

        fun newInstance(question: Question) = FlipCardFragment().apply {
            arguments = Bundle().apply {
                putParcelable(ARG_QUESTION, question)
            }
        }
    }
} 