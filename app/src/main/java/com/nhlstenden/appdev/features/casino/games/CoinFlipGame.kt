package com.nhlstenden.appdev.features.casino.games

import androidx.fragment.app.Fragment
import com.nhlstenden.appdev.features.casino.models.GameData

class CoinFlipGame: BaseGame() {
    override fun createFragment(): Fragment {
        TODO("Not yet implemented")
    }

    override fun getGameData(): GameData {
        return GameData(
            "Coinflip",
            "Click the coin to flip a coin! if it lands on the happy guinea pig, you get double your points. If it lands on the angry guinea pig, you lose half your points."
        )
    }

    override fun calculateScore(score: Int): Int {
        return if (hasWonTheGame) {
            (score  * 2).toInt()
        } else {
            (score / 2).toInt()
        }
    }
}