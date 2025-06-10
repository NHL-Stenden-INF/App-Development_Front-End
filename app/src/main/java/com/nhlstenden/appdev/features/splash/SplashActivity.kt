package com.nhlstenden.appdev.features.splash

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.nhlstenden.appdev.R
import com.nhlstenden.appdev.MainActivity
import com.nhlstenden.appdev.core.repositories.AuthRepository
import com.nhlstenden.appdev.core.repositories.UserRepository
import com.nhlstenden.appdev.features.login.screens.LoginActivity
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * SplashActivity handles the initial app launch by checking for active user sessions.
 * - If user is logged in with valid JWT: Navigate to MainActivity
 * - If user is not logged in or JWT is expired: Navigate to LoginActivity
 */
@AndroidEntryPoint
class SplashActivity : AppCompatActivity() {
    
    @Inject
    lateinit var authRepository: AuthRepository
    
    @Inject
    lateinit var userRepository: UserRepository
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_splash)
        
        // Start mascot animation
        val logoImageView = findViewById<android.widget.ImageView>(R.id.splash_logo)
        logoImageView.setImageResource(R.drawable.mascot_animation)
        (logoImageView.drawable as? android.graphics.drawable.AnimationDrawable)?.start()
        
        checkSessionAndNavigate()
    }
    
    private fun checkSessionAndNavigate() {
        lifecycleScope.launch {
            // Add a small delay for better user experience (splash screen effect)
            delay(1000) // 1 second
            
            try {
                // Check if user is logged in
                if (authRepository.isLoggedIn()) {
                    val currentUser = authRepository.getCurrentUserSync()
                    if (currentUser != null) {
                        // Validate the JWT by making a test API call
                        android.util.Log.d("SplashActivity", "Validating session for user: ${currentUser.email}")
                        
                        val validationResult = userRepository.getUserAttributes(currentUser.id)
                        if (validationResult.isSuccess) {
                            // JWT is valid, go to MainActivity
                            android.util.Log.d("SplashActivity", "Valid session confirmed for user: ${currentUser.email}")
                            navigateToMainActivity()
                        } else {
                            // JWT validation failed (likely expired), clear session and go to login
                            android.util.Log.w("SplashActivity", "Session validation failed: ${validationResult.exceptionOrNull()?.message}")
                            authRepository.handleJWTExpiration()
                            navigateToLoginActivity()
                        }
                    } else {
                        // Session data corrupted, go to login
                        android.util.Log.w("SplashActivity", "Session data corrupted, redirecting to login")
                        navigateToLoginActivity()
                    }
                } else {
                    // No active session, go to LoginActivity
                    android.util.Log.d("SplashActivity", "No active session found, redirecting to login")
                    navigateToLoginActivity()
                }
            } catch (e: Exception) {
                // Error checking session, go to login for safety
                android.util.Log.e("SplashActivity", "Error checking session", e)
                navigateToLoginActivity()
            }
        }
    }
    
    private fun navigateToMainActivity() {
        val intent = Intent(this, MainActivity::class.java)
        // Clear the back stack so user can't go back to splash
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        startActivity(intent)
        finish()
    }
    
    private fun navigateToLoginActivity() {
        val intent = Intent(this, LoginActivity::class.java)
        // Clear the back stack so user can't go back to splash
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        startActivity(intent)
        finish()
    }
} 