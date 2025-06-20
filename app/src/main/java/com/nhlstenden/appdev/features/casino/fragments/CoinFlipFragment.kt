package com.nhlstenden.appdev.features.casino.fragments

import com.nhlstenden.appdev.R
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.Toast
import dagger.hilt.android.AndroidEntryPoint
import kotlin.random.Random

@AndroidEntryPoint
class CoinFlipFragment : BaseGameFragment() {
    private lateinit var coinflipCoin: ImageButton
    private val frames = listOf(
        R.drawable.coin_head,
        R.drawable.coin_side,
        R.drawable.coin_tail,
        R.drawable.coin_side
    )
    private var currentFrame = 0
    private var frameDuration = 5L
    private val handler = Handler(Looper.getMainLooper())
    private var iterations = 0
    private val maxIterations = Random.nextInt(25, 35)

    var hasWonTheGame: Boolean = false

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_casino_coin_flip, container, false)
        coinflipCoin = view.findViewById<ImageButton>(R.id.coinButton)
        coinflipCoin.setOnClickListener {
            startGame()
            coinflipCoin.setOnClickListener(null)
        }

        return view
    }

    private val frameRunnable = object : Runnable {
        override fun run() {
            if (iterations >= maxIterations) {
                if (frames[currentFrame] == R.drawable.coin_side) {
                    frameDuration = Random.nextLong(2500, 5000)
                } else {
                    coinflipCoin.setImageResource(frames[currentFrame])
                    hasWonTheGame = frames[currentFrame] == R.drawable.coin_head

                    Toast.makeText(context, "You've ${if (hasWonTheGame) "won" else "lost"} the game!", Toast.LENGTH_SHORT).show()

                    val rewardedPoints: Int = calculateScore(viewModel.gamePoint.value!!)
                    return finishGame(rewardedPoints)
                }
            }
            coinflipCoin.setImageResource(frames[currentFrame])
            currentFrame = (currentFrame + 1) % frames.size

            if (frameDuration <= 300) {
                frameDuration += Random.nextInt(5, 10)
            }
            iterations++
            handler.postDelayed(this, frameDuration)
        }
    }

    override fun startGame() {
        handler.postDelayed(frameRunnable, frameDuration)
    }
}