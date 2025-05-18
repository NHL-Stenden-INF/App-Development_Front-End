package com.nhlstenden.appdev

import android.graphics.Color
import android.graphics.PorterDuff
import android.os.Bundle
import android.view.Gravity
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.core.content.ContextCompat

class HomeFragment : Fragment() {

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        return inflater.inflate(R.layout.fragment_home, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        dayCounter(view)
    }

    private fun dayCounter(view: View) {
        val days = listOf("Mon", "Tue", "Wed", "Thu", "Fri", "Sat", "Sun")
        val completedDays = setOf(0, 1, 2, 3, 4)

        val container = view.findViewById<LinearLayout>(R.id.daysContainer)
        container.removeAllViews()

        for ((index, day) in days.withIndex()) {
            val dayLayout = LinearLayout(requireContext()).apply {
                orientation = LinearLayout.VERTICAL
                gravity = Gravity.CENTER
                layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
            }

            // Draw the circle
            val circle = FrameLayout(requireContext()).apply {
                layoutParams = FrameLayout.LayoutParams(64, 64).apply {
                    gravity = Gravity.CENTER
                }

                background = ContextCompat.getDrawable(
                    requireContext(),
                    if (completedDays.contains(index)) R.drawable.day_circle_active else R.drawable.day_circle_inactive
                )
            }

            // Draw the checkmark
            val check = ImageView(requireContext()).apply {
                layoutParams = FrameLayout.LayoutParams(32, 32, Gravity.CENTER)
                setImageResource(R.drawable.ic_check)
                visibility = if (completedDays.contains(index)) View.VISIBLE else View.INVISIBLE
                setColorFilter(Color.WHITE, PorterDuff.Mode.SRC_IN)
            }

            val label = TextView(requireContext()).apply {
                text = day
                setTextColor(Color.BLACK)
                textSize = 16f
                setPadding(0, 8, 0, 0)
                gravity = Gravity.CENTER
            }

            circle.addView(check)
            dayLayout.addView(circle)
            dayLayout.addView(label)
            container.addView(dayLayout)
        }
    }
}