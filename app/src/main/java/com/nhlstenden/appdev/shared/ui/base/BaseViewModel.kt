package com.nhlstenden.appdev.shared.ui.base

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

abstract class BaseViewModel(application: Application) : AndroidViewModel(application) {
    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()
    
    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()
    
    private val _success = MutableStateFlow<String?>(null)
    val success: StateFlow<String?> = _success.asStateFlow()
    
    protected fun launchWithLoading(block: suspend () -> Unit) {
        viewModelScope.launch {
            try {
                _isLoading.value = true
                _error.value = null
                _success.value = null
                block()
            } catch (e: Exception) {
                handleError(e)
            } finally {
                _isLoading.value = false
            }
        }
    }
    
    protected fun handleError(error: Throwable) {
        val errorMessage = when (error) {
            is IllegalArgumentException -> error.message ?: "Invalid input"
            is IllegalStateException -> error.message ?: "Invalid state"
            else -> error.message ?: "An unexpected error occurred"
        }
        setError(errorMessage)
    }
    
    protected fun setError(message: String) {
        _error.value = message
        _success.value = null
    }
    
    protected fun setSuccess(message: String) {
        _success.value = message
        _error.value = null
    }
    
    protected fun clearError() {
        _error.value = null
    }
    
    protected fun clearSuccess() {
        _success.value = null
    }
    
    protected fun clearAllMessages() {
        _error.value = null
        _success.value = null
    }
} 