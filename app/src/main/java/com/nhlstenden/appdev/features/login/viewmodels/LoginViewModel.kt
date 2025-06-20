package com.nhlstenden.appdev.features.login.viewmodels

import android.os.Build
import android.util.Log
import androidx.annotation.RequiresApi
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.nhlstenden.appdev.core.repositories.AuthRepository
import com.nhlstenden.appdev.core.repositories.UserRepository
import com.nhlstenden.appdev.features.rewards.AchievementManager
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.time.LocalDate
import java.time.temporal.ChronoUnit
import java.util.UUID
import com.nhlstenden.appdev.core.models.User

@HiltViewModel
class LoginViewModel @Inject constructor(
    private val authRepository: AuthRepository,
    private val userRepository: UserRepository,
    private val achievementManager: AchievementManager
) : ViewModel() {
    private val _loginState = MutableStateFlow<LoginState>(LoginState.Initial)
    val loginState: StateFlow<LoginState> = _loginState

    @RequiresApi(Build.VERSION_CODES.O)
    fun login(email: String, password: String) {
        viewModelScope.launch {
            _loginState.value = LoginState.Loading
            try {
                Log.d("LoginViewModel", "Attempting to login with email: $email")
                
                val result = authRepository.login(email, password)
                if (result.isSuccess) {
                    val user = result.getOrThrow()
                    
                    // Update streak logic using the new UserRepository
                    updateStreakIfNeeded(user)
                    
                    // Check for any achievements that should be unlocked
                    checkExistingAchievements(user)
                    
                    Log.d("LoginViewModel", "Login successful for user: ${user.email}")
                    _loginState.value = LoginState.Success(user)
                } else {
                    val error = result.exceptionOrNull()
                    Log.e("LoginViewModel", "Login failed", error)
                    val errorMessage = when {
                        error?.message?.contains("Invalid login credentials") == true -> "Invalid email or password"
                        error?.message?.contains("Email not confirmed") == true -> "Please confirm your email first"
                        else -> "Login failed: ${error?.message}"
                    }
                    _loginState.value = LoginState.Error(errorMessage)
                }
            } catch (e: Exception) {
                Log.e("LoginViewModel", "Unexpected login error", e)
                _loginState.value = LoginState.Error("Login failed: ${e.message}")
            }
        }
    }
    
    @RequiresApi(Build.VERSION_CODES.O)
    private suspend fun updateStreakIfNeeded(user: User) {
        try {
            val attributesResult = userRepository.getUserAttributes(user.id)
            if (attributesResult.isSuccess) {
                val attributes = attributesResult.getOrThrow()
                
                val currentStreak = attributes.optInt("streak", 0)
                val lastTaskDateStr = attributes.optString("last_task_date", "")
                Log.d("LoginViewModel", "Current streak from DB: $currentStreak")
                Log.d("LoginViewModel", "Last task date from DB: $lastTaskDateStr")
                
                val lastTaskDate = if (lastTaskDateStr.isNotEmpty() && lastTaskDateStr != "null") {
                    try {
                        LocalDate.parse(lastTaskDateStr)
                    } catch (e: Exception) {
                        Log.e("LoginViewModel", "Error parsing last task date: ${e.message}")
                        null
                    }
                } else null

                val today = LocalDate.now()
                Log.d("LoginViewModel", "Today's date: $today")
                
                val newStreak = if (lastTaskDate != null) {
                    val daysBetween = ChronoUnit.DAYS.between(lastTaskDate, today)
                    Log.d("LoginViewModel", "Days between last task and today: $daysBetween")
                    
                    when {
                        daysBetween == 0L -> {
                            Log.d("LoginViewModel", "Same day as last task, keeping streak at $currentStreak")
                            currentStreak
                        }
                        daysBetween == 1L -> {
                            Log.d("LoginViewModel", "Next day after last task, keeping streak at $currentStreak")
                            currentStreak
                        }
                        else -> {
                            Log.d("LoginViewModel", "More than one day has passed ($daysBetween days), resetting streak from $currentStreak to 0")
                            0
                        }
                    }
                } else {
                    Log.d("LoginViewModel", "No last task date found, starting with streak 0")
                    0
                }

                // Update streak in database if it changed
                if (newStreak != currentStreak) {
                    Log.d("LoginViewModel", "Updating streak in database from $currentStreak to $newStreak")
                    userRepository.updateUserStreak(user.id, newStreak)
                } else {
                    Log.d("LoginViewModel", "No streak update needed, keeping streak at $currentStreak")
                }
            }
        } catch (e: Exception) {
            Log.e("LoginViewModel", "Error updating streak", e)
        }
    }
    
    private fun checkExistingAchievements(user: User) {
        viewModelScope.launch {
            try {
                Log.d("LoginViewModel", "🎯 === STARTING LOGIN ACHIEVEMENT CHECK for user: ${user.id} ===")
                
                Log.d("LoginViewModel", "🎯 Calling achievementManager.checkAchievementsAfterTaskCompletion() for login")
                // Check course completion achievements
                achievementManager.checkAchievementsAfterTaskCompletion(user.id.toString(), "all")
                
                Log.d("LoginViewModel", "🎯 Calling achievementManager.checkStreakAchievement() for login")
                // Check streak achievement
                achievementManager.checkStreakAchievement(user.id.toString())
                
                Log.d("LoginViewModel", "🎯 === LOGIN ACHIEVEMENT CHECK COMPLETED ===")
            } catch (e: Exception) {
                Log.e("LoginViewModel", "🎯 Error checking existing achievements on login", e)
            }
        }
    }

    sealed class LoginState {
        object Initial : LoginState()
        object Loading : LoginState()
        data class Success(val user: User) : LoginState()
        data class Error(val message: String) : LoginState()
    }
} 