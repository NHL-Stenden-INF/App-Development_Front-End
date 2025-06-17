package com.nhlstenden.appdev.supabase

import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.Response
import org.json.JSONObject

suspend fun SupabaseClient.getUser(email: String, password: String): User {
    throw UnsupportedOperationException("Method not implemented")
}

fun SupabaseClient.signup(email: String, password: String, username: String): Response {
    val json = """{"email": "$email", "password": "$password", "data": { "display_name": "$username" } }"""
    val requestBody = json.toRequestBody("application/json".toMediaType())

    val request = Request.Builder()
        .url("$supabaseUrl/auth/v1/signup")
        .post(requestBody)
        .addHeader("apikey", supabaseKey)
        .addHeader("Content-Type", "application/json")
        .build()

    return client.newCall(request).execute()
}

suspend fun SupabaseClient.login(email: String, password: String): String {
    return withContext(Dispatchers.IO) {
        val json = """{"email": "$email", "password": "$password"}"""
        val requestBody = json.toRequestBody("application/json".toMediaType())

        val request = Request.Builder()
            .url("$supabaseUrl/auth/v1/token?grant_type=password")
            .post(requestBody)
            .addHeader("apikey", supabaseKey)
            .addHeader("Content-Type", "application/json")
            .build()

        val response = client.newCall(request).execute()
        Log.d("SupabaseClient", "Login response code: ${response.code}")
        if (!response.isSuccessful) {
            val errorBody = response.body?.string()
            Log.e("SupabaseClient", "Login failed with response: $errorBody")
            throw RuntimeException(errorBody ?: "Login failed with code ${response.code}")
        }
        val body = response.body?.string() ?: throw RuntimeException("No response body")
        val jsonObj = JSONObject(body)
        jsonObj.optString("access_token", "")
    }
}

suspend fun SupabaseClient.register(email: String, password: String, displayName: String): String {
    val json = """{"email": "$email", "password": "$password", "data": { "display_name": "$displayName" } }"""
    val requestBody = json.toRequestBody("application/json".toMediaType())
    val request = Request.Builder()
        .url("$supabaseUrl/auth/v1/signup")
        .post(requestBody)
        .addHeader("apikey", supabaseKey)
        .addHeader("Content-Type", "application/json")
        .build()
    val response = client.newCall(request).execute()
    if (!response.isSuccessful) throw RuntimeException(response.body?.string())
    val body = response.body?.string() ?: throw RuntimeException("No response body")
    val jsonObj = JSONObject(body)
    return jsonObj.optString("access_token", "")
}