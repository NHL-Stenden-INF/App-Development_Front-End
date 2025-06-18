package com.nhlstenden.appdev.features.casino.games

import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ImageView
import com.nhlstenden.appdev.R
import kotlin.random.Random

class WheelOfFortuneFragment : BaseGameFragment() {
    private lateinit var wheelOfFortune: ImageView
    private lateinit var spinButton: Button
    private val handler = Handler(Looper.getMainLooper())
    private var currentRotation: Double = 1.0
    private val maxRotations: Double = Random.nextDouble(320.0 * 5, 360.0 * 8)

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_casino_wheel_of_fortune, container, false)

        wheelOfFortune = view.findViewById<ImageView>(R.id.wheelOfFortune)
        spinButton = view.findViewById<Button>(R.id.spinButton)

        spinButton.setOnClickListener {
            startAnimation()
        }

        return view
    }

    private val frameRunnable = object : Runnable {
        override fun run() {
            var newRotation = maxRotations - currentRotation
            if (newRotation <= 5.0) {
                val rewardedPoints: Int = (viewModel.gamePoint.value!! * calculateMultiplier(currentRotation)).toInt()

                Log.d("WheelOfFortuneFragment", "Awarded points: $rewardedPoints")

                return finishGame(rewardedPoints)
            }
            wheelOfFortune.rotation = (currentRotation % 360).toFloat()

            currentRotation += newRotation * 0.01
            Log.d("WheelOfFortuneFragment", "$currentRotation")
            handler.post(this)
        }
    }

    private fun calculateMultiplier(rotation: Double): Double {
        return when (rotation % 360) {
            in 0.0..45.0 -> 5.0
            in 45.1..90.0 -> 1.0
            in 90.1..135.0 -> 2.0
            in 135.1..180.0 -> 0.5
            in 180.1..225.0 -> 0.0
            in 225.1..270.0 -> 1.0
            in 270.1..315.0 -> 2.0
            in 315.1..360.0 -> 0.5
            else -> 1.0
        }
    }

    private fun startAnimation() {
        handler.post(frameRunnable)
    }
}