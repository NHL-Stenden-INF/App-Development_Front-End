package com.nhlstenden.appdev.core.repositories

import android.util.Log
import okhttp3.Response
import org.json.JSONArray
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

abstract class BaseRepository {
    
    protected suspend fun isJWTExpired(response: Response, authRepository: AuthRepository): Boolean {
        if (response.code == 401) {
            val body = response.body?.string()
            if (body?.contains("JWT expired") == true) {
                Log.w(this::class.simpleName, "JWT expired detected, clearing session")
                authRepository.handleJWTExpiration()
                return true
            }
        }
        return false
    }
    
    protected suspend fun handleApiResponse(
        response: Response,
        authRepository: AuthRepository,
        onSuccess: (String) -> Unit,
        onError: (String) -> Unit = {}
    ) {
        try {
            if (response.isSuccessful) {
                val body = response.body?.string()
                if (!body.isNullOrEmpty()) {
                    onSuccess(body)
                } else {
                    onError("Empty response body")
                }
            } else {
                if (isJWTExpired(response, authRepository)) {
                    throw Exception("Session expired. Please login again.")
                } else {
                    Log.e(this::class.simpleName, "API call failed: ${response.code}")
                    onError("API call failed: ${response.code}")
                }
            }
        } catch (e: Exception) {
            Log.e(this::class.simpleName, "Error handling API response", e)
            if (e.message?.contains("Session expired") == true) {
                throw e
            }
            onError(e.message ?: "Unknown error")
        }
    }
    
    protected fun parseJsonArraySafely(
        jsonString: String,
        onSuccess: (JSONArray) -> Unit,
        onError: (String) -> Unit = {}
    ) {
        try {
            val jsonArray = JSONArray(jsonString)
            onSuccess(jsonArray)
        } catch (e: Exception) {
            Log.e(this::class.simpleName, "Error parsing JSON array", e)
            onError("Failed to parse JSON response")
        }
    }
} 