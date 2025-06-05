package com.nhlstenden.appdev.shared.ui.base

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

open class BaseViewModel(application: Application) : AndroidViewModel(application) {
    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error

    private val _success = MutableStateFlow<String?>(null)
    val success: StateFlow<String?> = _success

    protected fun launchWithLoading(block: suspend CoroutineScope.() -> Unit): Job {
        return viewModelScope.launch {
            _isLoading.value = true
            _error.value = null
            _success.value = null
            try {
                block()
            } catch (e: Exception) {
                _error.value = e.message ?: "Unknown error"
            } finally {
                _isLoading.value = false
            }
        }
    }

    protected fun setError(message: String) {
        _error.value = message
        _isLoading.value = false
    }

    protected fun setSuccess(message: String) {
        _success.value = message
        _isLoading.value = false
    }
} 