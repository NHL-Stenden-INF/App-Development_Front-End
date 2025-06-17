package com.nhlstenden.appdev.features.casino

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.nhlstenden.appdev.R
import com.nhlstenden.appdev.features.casino.games.CoinFlipFragment
import com.nhlstenden.appdev.features.casino.games.PlinkoFragment
import com.nhlstenden.appdev.features.casino.games.WheelOfFortuneFragment

class CasinoActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_casino)

        val game = intent.getStringExtra("game")
        val fragment = when (game) {
            "conflip" -> CoinFlipFragment()
            "plinko" -> PlinkoFragment()
            "wheel_of_fortune" -> WheelOfFortuneFragment()
            else -> throw Exception("Casino type $game does not exist")
        }

        supportFragmentManager.beginTransaction()
            .replace(R.id.fragment_container, fragment)
            .commit()
    }
}