package com.nhlstenden.appdev.features.casino.fragments

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
import com.nhlstenden.appdev.features.casino.interfaces.GameCallback
import kotlin.random.Random

class WheelOfFortuneFragment(
    gameCallback: GameCallback
) : BaseGameFragment(gameCallback) {
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
            spinButton.setOnClickListener(null)
            spinButton.visibility = View.GONE
            startGame()
        }

        return view
    }

    override fun startGame() {
        handler.post(frameRunnable)
    }

    private val frameRunnable = object : Runnable {
        override fun run() {
            var newRotation = maxRotations - currentRotation
            if (newRotation <= 5.0) {
                val rewardedPoints = gameCallback.onGameFinished(viewModel.gamePoint.value!!, currentRotation.toInt())

                Log.d("WheelOfFortuneFragment", "Awarded points: $rewardedPoints")

                return finishGame(rewardedPoints)
            }
            wheelOfFortune.rotation = (currentRotation % 360).toFloat()

            currentRotation += newRotation * 0.01
            Log.d("WheelOfFortuneFragment", "$currentRotation")
            handler.post(this)
        }
    }
}