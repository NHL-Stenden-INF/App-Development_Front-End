package com.nhlstenden.appdev.features.casino.interfaces

import androidx.fragment.app.Fragment
import com.nhlstenden.appdev.features.casino.models.GameData

interface GameType {
    fun createFragment(): Fragment
    fun getGameData(): GameData
    fun calculateScore(score: Int): Int
}