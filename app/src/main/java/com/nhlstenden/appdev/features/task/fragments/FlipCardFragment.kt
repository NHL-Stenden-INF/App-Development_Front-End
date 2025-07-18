package com.nhlstenden.appdev.features.task.fragments

import android.annotation.SuppressLint
import android.os.Bundle
import android.view.GestureDetector
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import com.nhlstenden.appdev.databinding.FragmentFlipCardBinding
import com.nhlstenden.appdev.features.task.models.Question
import kotlin.math.abs

class FlipCardFragment : BaseTaskFragment() {
    private var _binding: FragmentFlipCardBinding? = null
    private val binding get() = _binding!!
    private val flipCardQuestion: Question.FlipCardQuestion
        get() = question as? Question.FlipCardQuestion
            ?: throw IllegalStateException("Question must be of type FlipCardQuestion")

    private lateinit var gestureDetector: GestureDetector

    private var isShowingFront = true

    private val gestureListener = object : GestureDetector.SimpleOnGestureListener() {
        private val SWIPE_THRESHOLD = 100
        private val SWIPE_VELOCITY_THRESHOLD = 100

        override fun onSingleTapUp(e: MotionEvent): Boolean {
            flipCard()
            return true
        }

        override fun onFling(e1: MotionEvent?, e2: MotionEvent, velocityX: Float, velocityY: Float): Boolean {
            if (e1 == null)
                return false

            val diffX = e2.x - e1.x
            val diffY = e2.y - e1.y

            return if (abs(diffX) > abs(diffY)) {
                if (abs(diffX) > SWIPE_THRESHOLD && abs(velocityX) > SWIPE_VELOCITY_THRESHOLD) {
                    if (diffX > 0) {
                        onSwipeRight()
                    } else {
                        onSwipeLeft()
                    }
                    true
                } else {
                    false
                }
            } else {
                false
            }
        }
    }

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
        gestureDetector = GestureDetector(requireContext(), gestureListener)
    }

    @SuppressLint("ClickableViewAccessibility")
    override fun setupViews() {
        binding.cardView.setOnTouchListener { v, event ->
            gestureDetector.onTouchEvent(event)
            true
        }
    }

    override fun bindQuestion() {
        binding.cardText.text = flipCardQuestion.front
        isShowingFront = true
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
        val rotation = 90f

        binding.cardView.animate()
            .rotationYBy(rotation)
            .setDuration(300)
            .withEndAction {
                binding.cardText.text = text
                binding.cardView.rotationY = -rotation
                binding.cardView.animate()
                    .rotationYBy(rotation)
                    .setDuration(150)
                    .start()
            }
            .start()
    }

    private fun onSwipeRight() {
        binding.cardView.animate()
            .translationXBy(1000f)
            .setDuration(150)
            .withEndAction {
                this.onTaskComplete(true)
            }
            .start()
    }

    private fun onSwipeLeft() {
        binding.cardView.animate()
            .translationXBy(-1000f)
            .setDuration(150)
            .withEndAction {
                this.onTaskComplete(false)
            }
            .start()
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