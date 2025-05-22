package com.nhlstenden.appdev

import android.animation.AnimatorInflater
import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.GestureDetector
import android.view.MotionEvent
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.GestureDetectorCompat
import com.nhlstenden.appdev.databinding.ActivityLoginBinding
import android.widget.Toast
import android.util.Log
import com.bumptech.glide.Glide
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONException
import org.json.JSONObject
import java.lang.RuntimeException

class LoginActivity : AppCompatActivity() {
    private lateinit var binding: ActivityLoginBinding
    private lateinit var gestureDetector: GestureDetectorCompat
    private val supabaseClient = SupabaseClient()
    private val sleepHandler = Handler(Looper.getMainLooper())
    private var isMascotSleeping = false
    private val SLEEP_DELAY = 60000L // 1 minute in milliseconds

    // Runnable that changes mascot to sleeping state
    private val sleepRunnable = Runnable {
        if (!isFinishing) {
            isMascotSleeping = true
            // Switch to sleeping mascot with crossfade
            Glide.with(this@LoginActivity)
                .asGif()
                .load(R.drawable.mascot_sleep)
                .into(binding.imageViewLogo)
        }
    }

    // Method to reset the sleep timer
    private fun resetSleepTimer() {
        sleepHandler.removeCallbacks(sleepRunnable)
        
        // If mascot was sleeping, wake it up
        if (isMascotSleeping) {
            isMascotSleeping = false
            Glide.with(this)
                .asGif()
                .load(R.drawable.mascot)
                .into(binding.imageViewLogo)
        }
        
        // Start the sleep timer again
        sleepHandler.postDelayed(sleepRunnable, SLEEP_DELAY)
    }

    private inner class SwipeGestureListener : GestureDetector.SimpleOnGestureListener() {
        override fun onFling(
            e1: MotionEvent,
            e2: MotionEvent,
            velocityX: Float,
            velocityY: Float
        ): Boolean {
            resetSleepTimer()
            val diffX = e2.x - e1.x
            val diffY = e2.y - e1.y

            if (Math.abs(diffX) > Math.abs(diffY)) {
                if (Math.abs(diffX) > 100 && Math.abs(velocityX) > 100) {
                    if (diffX < 0) {
                        // Swipe left - go to register
                        startActivity(Intent(this@LoginActivity, RegisterActivity::class.java))
                        overridePendingTransition(R.anim.slide_in_right, R.anim.slide_out_left)
                        return true
                    }
                }
            }
            return false
        }

        override fun onDown(e: MotionEvent): Boolean {
            resetSleepTimer()
            return true
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityLoginBinding.inflate(layoutInflater)
        setContentView(binding.root)
        
        // Simple GIF loading without any transitions or placeholders
        Glide.with(this)
            .asGif()
            .load(R.drawable.mascot)
            .into(binding.imageViewLogo)
            
        // Set up gesture detector
        gestureDetector = GestureDetectorCompat(this, SwipeGestureListener())

        // Start the sleep timer
        resetSleepTimer()

        // Set up touch listener for the entire screen to reset sleep timer
        binding.root.setOnTouchListener { _, event ->
            resetSleepTimer()
            false // Don't consume the event
        }

        // Start arrow animation
        binding.imageViewArrow.let { arrow ->
            val animator = AnimatorInflater.loadAnimator(this, R.animator.arrow_animation)
            animator.setTarget(arrow)
            animator.start()
        }

        // Set up click listeners
        binding.buttonLogin.setOnClickListener {
            resetSleepTimer()
            val email = binding.editTextEmailAddress.text.toString().trim()
            val password = binding.editTextPassword.text.toString()

            if (email.isNotEmpty() && password.isNotEmpty()) {
                binding.buttonLogin.isEnabled = false // Disable button

                CoroutineScope(Dispatchers.IO).launch {
                    try {
                        // Call the login endpoint with the auth header
                        val response = supabaseClient.getUser(email, password)
                        withContext(Dispatchers.Main) {
                            Toast.makeText(this@LoginActivity, "Login Successful!", Toast.LENGTH_SHORT).show()
                            navigateToMain(response)
                        }
                    } catch (e: RuntimeException) {
                        Log.e("LoginActivity", "HTTP error during login: ", e)
                        withContext(Dispatchers.Main) {
                            Toast.makeText(this@LoginActivity, JSONObject(e.message.toString()).getString("msg"), Toast.LENGTH_LONG).show()
                        }
                    } catch (e: JSONException) {
                        Log.e("LoginActivity", "JSON error during login: ", e)
                        withContext(Dispatchers.Main) {
                            Toast.makeText(this@LoginActivity, "Faulty response, unable to login", Toast.LENGTH_LONG).show()
                        }
                    } finally {
                        withContext(Dispatchers.Main) {
                            binding.buttonLogin.isEnabled = true // Re-enable button
                        }
                    }
                }
            } else {
                Toast.makeText(this, "Please enter email and password", Toast.LENGTH_SHORT).show()
            }
        }

        // Add touch listeners to input fields to reset sleep timer
        binding.editTextEmailAddress.setOnTouchListener { _, _ ->
            resetSleepTimer()
            false
        }
        
        binding.editTextPassword.setOnTouchListener { _, _ ->
            resetSleepTimer()
            false
        }
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        resetSleepTimer()
        return gestureDetector.onTouchEvent(event) || super.onTouchEvent(event)
    }

    override fun onPause() {
        super.onPause()
        // Remove any pending sleep timer callbacks when activity is paused
        sleepHandler.removeCallbacks(sleepRunnable)
    }

    override fun onResume() {
        super.onResume()
        // Reset sleep timer when activity is resumed
        resetSleepTimer()
    }

    private fun navigateToMain(loggedInUser: User?) {
        val intent = Intent(this, MainActivity::class.java)
        intent.putExtra("USER_DATA", loggedInUser)
        startActivity(intent)
        finish() // Close LoginActivity so user can't go back to it
    }
}