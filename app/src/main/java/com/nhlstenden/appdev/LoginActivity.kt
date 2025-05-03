package com.nhlstenden.appdev

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import android.content.Intent
import android.util.Base64 
import android.util.Log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import retrofit2.HttpException
import java.io.IOException

class LoginActivity : AppCompatActivity() {

    private val apiService = RetrofitClient.instance

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_login)

        val emailEditText = findViewById<EditText>(R.id.editTextEmailAddress)
        val passwordEditText = findViewById<EditText>(R.id.editTextPassword)
        val loginButton = findViewById<Button>(R.id.buttonLogin)
        val registerButton = findViewById<Button>(R.id.buttonRegister)

        loginButton.setOnClickListener {
            val email = emailEditText.text.toString().trim()
            val password = passwordEditText.text.toString()

            if (email.isNotEmpty() && password.isNotEmpty()) {
                loginButton.isEnabled = false // Disable button

                // Create Basic Auth header
                val credentials = "$email:$password"
                val basicAuthHeader = "Basic " + Base64.encodeToString(credentials.toByteArray(), Base64.NO_WRAP)

                CoroutineScope(Dispatchers.IO).launch {
                    try {
                        // Call the login endpoint with the auth header
                        val response = apiService.loginUser(basicAuthHeader)

                        withContext(Dispatchers.Main) {
                            if (response.isSuccessful) {
                                // Login successful (backend authenticated the user via header)
                                // You might want to store user info/token if the backend returns it
                                // For now, just navigate to MainActivity
                                Toast.makeText(this@LoginActivity, "Login Successful!", Toast.LENGTH_SHORT).show()
                                navigateToMain()
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
                            loginButton.isEnabled = true // Re-enable button
                        }
                    }
                }

            } else {
                Toast.makeText(this, "Please enter email and password", Toast.LENGTH_SHORT).show()
            }
        }

        registerButton.setOnClickListener {
            val intent = Intent(this, RegisterActivity::class.java)
            startActivity(intent)
        }
    }

    private fun navigateToMain() {
        val intent = Intent(this, MainActivity::class.java)
        startActivity(intent)
        finish() // Close LoginActivity so user can't go back to it
    }
} 