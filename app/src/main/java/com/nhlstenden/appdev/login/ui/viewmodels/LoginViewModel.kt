package com.nhlstenden.appdev.login.ui.viewmodels

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.nhlstenden.appdev.supabase.User
import com.nhlstenden.appdev.supabase.SupabaseClient
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.UUID
import javax.inject.Inject

@HiltViewModel
class LoginViewModel @Inject constructor(
    private val supabaseClient: SupabaseClient
) : ViewModel() {
    private val _loginState = MutableStateFlow<LoginState>(LoginState.Initial)
    val loginState: StateFlow<LoginState> = _loginState

    fun login(email: String, password: String) {
        viewModelScope.launch(Dispatchers.IO) {
            _loginState.value = LoginState.Loading
            try {
                Log.d("LoginViewModel", "Attempting to login with email: $email")
                val user = supabaseClient.getUser(email, password)
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