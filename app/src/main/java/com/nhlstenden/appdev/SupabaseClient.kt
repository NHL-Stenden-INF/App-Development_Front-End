package com.nhlstenden.appdev

import android.os.Parcelable
import android.util.Log
import kotlinx.android.parcel.Parcelize
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.Response
import org.json.JSONArray
import org.json.JSONObject
import java.util.UUID

class SupabaseClient() {
    private val client = OkHttpClient()

    private val supabaseUrl = "https://ggpdstbylyiwkfcucoxd.supabase.co"
    private val supabaseKey = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJpc3MiOiJzdXBhYmFzZSIsInJlZiI6ImdncGRzdGJ5bHlpd2tmY3Vjb3hkIiwicm9sZSI6ImFub24iLCJpYXQiOjE3NDczMDg4MzYsImV4cCI6MjA2Mjg4NDgzNn0.2ZGOttYWxBJkNcPmAtJh6dzlm3G6vwpIonEtRvtNNa8"

    fun createNewUser(email: String, password: String, username: String) {
        val signupRequest = this.signup(email, password, username)
        if (signupRequest.code != 200) {
            throw RuntimeException(signupRequest.body?.string())
        }
        val authToken = JSONObject(signupRequest.body?.string()).getString("access_token")
        val createUserRequest = this.createUserAttributes(authToken)
        if (createUserRequest.code != 201) {
            throw RuntimeException(createUserRequest.body?.string())
        }
    }

    fun getUser(email: String, password: String): User {
        val loginRequest = login(email, password)
        if (loginRequest.code != 200) {
            throw RuntimeException(loginRequest.body?.string())
        }
        val authResponse = JSONObject(loginRequest.body?.string())
        val userRequest = this.getUserAttributes(authResponse.getJSONObject("user").getString("id"))
        if (userRequest.code != 200) {
            throw RuntimeException(userRequest.body?.string())
        }

        val userResponse = JSONArray(userRequest.body?.string())
        val friends = ArrayList<UUID>()
        val jsonFriends = userResponse.getJSONObject(0).getJSONArray("friends")
        for (i in 0 until jsonFriends.length()) {
            friends.add(UUID.fromString(jsonFriends.getString(i)))
        }

        val achievements = ArrayList<String>()
        val jsonAchievements = userResponse.getJSONObject(0).getJSONArray("achievements")
        for (i in 0 until jsonAchievements.length()) {
            achievements.add(jsonAchievements.getString(i))
        }

        return User(
            authResponse.getString("access_token"),
            UUID.fromString(authResponse.getJSONObject("user").getString("id")),
            authResponse.getJSONObject("user").getJSONObject("user_metadata").getString("display_name"),
            authResponse.getJSONObject("user").getString("email"),
            userResponse.getJSONObject(0).getInt("points"),
            friends,
            achievements,
            userResponse.getJSONObject(0).getString("profile_picture"),
        )
    }

    fun signup(email: String, password: String, username: String): Response {
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

    fun getUserAttributes(userId: String): Response {
        val request = Request.Builder()
            .url("$supabaseUrl/rest/v1/user_attributes?select=*&user_id=eq.$userId")
            .get()
            .addHeader("apikey", supabaseKey)
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

    fun addFriend(userId: String, authToken: String): Response {
        val json = """{"new_friend_id": "$userId"}"""
        val requestBody = json.toRequestBody("application/json".toMediaType())

        val request = Request.Builder()
            .url("$supabaseUrl/rest/v1/rpc/add_friend")
            .post(requestBody)
            .addHeader("apikey", supabaseKey)
            .addHeader("Authorization", "Bearer $authToken")
            .addHeader("Content-Type", "application/json")
            .addHeader("Prefer", "return=minimal")
            .build()

        return client.newCall(request).execute()
    }

    fun updateUserPoints(userId: String, newPoints: Int, authToken: String): Response {
        // Use RPC (Remote Procedure Call) instead of PATCH
        // This calls a database function directly, bypassing any REST API issues
        val json = """{"input_user_id": "$userId", "new_points": $newPoints}"""
        val requestBody = json.toRequestBody("application/json".toMediaType())
        val request = Request.Builder()
            .url("$supabaseUrl/rest/v1/rpc/update_user_points")
            .post(requestBody) // RPC uses POST, not PATCH
            .addHeader("apikey", supabaseKey)
            .addHeader("Authorization", "Bearer $authToken")
            .addHeader("Content-Type", "application/json")
            .addHeader("Prefer", "return=minimal")
            .build()
        return client.newCall(request).execute()
    }

    fun updateUserOpenedDaily(userId: String, date: String, authToken: String): Response {
        val json = """{"input_user_id": "$userId", "new_date": "$date"}"""
        val requestBody = json.toRequestBody("application/json".toMediaType())
        val request = Request.Builder()
            .url("$supabaseUrl/rest/v1/rpc/update_user_opened_daily")
            .post(requestBody) // RPC uses POST, not PATCH
            .addHeader("apikey", supabaseKey)
            .addHeader("Authorization", "Bearer $authToken")
            .addHeader("Content-Type", "application/json")
            .addHeader("Prefer", "return=minimal")
            .build()
        return client.newCall(request).execute()
    }

    fun updateOpenedDailyAt(userId: String, date: String, authToken: String): Response {
        val json = """{"opened_daily_at": "$date"}"""
        val requestBody = json.toRequestBody("application/json".toMediaType())
        val request = Request.Builder()
            .url("$supabaseUrl/rest/v1/user_attributes?user_id=eq.$userId")
            .patch(requestBody)
            .addHeader("apikey", supabaseKey)
            .addHeader("Authorization", "Bearer $authToken")
            .addHeader("Content-Type", "application/json")
            .addHeader("Prefer", "return=minimal")
            .build()
        return client.newCall(request).execute()
    }
}

@Parcelize
data class User(
    val authToken: String,
    val id: UUID,
    val username: String,
    val email: String,
    val points: Int,
    val friends: ArrayList<UUID>,
    val achievements: ArrayList<String>,
    val profilePicture: String
) : Parcelable
