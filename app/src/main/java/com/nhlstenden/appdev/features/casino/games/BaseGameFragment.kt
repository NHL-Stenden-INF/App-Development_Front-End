package com.nhlstenden.appdev.features.casino.games

import android.util.Log
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import com.nhlstenden.appdev.features.casino.viewmodels.CasinoViewmodel
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import kotlin.getValue

abstract class BaseGameFragment : Fragment() {
    protected val viewModel: CasinoViewmodel by viewModels({ requireActivity() })

    protected fun finishGame(points: Int) {
        Log.d("BaseGame", "Points won: $points")
        viewModel.setIsGameDone(true)
        viewModel.setGamePoints(points)

        runBlocking {
            delay(1000)
            activity?.finish()
        }
    }
}