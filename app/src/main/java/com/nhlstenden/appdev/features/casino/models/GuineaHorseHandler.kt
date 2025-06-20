package com.nhlstenden.appdev.features.casino.models

import android.os.Handler
import android.os.Looper
import android.widget.ImageView
import com.nhlstenden.appdev.features.casino.interfaces.GameObject
import com.nhlstenden.appdev.features.casino.interfaces.RaceManager
import kotlin.random.Random

class GuineaHorseHandler(
    private val guineaHorse: ImageView,
    private val guineaHorseName: String,
    private val raceManager: RaceManager
): Runnable, GameObject {
    val handler = Handler(Looper.getMainLooper())

    override fun start() {
        handler.post(this)
    }

    override fun stop() {
        handler.removeCallbacksAndMessages(null)
    }

    override fun run() {
        if (guineaHorse.y >= raceManager.getFinishline()) {
            raceManager.onRaceCompleted(guineaHorseName)
            stop()

            return
        }
        val nextPosition = Random.Default.nextInt(1, 15)
        guineaHorse.y += nextPosition.toFloat()
        handler.postDelayed(this, nextPosition.toLong())
    }
}