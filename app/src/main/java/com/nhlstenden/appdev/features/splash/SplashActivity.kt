package com.nhlstenden.appdev.features.splash

import android.content.Context
import android.content.Intent
import android.hardware.biometrics.BiometricManager.Authenticators.BIOMETRIC_STRONG
import androidx.biometric.BiometricPrompt
import androidx.core.content.ContextCompat
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.biometric.BiometricManager
import androidx.lifecycle.lifecycleScope
import com.nhlstenden.appdev.R
import com.nhlstenden.appdev.MainActivity
import com.nhlstenden.appdev.core.repositories.AuthRepository
import com.nhlstenden.appdev.core.repositories.UserRepository
import com.nhlstenden.appdev.features.login.screens.LoginActivity
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import javax.crypto.Cipher
import javax.inject.Inject
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine

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
                        if (!biometricLogin()) {
                            Log.d("SplashActivity", "Biometrics required, but biometrics failed/ are unavailable. Going to login screen")
                            navigateToLoginActivity()

                            return@launch
                        }
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

    private suspend fun biometricLogin(): Boolean = suspendCoroutine { continuation ->
        val executor = ContextCompat.getMainExecutor(this)
        val biometricPrompt = BiometricPrompt(this, executor,
            object : BiometricPrompt.AuthenticationCallback() {
                override fun onAuthenticationError(errorCode: Int, errString: CharSequence) {
                    super.onAuthenticationError(errorCode, errString)
                    Log.d("SplashActivity", "Failed to login with biometrics: $errString with code: $errorCode")
                    Toast.makeText(applicationContext, "Unable to authenticate with biometrics: $errString", Toast.LENGTH_LONG).show()
                    continuation.resume(false)
                }

                override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                    Log.d("SplashActivity", "Successfully logged in with biometrics")
                    super.onAuthenticationSucceeded(result)
                    continuation.resume(true)
                }

                override fun onAuthenticationFailed() {
                    super.onAuthenticationFailed()
//                    This triggers whenever the user fails, but the user should try again so we fail over on onAuthenticationError
//                    So this remains unused
                    Log.d("SplashActivity", "Biometric authentication failed")
                }
            })

        val promptInfo = if (Build.VERSION.SDK_INT < Build.VERSION_CODES.R) {
            // If on the wrong version, skip biometric auth because it's not available
            continuation.resume(false)

            return@suspendCoroutine
        } else {
            Log.d("SplashActivity", "Attempting biometric login")
            BiometricPrompt.PromptInfo.Builder()
                .setTitle("Biometric login for my app")
                .setSubtitle("Log in using your biometric credential")
                .setNegativeButtonText("Cancel")
                .setAllowedAuthenticators(BiometricManager.Authenticators.BIOMETRIC_STRONG)
                .build()
        }

        biometricPrompt.authenticate(promptInfo)
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