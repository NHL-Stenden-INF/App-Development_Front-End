package com.nhlstenden.appdev.features.casino.interfaces

interface GameCallback {
    fun onGameFinished(score: Int, params: Int): Int
}