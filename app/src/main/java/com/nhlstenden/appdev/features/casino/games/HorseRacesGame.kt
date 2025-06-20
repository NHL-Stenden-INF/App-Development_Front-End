package com.nhlstenden.appdev.features.casino.games

import androidx.fragment.app.Fragment
import com.nhlstenden.appdev.features.casino.models.GameData

class HorseRacesGame: BaseGame() {
    override fun createFragment(): Fragment {
        TODO("Not yet implemented")
    }

    override fun getGameData(): GameData {
        return GameData(
            "Horse Races",
            "Click on the Guinea-Horse you think is going to win! If your horse wins, you get 3x the amount of points you put in. But if you pick wrong, you lose 2/3 points."
        )
    }

    override fun calculateScore(score: Int): Int {
        return if (winnerGuineaHorse == betterGuineaHorse) {
            (score * 3).toInt()
        } else {
            (score / 3).toInt()
        }
    }
}