package com.nhlstenden.appdev.features.casino.fragments

import android.os.Handler
import android.os.Looper
import android.util.Log
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import com.nhlstenden.appdev.features.casino.interfaces.GameType
import com.nhlstenden.appdev.features.casino.interfaces.ScoreCalculator
import com.nhlstenden.appdev.features.casino.viewmodels.CasinoViewmodel
import kotlin.getValue

abstract class BaseGameFragment : Fragment() {
    protected val viewModel: CasinoViewmodel by viewModels({ requireActivity() })

    protected fun finishGame(points: Int) {
        Log.d("BaseGame", "Points won: $points")
        viewModel.setIsGameDone(true)
        viewModel.setGamePoints(points)

        val handler = Handler(Looper.getMainLooper())
        val runnable = object: Runnable {
            override fun run() {
                activity?.finish()
            }
        }
        handler.postDelayed(runnable, 1000)
    }

    abstract fun startGame()
}