package com.nhlstenden.appdev

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.widget.TextView

class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        val user: UserResponse? = intent.getParcelableExtra("USER_DATA", UserResponse::class.java)

        user?.let {
            findViewById<TextView>(R.id.Usernameview).text = """username: ${it.username}"""
            findViewById<TextView>(R.id.Emailview).text = """email: ${it.email}"""
            findViewById<TextView>(R.id.Pointsview).text = """points: ${it.points.toString()}"""
            findViewById<TextView>(R.id.Useridview).text = """User ID: ${it.id.toString()}"""
        }
    }
}