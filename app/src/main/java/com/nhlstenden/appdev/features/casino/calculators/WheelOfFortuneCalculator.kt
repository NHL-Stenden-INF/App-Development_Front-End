package com.nhlstenden.appdev.features.casino.calculators

import com.nhlstenden.appdev.features.casino.interfaces.ScoreCalculator

class WheelOfFortuneCalculator: ScoreCalculator {
    override fun calculateScore(score: Int, additionalParams: Int?): Int {
        if (additionalParams == null) {
            return score
        }
        return (score * when (additionalParams % 360) {
            in 0.0..45.0 -> 5.0
            in 45.1..90.0 -> 1.0
            in 90.1..135.0 -> 2.0
            in 135.1..180.0 -> 0.5
            in 180.1..225.0 -> 0.0
            in 225.1..270.0 -> 1.0
            in 270.1..315.0 -> 2.0
            in 315.1..360.0 -> 0.5
            else -> 1.0
        }).toInt()
    }
}