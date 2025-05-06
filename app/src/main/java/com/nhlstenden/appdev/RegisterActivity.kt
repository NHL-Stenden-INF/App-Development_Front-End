package com.nhlstenden.appdev

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import android.content.Intent
import android.util.Log // For logging errors
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import retrofit2.HttpException
import java.io.IOException

class RegisterActivity : AppCompatActivity() {

    private val apiService = RetrofitClient.instance // Get API service instance

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_register)

        val usernameEditText = findViewById<EditText>(R.id.editTextUsername)
        val emailEditText = findViewById<EditText>(R.id.editTextEmailAddressRegister)
        val passwordEditText = findViewById<EditText>(R.id.editTextPasswordRegister)
        val registerButton = findViewById<Button>(R.id.buttonRegister)
        val backToLoginButton = findViewById<Button>(R.id.buttonBackToLogin) // Get the new button

        registerButton.setOnClickListener {
            val username = usernameEditText.text.toString().trim()
            val email = emailEditText.text.toString().trim()
            val password = passwordEditText.text.toString() // No trim for password

            if (username.isNotEmpty() && email.isNotEmpty() && password.isNotEmpty()) {
                registerButton.isEnabled = false // Disable button during request
                // Launch a coroutine in the IO dispatcher for network call
                CoroutineScope(Dispatchers.IO).launch {
                    try {
                        val registerRequest = RegisterRequest(username, email, password)
                        val response = apiService.registerUser(registerRequest)

                        // Switch back to the Main thread to update UI
                        withContext(Dispatchers.Main) {
                            if (response.isSuccessful) {
                                val successMessage = response.body() ?: "Registration successful!"
                                Toast.makeText(this@RegisterActivity, successMessage, Toast.LENGTH_LONG).show()
                                navigateToLogin()
                            } else {
                                // Handle unsuccessful response (e.g., 4xx, 5xx)
                                val errorBody = response.errorBody()?.string() ?: "Unknown registration error"
                                Log.e("RegisterActivity", "Registration failed: ${response.code()} - $errorBody")
                                Toast.makeText(this@RegisterActivity, "Registration failed: $errorBody", Toast.LENGTH_LONG).show()
                            }
                        }
                    } catch (e: IOException) {
                        // Handle network errors (e.g., no internet)
                        Log.e("RegisterActivity", "Network error during registration", e)
                        withContext(Dispatchers.Main) {
                            Toast.makeText(this@RegisterActivity, "Network error. Please check connection.", Toast.LENGTH_LONG).show()
                        }
                    } catch (e: HttpException) {
                        // Handle HTTP errors (non-2xx responses)
                        Log.e("RegisterActivity", "HTTP error during registration", e)
                        withContext(Dispatchers.Main) {
                            Toast.makeText(this@RegisterActivity, "Server error: ${e.message()}", Toast.LENGTH_LONG).show()
                        }
                    } catch (e: Exception) {
                        // Handle other unexpected errors
                        Log.e("RegisterActivity", "Unexpected error during registration", e)
                        withContext(Dispatchers.Main) {
                            Toast.makeText(this@RegisterActivity, "An unexpected error occurred.", Toast.LENGTH_LONG).show()
                        }
                    } finally {
                        // Ensure button is re-enabled on the Main thread
                        withContext(Dispatchers.Main) {
                            registerButton.isEnabled = true
                        }
                    }
                }
            } else {
                Toast.makeText(this, "Please fill in all fields", Toast.LENGTH_SHORT).show()
            }
        }

        backToLoginButton.setOnClickListener { // Add listener for the back button
            finish() // Simply close this activity to go back to LoginActivity
        }
    }

    private fun navigateToLogin() {
        val intent = Intent(this, LoginActivity::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_CLEAR_TOP // Clear activity stack above LoginActivity
        startActivity(intent)
        finish() // Close RegisterActivity
    }
} 