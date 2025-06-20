package com.nhlstenden.appdev.features.casino.interfaces

interface ScoreCalculator {
    fun calculateScore(betPoints: Int, additionalParams: Int? = null): Int
}