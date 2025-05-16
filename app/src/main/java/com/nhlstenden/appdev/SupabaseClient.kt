package com.nhlstenden.appdev

import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.Response
import org.json.JSONObject

class SupabaseClient() {
    private val client = OkHttpClient()

    private val supabaseUrl = "https://ggpdstbylyiwkfcucoxd.supabase.co"
    private val supabaseKey = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJpc3MiOiJzdXBhYmFzZSIsInJlZiI6ImdncGRzdGJ5bHlpd2tmY3Vjb3hkIiwicm9sZSI6ImFub24iLCJpYXQiOjE3NDczMDg4MzYsImV4cCI6MjA2Mjg4NDgzNn0.2ZGOttYWxBJkNcPmAtJh6dzlm3G6vwpIonEtRvtNNa8"

    fun createNewUser(email: String, password: String): Response {
        val signupRequest = this.signup(email, password)
        if (signupRequest.code != 200) {
            throw IllegalArgumentException("Unable to create a new user: " + signupRequest.body?.string() + "\nStatus code: " + signupRequest.code)
        }
        val loginRequest = login (email, password)
        if (loginRequest.code != 200) {
            throw IllegalArgumentException("Unable to create a new user: " + signupRequest.body?.string() + "\nStatus code: " + signupRequest.code)
        }
        val authToken = JSONObject(loginRequest.body?.string()).getString("access_token")
        return this.createUserAttributes(authToken)
    }

    fun signup(email: String, password: String): Response {
        val json = """{"email": "$email", "password": "$password"}"""
        val requestBody = json.toRequestBody("application/json".toMediaType())

        val request = Request.Builder()
            .url("$supabaseUrl/auth/v1/signup")
            .post(requestBody)
            .addHeader("apikey", supabaseKey)
            .addHeader("Content-Type", "application/json")
            .build()

        return client.newCall(request).execute()
    }

    fun login(email: String, password: String): Response {
        val json = """{"email": "$email", "password": "$password"}"""
        val requestBody = json.toRequestBody("application/json".toMediaType())

        val request = Request.Builder()
            .url("$supabaseUrl/auth/v1/token?grant_type=password")
            .post(requestBody)
            .addHeader("apikey", supabaseKey)
            .addHeader("Content-Type", "application/json")
            .build()

        return client.newCall(request).execute()
    }

    fun getUserAttributes(authToken: String): Response {
        val request = Request.Builder()
            .url("$supabaseUrl/rest/v1/user_attributes?select=*")
            .get()
            .addHeader("apikey", supabaseKey)
            .addHeader("Authorization", "Bearer $authToken")
            .build()

        return client.newCall(request).execute()
    }

    fun createUserAttributes(authToken: String): Response {
        val json = "{}"
        val requestBody = json.toRequestBody("application/json".toMediaType())

        val request = Request.Builder()
            .url("$supabaseUrl/rest/v1/user_attributes")
            .post(requestBody)
            .addHeader("apikey", supabaseKey)
            .addHeader("Authorization", "Bearer $authToken")
            .addHeader("Content-Type", "application/json")
            .addHeader("Prefer", "return=minimal")
            .build()

        return client.newCall(request).execute()
    }
}
