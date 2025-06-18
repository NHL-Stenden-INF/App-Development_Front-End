package com.nhlstenden.appdev.features.casino.games

interface RaceManager {
    fun onRaceCompleted(guineaHorseName: String)
    fun getFinishline(): Float
}