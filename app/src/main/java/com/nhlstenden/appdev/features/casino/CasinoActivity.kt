package com.nhlstenden.appdev.features.casino

import android.os.Build
import android.os.Bundle
import android.util.Log
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import com.nhlstenden.appdev.R
import com.nhlstenden.appdev.core.repositories.AuthRepository
import com.nhlstenden.appdev.core.repositories.UserRepository
import com.nhlstenden.appdev.features.casino.games.CoinFlipFragment
import com.nhlstenden.appdev.features.casino.games.PlinkoFragment
import com.nhlstenden.appdev.features.casino.games.WheelOfFortuneFragment
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class CasinoActivity : AppCompatActivity() {
    private var amountToGamble = 0

    @Inject
    lateinit var userRepository: UserRepository

    @Inject
    lateinit var authRepository: AuthRepository

    @RequiresApi(Build.VERSION_CODES.TIRAMISU)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_casino)

        val game = intent.extras?.getSerializable("game", CasinoTypes::class.java)
        Log.d("CasinoActivity", "Started the casino for game: ${game.toString()}")
        val fragment: Fragment = when (game) {
            CasinoTypes.COINFLIP -> CoinFlipFragment()
            CasinoTypes.PLINKO -> PlinkoFragment()
            CasinoTypes.WHEEL_OF_FORTUNE -> WheelOfFortuneFragment()
            else -> throw Exception("Casino type $game does not exist")
        }
        amountToGamble = intent.getIntExtra("points", 100)

        supportFragmentManager.beginTransaction()
            .replace(R.id.fragment_container, fragment)
            .commit()
    }

//    TODO: Implement this properly
    private fun awardPoints(points: Int) {
        CoroutineScope(Dispatchers.IO).launch {
            val currentUser = authRepository.getCurrentUserSync()
            val profile = userRepository.getUserAttributes(currentUser?.id.toString()).getOrNull()
            userRepository.updateUserPoints(currentUser?.id.toString(), profile?.optInt("points", 0)!! + points)
        }
    }
}