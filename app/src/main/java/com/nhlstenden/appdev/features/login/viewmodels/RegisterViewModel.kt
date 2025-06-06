package com.nhlstenden.appdev.features.login.viewmodels

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.nhlstenden.appdev.supabase.SupabaseClient
import com.nhlstenden.appdev.core.models.User
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@HiltViewModel
class RegisterViewModel @Inject constructor(
    private val supabaseClient: SupabaseClient
) : ViewModel() {
    private val _registerState = MutableStateFlow<RegisterState>(RegisterState.Initial)
    val registerState: StateFlow<RegisterState> = _registerState

    fun register(email: String, password: String) {
        viewModelScope.launch(Dispatchers.IO) {
            _registerState.value = RegisterState.Loading
            try {
                Log.d("RegisterViewModel", "Attempting to register with email: $email")
                val username = email.split("@")[0]
                // Register the user and get the access token
                val accessToken = supabaseClient.register(email, password, username)
                // Fetch profile and attributes using the access token
                val profile = supabaseClient.fetchProfile(accessToken)
                val attributes = supabaseClient.fetchUserAttributes(accessToken)
                // Construct the User object (update as needed for your User model)
                val user = User(
                    id = profile.getString("id"),
                    username = profile.optString("display_name", username),
                    email = profile.optString("email", email),
                    profilePicture = profile.optString("profile_picture", ""),
                    friends = mutableListOf(),
                    authToken = accessToken
                )
                Log.d("RegisterViewModel", "Registration and login successful for user: "+user.email)
                withContext(Dispatchers.Main) {
                    _registerState.value = RegisterState.Success(user)
                }
            } catch (e: Exception) {
                Log.e("RegisterViewModel", "Registration failed", e)
                val errorMessage = when {
                    e.message?.contains("User already registered") == true -> "This email is already registered"
                    e.message?.contains("Invalid email") == true -> "Please enter a valid email address"
                    e.message?.contains("Password too short") == true -> "Password must be at least 6 characters"
                    else -> "Registration failed: ${e.message}"
                }
                withContext(Dispatchers.Main) {
                    _registerState.value = RegisterState.Error(errorMessage)
                }
            }
        }
    }

    sealed class RegisterState {
        object Initial : RegisterState()
        object Loading : RegisterState()
        data class Success(val user: User) : RegisterState()
        data class Error(val message: String) : RegisterState()
    }
} 