package com.nhlstenden.appdev.features.home

import android.app.Activity
import android.os.Bundle
import android.util.Log
import android.widget.Button
import android.widget.EditText
import com.nhlstenden.appdev.R

data class DailyChallenge(
    val title: String,
    val problemText: String,
    val buggedCode: String,
    val correctedCode: String,
)

class DailyChallengeActivity : Activity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.fragment_daily_challenge)

        var submitButton = findViewById<Button>(R.id.submitButton)
        var bugreportTextField = findViewById<EditText>(R.id.bugreport)

        val dailyChallenge = DailyChallenge(
            title = "Off-by-one",
            problemText = "This code should iterate through the array and print each element, but throws an ArrayIndexOutOfBounds exception",
            buggedCode = """
                int[] numbers = {1, 2, 3, 4, 5};
    
                for (int i = 0; i <= numbers.length; i++) {
                    System.out.println(numbers[i]);
                }
            """.trimIndent(),
            correctedCode = """
                int[] numbers = {1, 2, 3, 4, 5};
    
                for (int i = 0; i < numbers.length; i++) {
                    System.out.println(numbers[i]);
                }
            """.trimIndent()
        )

        submitButton.setOnClickListener {
            Log.d("DailyChallengeActivity", "Wrong key: ${checkAnswer(dailyChallenge.correctedCode, dailyChallenge.buggedCode)}")
            Log.d("DailyChallengeActivity", "Correct key: ${checkAnswer(dailyChallenge.correctedCode, dailyChallenge.correctedCode)}")
            Log.d("DailyChallengeActivity", "User input key: ${checkAnswer(dailyChallenge.correctedCode, bugreportTextField.text.toString())}")
        }
        bugreportTextField.setText(dailyChallenge.buggedCode)
    }

    private fun checkAnswer(correctedCode: String, answer: String): Boolean {
        return correctedCode.replace("\\s".toRegex(), "") == answer.replace("\\s".toRegex(), "")
    }
}