package com.nhlstenden.appdev.features.casino

import android.os.Build
import android.os.Bundle
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import com.nhlstenden.appdev.R
import com.nhlstenden.appdev.features.casino.games.CoinFlipFragment
import com.nhlstenden.appdev.features.casino.games.PlinkoFragment
import com.nhlstenden.appdev.features.casino.games.WheelOfFortuneFragment

class CasinoActivity : AppCompatActivity() {
    @RequiresApi(Build.VERSION_CODES.TIRAMISU)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_casino)

        val game = intent.extras?.getSerializable("game", CasinoTypes::class.java)
        val fragment: Fragment = when (game) {
            CasinoTypes.COINFLIP -> CoinFlipFragment()
            CasinoTypes.PLINKO -> PlinkoFragment()
            CasinoTypes.WHEEL_OF_FORTUNE -> WheelOfFortuneFragment()
            else -> throw Exception("Casino type $game does not exist")
        }

        supportFragmentManager.beginTransaction()
            .replace(R.id.fragment_container, fragment)
            .commit()
    }
}