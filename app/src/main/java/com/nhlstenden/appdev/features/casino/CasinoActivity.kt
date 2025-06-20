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
import com.nhlstenden.appdev.features.casino.fragments.CoinFlipFragment
import com.nhlstenden.appdev.features.casino.fragments.HorseRaceFragment
import com.nhlstenden.appdev.features.casino.fragments.WheelOfFortuneFragment
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

    @RequiresApi(Build.VERSION_CODES.TIRAMISU)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_casino)

        val title = findViewById<TextView>(R.id.Title)
        val subtitle = findViewById<TextView>(R.id.Subtitle)

        val game = intent.extras?.getSerializable("game", CasinoTypes::class.java)
        lateinit var fragment: Fragment
        Log.d("CasinoActivity", "Started the casino for game: ${game.toString()}")

         when (game) {
            CasinoTypes.COINFLIP -> {
                title.text = String.format(title.text.toString(), "Coinflip")
                subtitle.text = String.format(subtitle.text.toString(), "Click the coin to flip a coin! if it lands on the happy guinea pig, you get double your points. If it lands on the angry guinea pig, you lose half your points.")
                 fragment = CoinFlipFragment()
            }
            CasinoTypes.HORSE_RACES -> {
                title.text = String.format(title.text.toString(), "Horse Races")
                subtitle.text = String.format(subtitle.text.toString(), "Click on the Guinea-Horse you think is going to win! If your horse wins, you get 3x the amount of points you put in. But if you pick wrong, you lose 2/3 points.")
                fragment = HorseRaceFragment()
            }
            CasinoTypes.WHEEL_OF_FORTUNE -> {
                title.text = String.format(title.text.toString(), "Wheel of Fortune")
                subtitle.text = String.format(subtitle.text.toString(), "Spin the wheel to get your points! The bigger the pile, the bigger the win. But watch out for the Evil Guinea Pig, he'll leave you with none left for yourself!")
                fragment = WheelOfFortuneFragment()
            }
            else -> throw Exception("Casino type $game does not exist")
        }

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