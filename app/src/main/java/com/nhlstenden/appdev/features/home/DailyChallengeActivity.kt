package com.nhlstenden.appdev.features.home

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import com.nhlstenden.appdev.R
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContentProviderCompat.requireContext
import com.nhlstenden.appdev.core.repositories.AuthRepository
import com.nhlstenden.appdev.core.repositories.UserRepository
import com.nhlstenden.appdev.features.casino.CasinoActivity
import com.nhlstenden.appdev.features.casino.CasinoTypes
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import javax.inject.Inject

data class DailyChallenge(
    val title: String,
    val description: String,
    val buggedCode: String,
    val correctedCode: String,
)

@AndroidEntryPoint
class DailyChallengeActivity : AppCompatActivity() {
    private lateinit var submitButton: Button
    private lateinit var undoButton: Button
    private lateinit var bugreportTextField: EditText
    private lateinit var title: TextView
    private lateinit var subtitle: TextView

    private lateinit var dailyChallenge: DailyChallenge

    @Inject
    lateinit var authRepository: AuthRepository

    @Inject
    lateinit var userRepository: UserRepository

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_daily_challenge)

        val challengeParser = ChallengeParser(applicationContext)
        dailyChallenge = challengeParser.loadAllChallenges().random()

        submitButton = findViewById<Button>(R.id.submitButton)
        undoButton = findViewById<Button>(R.id.undoButton)
        bugreportTextField = findViewById<EditText>(R.id.BugReport)
        title = findViewById<TextView>(R.id.Title)
        subtitle = findViewById<TextView>(R.id.Subtitle)

        setText()

        submitButton.setOnClickListener {
            val isSuccessful = checkAnswer()
            DailyChallengeCompletedDialog(isSuccessful).show(supportFragmentManager, "daily_challenge_completed")
        }

        undoButton.setOnClickListener {
            setText()
        }
    }

    override fun finish() {
        super.finish()
        val rewardedPoints = 300
        val currentUser = authRepository.getCurrentUserSync()

        if (checkAnswer()) {
            CoroutineScope(Dispatchers.IO).launch {
                val profile = userRepository.getUserAttributes(currentUser?.id.toString()).getOrNull()
                userRepository.updateUserPoints(currentUser?.id.toString(), profile?.optInt("points", 0)!! + rewardedPoints)
            }
            Toast.makeText(applicationContext, "Received $rewardedPoints points for challenge", Toast.LENGTH_LONG).show()
        }
        CoroutineScope(Dispatchers.IO).launch {
//            userRepository.updateUserDailyChallenge(currentUser?.id.toString())
        }
//        TODO: Temporary testing
        val intent = Intent(applicationContext, CasinoActivity::class.java)
        intent.putExtra("game", CasinoTypes.COINFLIP)
        startActivity(intent)
    }

    private fun setText() {
        title.text = title.text.toString().format(dailyChallenge.title)
        subtitle.text = subtitle.text.toString().format(dailyChallenge.description)
        bugreportTextField.setText(dailyChallenge.buggedCode)
    }

    private fun checkAnswer(): Boolean {
        return bugreportTextField.text.replace("\\s".toRegex(), "") == dailyChallenge.correctedCode.replace("\\s".toRegex(), "")
    }
}