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
    private val maxRotations: Double = Random.nextDouble(360.0 * 2, 360.0 * 3)
    private var isRotating = false

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_casino_wheel_of_fortune, container, false)

        wheelOfFortune = view.findViewById<ImageView>(R.id.wheelOfFortune)
        spinButton = view.findViewById<Button>(R.id.spinButton)

        spinButton.setOnClickListener {
            isRotating = true
            startAnimation()
        }

        return view
    }

    private val frameRunnable = object : Runnable {
        override fun run() {
            if (!isRotating) {
                return
            }
            var newRotation = maxRotations - currentRotation
            if (newRotation <= 1.0) {
                Log.d("WheelOfFortuneFragment", "Wheel stopped: $maxRotations")

                return
            }
            wheelOfFortune.rotation = (currentRotation % 360).toFloat()

            currentRotation += newRotation * 0.01
            Log.d("WheelOfFortuneFragment", "$currentRotation")
            handler.postDelayed(this, 1)
        }
    }

    override fun onPause() {
        super.onPause()
        isRotating = false
    }

    private fun startAnimation() {
        handler.post(frameRunnable)
    }
}