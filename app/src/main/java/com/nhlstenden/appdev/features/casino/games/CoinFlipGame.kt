package com.nhlstenden.appdev.features.casino.games

import androidx.fragment.app.Fragment
import com.nhlstenden.appdev.features.casino.fragments.CoinFlipFragment
import com.nhlstenden.appdev.features.casino.interfaces.GameCallback
import com.nhlstenden.appdev.features.casino.interfaces.ScoreCalculator
import com.nhlstenden.appdev.features.casino.models.GameData

class CoinFlipGame(
    scoreCalculator: ScoreCalculator
) : BaseGame(scoreCalculator), GameCallback {
    override fun createFragment(): Fragment {
        return CoinFlipFragment(this as GameCallback)
    }

    override fun getGameData(): GameData {
        return GameData(
            "Coinflip",
            "Click the coin to flip a coin! if it lands on the happy guinea pig, you get double your points. If it lands on the angry guinea pig, you lose half your points."
        )
    }

    override fun onGameFinished(score: Int, params: Int): Int {
        return scoreCalculator.calculateScore(score, params)
    }
}