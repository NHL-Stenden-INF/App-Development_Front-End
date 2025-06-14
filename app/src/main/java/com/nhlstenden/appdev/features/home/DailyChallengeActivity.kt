package com.nhlstenden.appdev.features.home

import android.app.Activity
import android.os.Bundle
import android.util.Log
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import com.nhlstenden.appdev.R
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.FragmentActivity

data class DailyChallenge(
    val title: String,
    val problemText: String,
    val buggedCode: String,
    val correctedCode: String,
)

class DailyChallengeActivity : AppCompatActivity() {
    private lateinit var submitButton: Button
    private lateinit var undoButton: Button
    private lateinit var bugreportTextField: EditText
    private lateinit var title: TextView
    private lateinit var subtitle: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.fragment_daily_challenge)

        submitButton = findViewById<Button>(R.id.submitButton)
        undoButton = findViewById<Button>(R.id.undoButton)
        bugreportTextField = findViewById<EditText>(R.id.BugReport)
        title = findViewById<TextView>(R.id.Title)
        subtitle = findViewById<TextView>(R.id.Subtitle)

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

        setText(dailyChallenge)

        submitButton.setOnClickListener {
            val isSuccessful = checkAnswer(dailyChallenge)
            DailyChallengeCompletedDialog(isSuccessful).show(supportFragmentManager, "daily_challenge_completed")
        }

        undoButton.setOnClickListener {
            setText(dailyChallenge)
        }
    }

    private fun setText(dailyChallenge: DailyChallenge) {
        title.text = title.text.toString().format(dailyChallenge.title)
        subtitle.text = subtitle.text.toString().format(dailyChallenge.problemText)
        bugreportTextField.setText(dailyChallenge.buggedCode)
    }

    private fun checkAnswer(dailyChallenge: DailyChallenge): Boolean {
        return bugreportTextField.text.replace("\\s".toRegex(), "") == dailyChallenge.correctedCode.replace("\\s".toRegex(), "")
    }
}