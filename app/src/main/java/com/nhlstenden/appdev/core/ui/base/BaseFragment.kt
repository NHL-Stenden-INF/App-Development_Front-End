package com.nhlstenden.appdev.core.ui.base

import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModel
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.launch
import android.util.Log

abstract class BaseFragment : Fragment() {
    
    protected fun launchSafely(action: suspend () -> Unit) {
        lifecycleScope.launch {
            try {
                action()
            } catch (e: Exception) {
                Log.e(this@BaseFragment::class.simpleName, "Error in fragment operation", e)
                handleError(e)
            }
        }
    }
    
    protected open fun handleError(error: Throwable) {
        // Override in subclasses for specific error handling
    }
} 