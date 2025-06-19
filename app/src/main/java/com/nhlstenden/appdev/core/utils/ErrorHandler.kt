package com.nhlstenden.appdev.core.utils

import android.util.Log

object ErrorHandler {
    
    fun <T> handleResult(
        result: Result<T>,
        tag: String,
        operation: String,
        onSuccess: (T) -> Unit = {},
        onFailure: (String) -> Unit = {}
    ) {
        result.fold(
            onSuccess = { data ->
                Log.d(tag, "$operation completed successfully")
                onSuccess(data)
            },
            onFailure = { exception ->
                val errorMessage = exception.message ?: "Unknown error occurred"
                Log.e(tag, "$operation failed: $errorMessage", exception)
                onFailure(errorMessage)
            }
        )
    }
    
    fun createErrorMessage(operation: String, exception: Throwable?): String {
        return "Error during $operation: ${exception?.message ?: "Unknown error"}"
    }
} 