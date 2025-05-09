package com.nhlstenden.appdev

import android.animation.AnimatorInflater
import android.content.Intent
import android.os.Bundle
import android.view.GestureDetector
import android.view.MotionEvent
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.GestureDetectorCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import com.nhlstenden.appdev.databinding.ActivityLoginBinding
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import android.util.Base64 
import android.util.Log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import retrofit2.HttpException
import java.io.IOException

class LoginActivity : AppCompatActivity() {
    private lateinit var binding: ActivityLoginBinding
    private lateinit var gestureDetector: GestureDetectorCompat
    private val apiService = RetrofitClient.instance

    private inner class SwipeGestureListener : GestureDetector.SimpleOnGestureListener() {
        override fun onFling(
            e1: MotionEvent,
            e2: MotionEvent,
            velocityX: Float,
            velocityY: Float
        ): Boolean {
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
            return true
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityLoginBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Set up gesture detector
        gestureDetector = GestureDetectorCompat(this, SwipeGestureListener())

        // Start arrow animation
        binding.imageViewArrow.let { arrow ->
            val animator = AnimatorInflater.loadAnimator(this, R.animator.arrow_animation)
            animator.setTarget(arrow)
            animator.start()
        }

        // Set up click listeners
        binding.buttonLogin.setOnClickListener {
            val email = binding.editTextEmailAddress.text.toString().trim()
            val password = binding.editTextPassword.text.toString()

            if (email.isNotEmpty() && password.isNotEmpty()) {
                binding.buttonLogin.isEnabled = false // Disable button

                // Create Basic Auth header
                val credentials = "$email:$password"
                val basicAuthHeader = "Basic " + Base64.encodeToString(credentials.toByteArray(), Base64.NO_WRAP)

                CoroutineScope(Dispatchers.IO).launch {
                    try {
                        // Call the login endpoint with the auth header
                        val response = apiService.login(basicAuthHeader)

                        withContext(Dispatchers.Main) {
                            if (response.isSuccessful) {
                                // Login successful (backend authenticated the user via header)
                                // You might want to store user info/token if the backend returns it
                                // For now, just navigate to MainActivity
                                Toast.makeText(this@LoginActivity, "Login Successful!", Toast.LENGTH_SHORT).show()
                                navigateToMain(response.body())
                            } else {
                                // Handle unsuccessful response (e.g., 401 Unauthorized)
                                val errorMsg = if (response.code() == 401) {
                                    "Invalid email or password"
                                } else {
                                    response.errorBody()?.string() ?: "Login failed"
                                }
                                Log.e("LoginActivity", "Login failed: ${response.code()} - $errorMsg")
                                Toast.makeText(this@LoginActivity, errorMsg, Toast.LENGTH_LONG).show()
                            }
                        }
                    } catch (e: IOException) {
                        Log.e("LoginActivity", "Network error during login", e)
                        withContext(Dispatchers.Main) {
                            Toast.makeText(this@LoginActivity, "Network error. Please check connection.", Toast.LENGTH_LONG).show()
                        }
                    } catch (e: HttpException) {
                        Log.e("LoginActivity", "HTTP error during login", e)
                        withContext(Dispatchers.Main) {
                             val errorMsg = if (e.code() == 401) {
                                "Invalid email or password"
                            } else {
                                "Server error: ${e.message()}"
                            }
                            Toast.makeText(this@LoginActivity, errorMsg, Toast.LENGTH_LONG).show()
                        }
                    } catch (e: Exception) {
                        Log.e("LoginActivity", "Unexpected error during login", e)
                        withContext(Dispatchers.Main) {
                            Toast.makeText(this@LoginActivity, "An unexpected error occurred.", Toast.LENGTH_LONG).show()
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

        binding.buttonRegister.setOnClickListener {
            startActivity(Intent(this, RegisterActivity::class.java))
            overridePendingTransition(R.anim.slide_in_right, R.anim.slide_out_left)
        }
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        return gestureDetector.onTouchEvent(event) || super.onTouchEvent(event)
    }

    private fun navigateToMain(loggedInUser: UserResponse?) {
        val intent = Intent(this, MainActivity::class.java)
        intent.putExtra("USER_DATA", loggedInUser)
        startActivity(intent)
        finish() // Close LoginActivity so user can't go back to it
    }
} 