package com.nhlstenden.appdev.login.ui

import android.animation.AnimatorInflater
import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.GestureDetector
import android.view.MotionEvent
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.GestureDetectorCompat
import com.bumptech.glide.Glide
import com.nhlstenden.appdev.R
import com.nhlstenden.appdev.supabase.SupabaseClient
import com.nhlstenden.appdev.databinding.ActivityRegisterBinding
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONException
import org.json.JSONObject

class RegisterActivity : AppCompatActivity() {

    private lateinit var binding: ActivityRegisterBinding
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
            Glide.with(this@RegisterActivity)
                .asGif()
                .load(R.drawable.mascot_sleep)
                .into(binding.imageViewLogoRegister)
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
                .into(binding.imageViewLogoRegister)
        }
        
        // Start the sleep timer again
        sleepHandler.postDelayed(sleepRunnable, SLEEP_DELAY)
    }

    private inner class SwipeGestureListener : GestureDetector.SimpleOnGestureListener() {
        override fun onFling(e1: MotionEvent, e2: MotionEvent, velocityX: Float, velocityY: Float): Boolean {
            resetSleepTimer()
            val diffX = e2.x - e1.x
            val diffY = e2.y - e1.y

            if (Math.abs(diffX) > Math.abs(diffY)) {
                if (Math.abs(diffX) > 100 && Math.abs(velocityX) > 100) {
                    if (diffX > 0) { // Positive diffX for right swipe
                        // Swipe right - go to LoginActivity
                        val intent = Intent(this@RegisterActivity, LoginActivity::class.java)
                        startActivity(intent)
                        overridePendingTransition(R.anim.slide_in_left, R.anim.slide_out_right)
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
        binding = ActivityRegisterBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Simple GIF loading without any transitions or placeholders
        Glide.with(this)
            .asGif()
            .load(R.drawable.mascot)
            .into(binding.imageViewLogoRegister)

        gestureDetector = GestureDetectorCompat(this, SwipeGestureListener())
        
        // Start the sleep timer
        resetSleepTimer()

        // Set up touch listener for the entire screen to reset sleep timer
        binding.root.setOnTouchListener { _, event ->
            resetSleepTimer()
            false // Don't consume the event
        }

        binding.imageViewArrowLogin.let { arrow ->
            val animator = AnimatorInflater.loadAnimator(this, R.animator.arrow_animation_right)
            animator.setTarget(arrow)
            animator.start()
        }

        binding.buttonRegister.setOnClickListener {
            resetSleepTimer()
            val username = binding.editTextUsername.text.toString().trim()
            val email = binding.editTextEmailAddressRegister.text.toString().trim()
            val password = binding.editTextPasswordRegister.text.toString()

            if (username.isNotEmpty() && email.isNotEmpty() && password.isNotEmpty()) {
                binding.buttonRegister.isEnabled = false

                CoroutineScope(Dispatchers.IO).launch {
                    try {
                        supabaseClient.createNewUser(email, password, username)
                        withContext(Dispatchers.Main) {
                            Toast.makeText(this@RegisterActivity, "Registration successful!", Toast.LENGTH_SHORT).show()
                            val intent = Intent(this@RegisterActivity, LoginActivity::class.java)
                            startActivity(intent)
                            overridePendingTransition(R.anim.slide_in_left, R.anim.slide_out_right)
                            finish()
                        }
                    } catch (e: RuntimeException) {
                        Log.e("RegisterActivity", "HTTP error during login: ", e)
                        withContext(Dispatchers.Main) {
                            Toast.makeText(this@RegisterActivity, JSONObject(e.message.toString()).getString("msg"), Toast.LENGTH_LONG).show()
                        }
                    }  catch (e: JSONException) {
                        Log.e("LoginActivity", "JSON error during login: ", e)
                        withContext(Dispatchers.Main) {
                            Toast.makeText(this@RegisterActivity, "Faulty response, unable to login", Toast.LENGTH_LONG).show()
                        }
                    } finally {
                        withContext(Dispatchers.Main) {
                            binding.buttonRegister.isEnabled = true
                        }
                    }
                }
            } else {
                Toast.makeText(this, "Please fill in all fields", Toast.LENGTH_SHORT).show()
            }
        }

        // Add touch listeners to input fields to reset sleep timer
        binding.editTextUsername.setOnTouchListener { _, _ ->
            resetSleepTimer()
            false
        }
        
        binding.editTextEmailAddressRegister.setOnTouchListener { _, _ ->
            resetSleepTimer()
            false
        }
        
        binding.editTextPasswordRegister.setOnTouchListener { _, _ ->
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
}