package com.nhlstenden.appdev.features.casino.games

import androidx.fragment.app.Fragment
import com.nhlstenden.appdev.features.casino.fragments.HorseRaceFragment
import com.nhlstenden.appdev.features.casino.interfaces.GameCallback
import com.nhlstenden.appdev.features.casino.interfaces.ScoreCalculator
import com.nhlstenden.appdev.features.casino.models.GameData

class HorseRacesGame(
    scoreCalculator: ScoreCalculator
) : BaseGame(scoreCalculator), GameCallback {
    override fun createFragment(): Fragment {
        return HorseRaceFragment()
    }

    override fun getGameData(): GameData {
        return GameData(
            "Horse Races",
            "Click on the Guinea-Horse you think is going to win! If your horse wins, you get 3x the amount of points you put in. But if you pick wrong, you lose 2/3 points."
        )
    }

    override fun onGameFinished(score: Int, params: Int): Int {
        return scoreCalculator.calculateScore(score, params)
    }
}