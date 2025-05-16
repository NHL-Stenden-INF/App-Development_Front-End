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
import okhttp3.Dispatcher
import org.json.JSONException
import org.json.JSONObject
import retrofit2.HttpException
import java.io.IOException
import java.lang.RuntimeException

class LoginActivity : AppCompatActivity() {
    private lateinit var binding: ActivityLoginBinding
    private lateinit var gestureDetector: GestureDetectorCompat
    private val supabaseClient = SupabaseClient()

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
                            Toast.makeText(this@LoginActivity, JSONObject(e.toString()).getString("msg"), Toast.LENGTH_LONG).show()
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

        // binding.buttonRegister.setOnClickListener {
        //     startActivity(Intent(this, RegisterActivity::class.java))
        //     overridePendingTransition(R.anim.slide_in_right, R.anim.slide_out_left)
        // }
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        return gestureDetector.onTouchEvent(event) || super.onTouchEvent(event)
    }

    private fun navigateToMain(loggedInUser: User?) {
        val intent = Intent(this, MainActivity::class.java)
        intent.putExtra("USER_DATA", loggedInUser)
        startActivity(intent)
        finish() // Close LoginActivity so user can't go back to it
    }
}