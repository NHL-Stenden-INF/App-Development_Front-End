package com.nhlstenden.appdev.features.casino.games

import androidx.fragment.app.Fragment
import com.nhlstenden.appdev.features.casino.fragments.WheelOfFortuneFragment
import com.nhlstenden.appdev.features.casino.models.GameData

class WheelOfFortuneGame: BaseGame() {
    override fun createFragment(): Fragment {
        return WheelOfFortuneFragment()
    }

    override fun getGameData(): GameData {
        return GameData(
            "Wheel of Fortune",
            "Spin the wheel to get your points! The bigger the pile, the bigger the win. But watch out for the Evil Guinea Pig, he'll leave you with none left for yourself!"
        )
    }

    override fun calculateScore(score: Int): Int {
        return (score * when (currentRotation % 360) {
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