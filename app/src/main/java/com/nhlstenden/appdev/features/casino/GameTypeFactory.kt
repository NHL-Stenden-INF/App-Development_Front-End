package com.nhlstenden.appdev.features.casino

import com.nhlstenden.appdev.features.casino.interfaces.GameType

object GameTypeFactory {
    fun createGameType(game: CasinoTypes): GameType {
        return when (game) {
            CasinoTypes.COINFLIP -> CoinFlipGame()
            CasinoTypes.HORSE_RACES -> HorseRacesGame()
            CasinoTypes.WHEEL_OF_FORTUNE -> WheelOfFortuneGame()
            else -> throw Exception("Casino type $game does not exist")
        }
    }
}