package com.nhlstenden.appdev.features.casino.calculators

import com.nhlstenden.appdev.features.casino.interfaces.ScoreCalculator

class WheelOfFortuneCalculator: ScoreCalculator {
    override fun calculateScore(score: Int, additionalParams: Int?): Int {
        if (additionalParams == null) {
            return score
        }
        return (score * when (additionalParams % 360) {
            in 0..45 -> 5.0
            in 45..90 -> 1.0
            in 90..135 -> 2.0
            in 135..180 -> 0.5
            in 180..225 -> 0.0
            in 225..270 -> 1.0
            in 270..315 -> 2.0
            in 315..360 -> 0.5
            else -> 1.0
        }).toInt()
    }
}