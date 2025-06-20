package com.nhlstenden.appdev.features.splash

import android.content.Intent
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
import com.nhlstenden.appdev.core.repositories.ProfileRepository
import com.nhlstenden.appdev.core.repositories.SettingsRepository
import com.nhlstenden.appdev.features.login.screens.LoginActivity
import com.nhlstenden.appdev.features.rewards.AchievementManager
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import javax.inject.Inject
import org.json.JSONObject
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine
import java.time.LocalDate
import java.time.temporal.ChronoUnit

/**
 * SplashActivity handles the initial app launch by checking for active user sessions.
 * - If user is logged in with valid JWT: Navigate to MainActivity
 * - If user is not logged in or JWT is expired: Navigate to LoginActivity
 */
@AndroidEntryPoint
class SplashActivity : AppCompatActivity() {
    @Inject
    lateinit var settingsRepository: SettingsRepository
    
    @Inject
    lateinit var authRepository: AuthRepository
    
    @Inject
    lateinit var userRepository: UserRepository

    @Inject
    lateinit var profileRepository: ProfileRepository
    
    @Inject
    lateinit var achievementManager: AchievementManager

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
                        val userHasBiometricsEnabled = true
                        if (settingsRepository.hasValue("biometric_enabled") && !biometricLogin()) {
                            Log.d("SplashActivity", "Biometrics required, but biometrics failed/ are unavailable. Going to login screen")
                            navigateToLoginActivity()

                            return@launch
                        }
                        // Validate the JWT by making a test API call
                        android.util.Log.d("SplashActivity", "Validating session for user: ${currentUser.email}")
                        
                        val validationResult = userRepository.getUserAttributes(currentUser.id)
                        if (validationResult.isSuccess) {
                            // Update streak if needed when app starts with existing session
                            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                                updateStreakIfNeeded(currentUser)
                            }
                            
                            // Check existing achievements (both course completion and streak)
                            checkExistingAchievements(currentUser)
                            
                            // Fetch full profile for header
                            val profileResult = profileRepository.getProfile()
                            if (profileResult.isSuccess) {
                                val profile = profileResult.getOrNull()!!
                                // Convert Profile to JSONObject for caching
                                val profileJson = JSONObject().apply {
                                    put("display_name", profile.displayName)
                                    put("email", profile.email)
                                    put("bio", profile.bio)
                                    put("profile_picture", profile.profilePicture)
                                    put("level", profile.level)
                                    put("xp", profile.experience)
                                    put("bell_peppers", profile.bellPeppers)
                                }
                                userRepository.cachedProfile = profileJson
                                android.util.Log.d("SplashActivity", "Cached profile set: $profileJson")
                                android.util.Log.d("SplashActivity", "Cached profile keys: ${profileJson.keys().asSequence().toList()}")
                                android.util.Log.d("SplashActivity", "Cached profile display_name: ${profileJson.optString("display_name")}")
                                android.util.Log.d("SplashActivity", "Cached profile profile_picture: ${profileJson.optString("profile_picture")}")
                            } else {
                                android.util.Log.w("SplashActivity", "Failed to fetch full profile: ${profileResult.exceptionOrNull()?.message}")
                            }
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
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.R) {
            Log.d("SplashActivity", "Biometric authentication not supported with this API version")
            Toast.makeText(applicationContext, "Biometric authentication not supported", Toast.LENGTH_LONG).show()
            continuation.resume(false)

            return@suspendCoroutine
        }

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
                    super.onAuthenticationSucceeded(result)
                    Log.d("SplashActivity", "Successfully logged in with biometrics")
                    continuation.resume(true)
                }

                override fun onAuthenticationFailed() {
                    super.onAuthenticationFailed()
//                    This triggers whenever the user fails, but the user should try again so we fail over on onAuthenticationError
//                    So this remains unused
                    Log.d("SplashActivity", "Biometric authentication failed")
                }
            })

        val promptInfo = BiometricPrompt.PromptInfo.Builder()
            .setTitle("Login with fingerprint")
            .setSubtitle("Log into GitGud using your fingerprint scanner")
            .setNegativeButtonText("Cancel")
            .setAllowedAuthenticators(BiometricManager.Authenticators.BIOMETRIC_STRONG)
            .build()

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

    @androidx.annotation.RequiresApi(Build.VERSION_CODES.O)
    private suspend fun updateStreakIfNeeded(user: com.nhlstenden.appdev.core.models.User) {
        try {
            Log.d("SplashActivity", "🎯 === SPLASH STREAK CHECK for user: ${user.id} ===")
            
            val attributesResult = userRepository.getUserAttributes(user.id)
            if (attributesResult.isSuccess) {
                val attributes = attributesResult.getOrThrow()
                
                val currentStreak = attributes.optInt("streak", 0)
                val lastTaskDateStr = attributes.optString("last_task_date", "")
                Log.d("SplashActivity", "🎯 Current streak from DB: $currentStreak")
                Log.d("SplashActivity", "🎯 Last task date from DB: $lastTaskDateStr")
                
                val lastTaskDate = if (lastTaskDateStr.isNotEmpty() && lastTaskDateStr != "null") {
                    try {
                        LocalDate.parse(lastTaskDateStr)
                    } catch (e: Exception) {
                        Log.e("SplashActivity", "🎯 Error parsing last task date: ${e.message}")
                        null
                    }
                } else null

                val today = LocalDate.now()
                Log.d("SplashActivity", "🎯 Today's date: $today")
                
                val newStreak = if (lastTaskDate != null) {
                    val daysBetween = ChronoUnit.DAYS.between(lastTaskDate, today)
                    Log.d("SplashActivity", "🎯 Days between last task and today: $daysBetween")
                    
                    when {
                        daysBetween == 0L -> {
                            Log.d("SplashActivity", "🎯 Same day as last task, keeping streak at $currentStreak")
                            currentStreak
                        }
                        daysBetween == 1L -> {
                            Log.d("SplashActivity", "🎯 Next day after last task, keeping streak at $currentStreak")
                            currentStreak
                        }
                        else -> {
                            Log.d("SplashActivity", "🎯 More than one day has passed ($daysBetween days), resetting streak from $currentStreak to 0")
                            0
                        }
                    }
                } else {
                    Log.d("SplashActivity", "🎯 No last task date found, starting with streak 0")
                    0
                }

                // Update streak in database if it changed
                if (newStreak != currentStreak) {
                    Log.d("SplashActivity", "🎯 Updating streak in database from $currentStreak to $newStreak")
                    userRepository.updateUserStreak(user.id, newStreak)
                } else {
                    Log.d("SplashActivity", "🎯 No streak update needed, keeping streak at $currentStreak")
                }
            }
        } catch (e: Exception) {
            Log.e("SplashActivity", "🎯 Error updating streak", e)
        }
    }
    
    private fun checkExistingAchievements(user: com.nhlstenden.appdev.core.models.User) {
        try {
            Log.d("SplashActivity", "🎯 === STARTING SPLASH ACHIEVEMENT CHECK for user: ${user.id} ===")
            
            Log.d("SplashActivity", "🎯 Calling achievementManager.checkAchievementsAfterTaskCompletion() for splash")
            // Check course completion achievements
            achievementManager.checkAchievementsAfterTaskCompletion(user.id.toString(), "all")
            
            Log.d("SplashActivity", "🎯 Calling achievementManager.checkStreakAchievement() for splash")
            // Check streak achievement
            achievementManager.checkStreakAchievement(user.id.toString())
            
            Log.d("SplashActivity", "🎯 === SPLASH ACHIEVEMENT CHECK COMPLETED ===")
        } catch (e: Exception) {
            Log.e("SplashActivity", "🎯 Error checking existing achievements on splash", e)
        }
    }
} 