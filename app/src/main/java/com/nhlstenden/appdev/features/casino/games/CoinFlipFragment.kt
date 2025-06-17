package com.nhlstenden.appdev.features.casino.games

import com.nhlstenden.appdev.R
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import androidx.fragment.app.Fragment
import kotlin.random.Random

class CoinFlipFragment : Fragment() {
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

        return view
    }

    private val frameRunnable = object : Runnable {
        override fun run() {
            if (iterations >= maxIterations) {
                if (frames[currentFrame] == R.drawable.coin_side) {
                    frameDuration = Random.nextLong(2500, 5000)
                } else {
                    coinflipCoin.setImageResource(frames[currentFrame])
//TODO: award points
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