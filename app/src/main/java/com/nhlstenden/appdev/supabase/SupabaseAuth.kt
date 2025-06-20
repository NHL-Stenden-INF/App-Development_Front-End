package com.nhlstenden.appdev.supabase

import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject

private val TAG = "SupabaseAuth"

suspend fun SupabaseClient.login(email: String, password: String): Result<String> {
    return withContext(Dispatchers.IO) {
        val json = """{"email": "$email", "password": "$password"}"""
        val requestBody = json.toRequestBody("application/json".toMediaType())
        val request = Request.Builder()
            .url("$supabaseUrl/auth/v1/token?grant_type=password")
            .post(requestBody)
            .addHeader("apikey", supabaseKey)
            .addHeader("Content-Type", "application/json")
            .build()

        try {
            val response = client.newCall(request).execute()
            Log.d("SupabaseClient", "Login response code: ${response.code}")

            if (!response.isSuccessful) {
                val errorBody = response.body?.string()
                Log.e("SupabaseClient", "Login failed with response: $errorBody")
                return@withContext Result.failure(Exception("Login failed: ${errorBody ?: "Unknown error"}"))
            }

            val body = response.body?.string() ?: throw RuntimeException("No response body")
            val jsonObj = JSONObject(body)
            val accessToken = jsonObj.optString("access_token", "")

            if (accessToken.isEmpty()) {
                return@withContext Result.failure(Exception("Access token not found in response"))
            }

            return@withContext Result.success(accessToken)
        } catch (e: Exception) {
            Log.e(TAG, "error", e)
            return@withContext Result.failure(e)
        }
    }
}

suspend fun SupabaseClient.register(email: String, password: String, displayName: String): Result<String> {
    return withContext(Dispatchers.IO) {
        val json = """{"email": "$email", "password": "$password", "data": { "display_name": "$displayName" } }"""
        val requestBody = json.toRequestBody("application/json".toMediaType())
        val request = Request.Builder()
            .url("$supabaseUrl/auth/v1/signup")
            .post(requestBody)
            .addHeader("apikey", supabaseKey)
            .addHeader("Content-Type", "application/json")
            .build()

        try {
            val response = client.newCall(request).execute()

            if (!response.isSuccessful) {
                return@withContext Result.failure(Exception("Failed to register user: ${response.message}"))
            }

            val body = response.body?.string()
                ?: return@withContext Result.failure(Exception("No response body"))

            val jsonObj = JSONObject(body)
            val accessToken = jsonObj.optString("access_token", "")

            if (accessToken.isEmpty()) {
                return@withContext Result.failure(Exception("Access token not found in response"))
            }

            return@withContext Result.success(accessToken)
        } catch (e: Exception) {
            Log.e(TAG, "Error in registering user", e)
            return@withContext Result.failure(e)
        }
    }
}