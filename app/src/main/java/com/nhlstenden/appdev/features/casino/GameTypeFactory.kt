package com.nhlstenden.appdev.features.casino

import com.nhlstenden.appdev.features.casino.calculators.CoinFlipCalculator
import com.nhlstenden.appdev.features.casino.calculators.HorseRaceCalculator
import com.nhlstenden.appdev.features.casino.calculators.WheelOfFortuneCalculator
import com.nhlstenden.appdev.features.casino.games.CoinFlipGame
import com.nhlstenden.appdev.features.casino.games.HorseRacesGame
import com.nhlstenden.appdev.features.casino.games.WheelOfFortuneGame
import com.nhlstenden.appdev.features.casino.interfaces.GameType

object GameTypeFactory {
    fun createGameType(game: CasinoTypes?): GameType {
        return when (game) {
            CasinoTypes.COINFLIP -> CoinFlipGame(CoinFlipCalculator())
            CasinoTypes.HORSE_RACES -> HorseRacesGame(HorseRaceCalculator())
            CasinoTypes.WHEEL_OF_FORTUNE -> WheelOfFortuneGame(WheelOfFortuneCalculator())
            else -> throw Exception("Casino type $game does not exist")
        }
    }
}