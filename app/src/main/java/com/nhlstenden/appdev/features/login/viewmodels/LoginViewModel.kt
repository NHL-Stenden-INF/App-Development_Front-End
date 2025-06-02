package com.nhlstenden.appdev.features.login.viewmodels

import android.os.Build
import android.util.Log
import androidx.annotation.RequiresApi
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.nhlstenden.appdev.supabase.SupabaseClient
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
    private val supabaseClient: SupabaseClient
) : ViewModel() {
    private val _loginState = MutableStateFlow<LoginState>(LoginState.Initial)
    val loginState: StateFlow<LoginState> = _loginState

    @RequiresApi(Build.VERSION_CODES.O)
    fun login(email: String, password: String) {
        viewModelScope.launch(Dispatchers.IO) {
            _loginState.value = LoginState.Loading
            try {
                Log.d("LoginViewModel", "Attempting to login with email: $email")
                // Login and get the access token
                val accessToken = supabaseClient.login(email, password)
                // Fetch profile and attributes using the access token
                val profile = supabaseClient.fetchProfile(accessToken)
                val attributes = supabaseClient.fetchUserAttributes(accessToken)
                
                // Get current streak and last task date
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

                // Check if streak should be reset
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
                            Log.d("LoginViewModel", "More than one day has passed, resetting streak from $currentStreak to 0")
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
                    supabaseClient.updateUserStreak(profile.getString("id"), newStreak, accessToken)
                } else {
                    Log.d("LoginViewModel", "No streak update needed, keeping streak at $currentStreak")
                }

                // Construct the User object
                val user = User(
                    id = profile.getString("id"),
                    email = profile.optString("email", email),
                    username = profile.optString("display_name", email.split("@")[0]),
                    profilePicture = profile.optString("profile_picture", ""),
                    friends = mutableListOf(),
                    authToken = accessToken
                )
                
                Log.d("LoginViewModel", "Login successful for user: ${user.email}")
                withContext(Dispatchers.Main) {
                    _loginState.value = LoginState.Success(user)
                }
            } catch (e: Exception) {
                Log.e("LoginViewModel", "Login failed", e)
                val errorMessage = when {
                    e.message?.contains("Invalid login credentials") == true -> "Invalid email or password"
                    e.message?.contains("Email not confirmed") == true -> "Please confirm your email first"
                    else -> "Login failed: ${e.message}"
                }
                withContext(Dispatchers.Main) {
                    _loginState.value = LoginState.Error(errorMessage)
                }
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