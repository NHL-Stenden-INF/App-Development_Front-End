package com.nhlstenden.appdev.features.casino.calculators

import com.nhlstenden.appdev.features.casino.interfaces.ScoreCalculator

class CoinFlipCalculator: ScoreCalculator {
    override fun calculateScore(score: Int, additionalParams: Int?): Int {
        if (additionalParams == null) {
            return score
        }
        val hasWonGame: Boolean = (score == 1)
        return if (hasWonGame) {
            score * 2
        } else {
            score / 2
        }
    }
}