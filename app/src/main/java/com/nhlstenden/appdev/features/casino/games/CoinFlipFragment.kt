package com.nhlstenden.appdev.features.casino.games

import com.nhlstenden.appdev.R
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import com.nhlstenden.appdev.features.casino.viewmodels.CasinoViewmodel
import dagger.hilt.android.AndroidEntryPoint
import kotlin.getValue
import kotlin.random.Random

@AndroidEntryPoint
class CoinFlipFragment : Fragment() {
    private val viewModel: CasinoViewmodel by viewModels()

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

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_casino_coin_flip, container, false)
        coinflipCoin = view.findViewById<ImageButton>(R.id.coinButton)
        coinflipCoin.setOnClickListener {
            startAnimation()
        }
        Log.d("CoinFlipFragment", "ViewModel instance: $viewModel")
        return view
    }

    private val frameRunnable = object : Runnable {
        override fun run() {
            if (iterations >= maxIterations) {
                if (frames[currentFrame] == R.drawable.coin_side) {
                    frameDuration = Random.nextLong(2500, 5000)
                } else {
                    coinflipCoin.setImageResource(frames[currentFrame])
                    val hasWonTheGame = frames[currentFrame] == R.drawable.coin_head

                    Toast.makeText(context, "You've ${if (hasWonTheGame) "won" else "lost"} the game!", Toast.LENGTH_LONG).show()

                    val gamePointValue = viewModel.gamePoint.value
                    Log.d("CoinFlipFragment", "gamePoint value: $gamePointValue")

                    val rewardedPoints: Int = if (hasWonTheGame) {
                        viewModel.gamePoint.value!! * 2
                    } else {
                        viewModel.gamePoint.value!! / 2
                    }
                    viewModel.setGamePoints(rewardedPoints)

                    return
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

    private fun startAnimation() {
        handler.postDelayed(frameRunnable, frameDuration)
    }
}