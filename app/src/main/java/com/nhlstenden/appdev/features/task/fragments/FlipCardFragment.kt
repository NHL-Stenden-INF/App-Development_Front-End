package com.nhlstenden.appdev.features.task.fragments

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.nhlstenden.appdev.databinding.FragmentFlipCardBinding
import com.nhlstenden.appdev.features.task.models.Question

class FlipCardFragment : BaseTaskFragment() {
    private var _binding: FragmentFlipCardBinding? = null
    private val binding get() = _binding!!
    private val flipCardQuestion: Question.FlipCardQuestion
        get() = question as? Question.FlipCardQuestion
            ?: throw IllegalStateException("Question must be of type FlipCardQuestion")

    private var isShowingFront = true
    private var rotation = 0f

    override fun onCreate(savedInstanceState: Bundle?){
        super.onCreate(savedInstanceState)
        if (question !is Question.FlipCardQuestion)
            throw IllegalArgumentException("Question must be of type FlipCardQuestion")
    }

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
    }

    override fun setupViews() {
        binding.cardView.setOnClickListener { flipCard() }
    }

    override fun bindQuestion() {
        binding.cardText.text = flipCardQuestion.front
    }

    private fun flipCard() {
        isShowingFront = !isShowingFront
        if (isShowingFront) {
            showSide(flipCardQuestion.front)
        } else {
            showSide(flipCardQuestion.back)
        }
    }

    private fun showSide(text: String) {
        binding.cardText.text = text
        rotation += 180f
        binding.cardText.rotationY = rotation
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    companion object {
        fun newInstance(question: Question): FlipCardFragment {
            return FlipCardFragment().apply {
                arguments = Bundle().apply {
                    putParcelable(ARG_QUESTION, question)
                }
            }
        }
    }
} 