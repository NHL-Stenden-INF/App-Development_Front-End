package com.nhlstenden.appdev.features.login.viewmodels

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.nhlstenden.appdev.core.repositories.AuthRepository
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
    private val authRepository: AuthRepository
) : ViewModel() {
    private val _registerState = MutableStateFlow<RegisterState>(RegisterState.Initial)
    val registerState: StateFlow<RegisterState> = _registerState

    fun register(email: String, password: String) {
        viewModelScope.launch {
            _registerState.value = RegisterState.Loading
            try {
                Log.d("RegisterViewModel", "Attempting to register with email: $email")
                val username = email.split("@")[0]
                
                val result = authRepository.register(email, password, username)
                if (result.isSuccess) {
                    val user = result.getOrThrow()
                    Log.d("RegisterViewModel", "Registration and login successful for user: ${user.email}")
                    _registerState.value = RegisterState.Success(user)
                } else {
                    val error = result.exceptionOrNull()
                    Log.e("RegisterViewModel", "Registration failed", error)
                    val errorMessage = when {
                        error?.message?.contains("User already registered") == true -> "This email is already registered"
                        error?.message?.contains("Invalid email") == true -> "Please enter a valid email address"
                        error?.message?.contains("Password too short") == true -> "Password must be at least 6 characters"
                        else -> "Registration failed: ${error?.message}"
                    }
                    _registerState.value = RegisterState.Error(errorMessage)
                }
            } catch (e: Exception) {
                Log.e("RegisterViewModel", "Unexpected registration error", e)
                _registerState.value = RegisterState.Error("Registration failed: ${e.message}")
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