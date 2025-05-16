package com.nhlstenden.appdev

import android.animation.AnimatorInflater
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.GestureDetector
import android.view.MotionEvent
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.GestureDetectorCompat
import com.nhlstenden.appdev.LoginActivity
import com.nhlstenden.appdev.databinding.ActivityRegisterBinding
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONException
import org.json.JSONObject
import retrofit2.HttpException
import java.io.IOException

class RegisterActivity : AppCompatActivity() {

    private lateinit var binding: ActivityRegisterBinding
    private lateinit var gestureDetector: GestureDetectorCompat
    private val supabaseClient = SupabaseClient()

    private inner class SwipeGestureListener : GestureDetector.SimpleOnGestureListener() {
        override fun onFling(e1: MotionEvent, e2: MotionEvent, velocityX: Float, velocityY: Float): Boolean {
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
            return true
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityRegisterBinding.inflate(layoutInflater)
        setContentView(binding.root)

        gestureDetector = GestureDetectorCompat(this, SwipeGestureListener())

        binding.imageViewArrowLogin.let { arrow ->
            val animator = AnimatorInflater.loadAnimator(this, R.animator.arrow_animation_right)
            animator.setTarget(arrow)
            animator.start()
        }

        binding.buttonRegister.setOnClickListener {
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

        // binding.buttonBackToLogin.setOnClickListener {
        //     val intent = Intent(this, LoginActivity::class.java)
        //     startActivity(intent)
        //     overridePendingTransition(R.anim.slide_in_left, R.anim.slide_out_right)
        //     finish()
        // }
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        return gestureDetector.onTouchEvent(event) || super.onTouchEvent(event)
    }
}