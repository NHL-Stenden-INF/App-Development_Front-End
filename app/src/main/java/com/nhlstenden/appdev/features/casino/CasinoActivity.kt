package com.nhlstenden.appdev.features.casino

import android.os.Build
import android.os.Bundle
import android.util.Log
import android.widget.TextView
import android.widget.Toast
import androidx.activity.viewModels
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import com.nhlstenden.appdev.R
import com.nhlstenden.appdev.core.repositories.AuthRepository
import com.nhlstenden.appdev.core.repositories.UserRepository
import com.nhlstenden.appdev.features.casino.GameTypeFactory
import com.nhlstenden.appdev.features.casino.fragments.CoinFlipFragment
import com.nhlstenden.appdev.features.casino.fragments.HorseRaceFragment
import com.nhlstenden.appdev.features.casino.fragments.WheelOfFortuneFragment
import com.nhlstenden.appdev.features.casino.models.GameData
import com.nhlstenden.appdev.features.casino.viewmodels.CasinoViewmodel
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class CasinoActivity : AppCompatActivity() {
    private val viewModel: CasinoViewmodel by viewModels()

    @Inject
    lateinit var userRepository: UserRepository

    @Inject
    lateinit var authRepository: AuthRepository

    val gameTypeFactory = GameTypeFactory

    @RequiresApi(Build.VERSION_CODES.TIRAMISU)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_casino)

        val title = findViewById<TextView>(R.id.Title)
        val subtitle = findViewById<TextView>(R.id.Subtitle)

        val game = intent.extras?.getSerializable("game", CasinoTypes::class.java)
        Log.d("CasinoActivity", "Started the casino for game: ${game.toString()}")

        val gameData = gameTypeFactory.createGameType(game)
        title.text = String.format(title.text.toString(), gameData.getGameData().titleText)
        subtitle.text = String.format(subtitle.text.toString(), gameData.getGameData().subtitleText)
        val fragment = gameData.createFragment()

        val points = intent.getIntExtra("points", 100)
        viewModel.setGamePoints(points)
        Log.d("CasinoActivity", viewModel.gamePoint.value.toString())

        viewModel.gamePoint.observe(this) {
            if (!viewModel.isGameDone.value!!) {
                return@observe
            }
            awardPoints((viewModel.gamePoint.value ?: 0))
            Log.d("CasinoActivity", "Collected points: ${viewModel.gamePoint.value}")
        }

        supportFragmentManager.beginTransaction()
            .replace(R.id.fragment_container, fragment)
            .commit()
    }

    private fun awardPoints(points: Int) {
        Toast.makeText(applicationContext!!, "You've been given $points points", Toast.LENGTH_LONG).show()
        CoroutineScope(Dispatchers.IO).launch {
            val currentUser = authRepository.getCurrentUserSync()
            val profile = userRepository.getUserAttributes(currentUser?.id.toString()).getOrNull()
            userRepository.updateUserPoints(currentUser?.id.toString(), profile?.optInt("points", 0)!! + points)
        }
    }
}