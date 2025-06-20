package com.nhlstenden.appdev.features.casino.games

import androidx.fragment.app.Fragment
import com.nhlstenden.appdev.features.casino.fragments.WheelOfFortuneFragment
import com.nhlstenden.appdev.features.casino.interfaces.GameCallback
import com.nhlstenden.appdev.features.casino.interfaces.ScoreCalculator
import com.nhlstenden.appdev.features.casino.models.GameData

class WheelOfFortuneGame(
    scoreCalculator: ScoreCalculator
) : BaseGame(scoreCalculator), GameCallback {
    override fun createFragment(): Fragment {
        return WheelOfFortuneFragment(this as GameCallback)
    }

    override fun getGameData(): GameData {
        return GameData(
            "Wheel of Fortune",
            "Spin the wheel to get your points! The bigger the pile, the bigger the win. But watch out for the Evil Guinea Pig, he'll leave you with none left for yourself!"
        )
    }

    override fun onGameFinished(score: Int, params: Int): Int {
        return scoreCalculator.calculateScore(score, params)
    }
}