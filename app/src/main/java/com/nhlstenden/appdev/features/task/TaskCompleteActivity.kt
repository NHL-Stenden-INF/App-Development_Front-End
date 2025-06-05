package com.nhlstenden.appdev.features.task

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.nhlstenden.appdev.R
import com.nhlstenden.appdev.features.courses.model.Task
import com.nhlstenden.appdev.main.MainActivity

class TaskCompleteActivity : AppCompatActivity() {
    private lateinit var returnButton: Button
    private lateinit var taskName: TextView
    private lateinit var pointsEarnedText: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContentView(R.layout.activity_task_complete)

        returnButton = findViewById(R.id.returnButton)
        taskName = findViewById(R.id.taskNameText)
        pointsEarnedText = findViewById(R.id.pointsEarnedText)

        returnButton.setOnClickListener {
            val intent = Intent(this, MainActivity::class.java)
            startActivity(intent)
            finish()
        }

        val task = intent.getParcelableExtra("TASK_DATA", Task::class.java)
        taskName.text = task?.title
        
        // Get and display points earned
        val pointsEarned = intent.getIntExtra("POINTS_EARNED", 0)
        if (pointsEarned > 0) {
            pointsEarnedText.visibility = View.VISIBLE
            pointsEarnedText.text = "+$pointsEarned points earned!"
        } else {
            pointsEarnedText.visibility = View.GONE
        }
    }
}