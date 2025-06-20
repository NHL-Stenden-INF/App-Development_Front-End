package com.nhlstenden.appdev.features.casino.interfaces

interface RaceManager {
    fun onRaceCompleted(guineaHorseName: String)
    fun getFinishline(): Float
}