package com.nhlstenden.appdev.features.casino.games

import com.nhlstenden.appdev.features.casino.interfaces.GameType
import com.nhlstenden.appdev.features.casino.interfaces.ScoreCalculator

abstract class BaseGame(
    protected val scoreCalculator: ScoreCalculator
): GameType {

}