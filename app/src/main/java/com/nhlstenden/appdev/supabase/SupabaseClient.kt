package com.nhlstenden.appdev.supabase

import android.os.Parcelable
import android.util.Log
import kotlinx.parcelize.Parcelize
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.Response
import org.json.JSONArray
import org.json.JSONObject
import java.util.UUID

class SupabaseClient() {
    val client = OkHttpClient()

    val supabaseUrl = "https://ggpdstbylyiwkfcucoxd.supabase.co"
    val supabaseKey = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJpc3MiOiJzdXBhYmFzZSIsInJlZiI6ImdncGRzdGJ5bHlpd2tmY3Vjb3hkIiwicm9sZSI6ImFub24iLCJpYXQiOjE3NDczMDg4MzYsImV4cCI6MjA2Mjg4NDgzNn0.2ZGOttYWxBJkNcPmAtJh6dzlm3G6vwpIonEtRvtNNa8"

    fun createNewUser(email: String, password: String, username: String) {
        val signupRequest = this.signup(email, password, username)
        if (!signupRequest.isSuccessful) {
            throw RuntimeException(signupRequest.body?.string())
        }

        val authToken = JSONObject(signupRequest.body?.string()).getString("access_token")
        val createUserRequest = this.createUserAttributes(authToken)
        if (!createUserRequest.isSuccessful) {
            throw RuntimeException(createUserRequest.body?.string())
        }
    }

    suspend fun getUser(email: String, password: String): User {
        Log.d("SupabaseClient", "Attempting to get user with email: $email")
        val loginRequest = login(email, password)
        if (!loginRequest.isSuccessful) {
            val errorBody = loginRequest.body?.string()
            Log.e("SupabaseClient", "Login request failed: $errorBody")
            throw RuntimeException(errorBody ?: "Login failed")
        }

        val authResponse = JSONObject(loginRequest.body?.string())
        Log.d("SupabaseClient", "Auth response received: ${authResponse.toString()}")
        
        val userId = authResponse.getJSONObject("user").getString("id")
        Log.d("SupabaseClient", "Getting user attributes for ID: $userId")
        
        val userRequest = withContext(Dispatchers.IO) {
            getUserAttributes(userId)
        }
        if (!userRequest.isSuccessful) {
            val errorBody = userRequest.body?.string()
            Log.e("SupabaseClient", "Failed to get user attributes: $errorBody")
            throw RuntimeException(errorBody ?: "Failed to get user attributes")
        }

        val userResponse = JSONArray(userRequest.body?.string())
        Log.d("SupabaseClient", "User attributes received: ${userResponse.toString()}")
        
        if (userResponse.length() == 0) {
            Log.e("SupabaseClient", "No user attributes found for ID: $userId")
            throw RuntimeException("User attributes not found")
        }

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

        val user = User(
            authResponse.getString("access_token"),
            UUID.fromString(userId),
            authResponse.getJSONObject("user").getJSONObject("user_metadata").getString("display_name"),
            authResponse.getJSONObject("user").getString("email"),
            userResponse.getJSONObject(0).getInt("points"),
            friends,
            achievements,
            userResponse.getJSONObject(0).getString("profile_picture"),
        )
        Log.d("SupabaseClient", "User object created successfully: ${user.email}")
        return user
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

    suspend fun login(email: String, password: String): Response {
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
            response
        }
    }

    suspend fun getUserAttributes(userId: String): Response {
        if (userId.isBlank() || userId == "null") {
            Log.e("SupabaseClient", "Invalid userId provided: $userId")
            throw IllegalArgumentException("Invalid userId provided")
        }
        
        return withContext(Dispatchers.IO) {
            val request = Request.Builder()
                .url("$supabaseUrl/rest/v1/user_attributes?user_id=eq.$userId")
                .get()
                .addHeader("apikey", supabaseKey)
                .build()

            client.newCall(request).execute()
        }
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

    suspend fun addFriend(friendId: String, authToken: String): Response {
        val json = """{"friend_id": "${friendId.trim()}"}"""
        val requestBody = json.toRequestBody("application/json".toMediaType())

        val request = Request.Builder()
            .url("$supabaseUrl/rest/v1/rpc/add_friend")
            .post(requestBody)
            .addHeader("apikey", supabaseKey)
            .addHeader("Authorization", "Bearer $authToken")
            .addHeader("Content-Type", "application/json")
            .addHeader("Prefer", "return=minimal")
            .build()

        return withContext(Dispatchers.IO) {
            client.newCall(request).execute()
        }
    }

    fun updateUserPoints(userId: String, newPoints: Int, authToken: String): Response {
        val json = """{"points": $newPoints}"""
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

    fun updateUserOpenedDaily(userId: String, date: String, authToken: String): Response {
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

    fun updateProfilePicture(userId: String, profilePicture: String, authToken: String): Response {
        // Log the attempt to update
        Log.d("SupabaseClient", "Attempting to update profile picture for user: $userId")
        
        try {
            // First try with RPC approach
            val jsonObject = org.json.JSONObject()
            jsonObject.put("input_user_id", userId)
            jsonObject.put("new_profile_picture", profilePicture)
            
            val requestBody = jsonObject.toString().toRequestBody("application/json".toMediaType())
            
            val request = Request.Builder()
                .url("$supabaseUrl/rest/v1/rpc/update_user_profile_picture")
                .post(requestBody)
                .addHeader("apikey", supabaseKey)
                .addHeader("Authorization", "Bearer $authToken")
                .addHeader("Content-Type", "application/json")
                .addHeader("Prefer", "return=minimal")
                .build()
            
            val response = client.newCall(request).execute()
            
            // Log the response
            Log.d("SupabaseClient", "RPC profile picture update response: ${response.code} ${response.message}")
            
            // If RPC succeeds or returns something other than 404, return that response
            if (response.isSuccessful || response.code != 404) {
                return response
            }
            
            // If we get here, RPC function doesn't exist, try direct update
            Log.d("SupabaseClient", "RPC method not found, trying direct update")
            
            val patchJson = org.json.JSONObject()
            patchJson.put("profile_picture", profilePicture)
            
            val patchRequestBody = patchJson.toString().toRequestBody("application/json".toMediaType())
            
            val patchRequest = Request.Builder()
                .url("$supabaseUrl/rest/v1/user_attributes?user_id=eq.$userId")
                .patch(patchRequestBody)
                .addHeader("apikey", supabaseKey)
                .addHeader("Authorization", "Bearer $authToken")
                .addHeader("Content-Type", "application/json")
                .addHeader("Prefer", "return=minimal")
                .build()
            
            return client.newCall(patchRequest).execute()
        } catch (e: Exception) {
            Log.e("SupabaseClient", "Error updating profile picture", e)
            throw e
        }
    }

    // Get user unlocked rewards using direct query
    fun getUserUnlockedRewards(userId: String, authToken: String): Response {
        if (userId.isBlank() || userId == "null") {
            Log.e("SupabaseClient", "Invalid userId provided for unlocked rewards: $userId")
            throw IllegalArgumentException("Invalid userId provided")
        }

        val request = Request.Builder()
            .url("$supabaseUrl/rest/v1/user_rewards?user_id=eq.$userId&select=reward_id,unlocked_at")
            .get()
            .addHeader("apikey", supabaseKey)
            .addHeader("Authorization", "Bearer $authToken")
            .build()

        return client.newCall(request).execute()
    }

    // Get friend details by user ID
    fun getFriendDetails(friendId: String, authToken: String): Response {
        val request = Request.Builder()
            .url("$supabaseUrl/rest/v1/user_attributes?select=user_id,points,profile_picture&user_id=eq.$friendId")
            .get()
            .addHeader("apikey", supabaseKey)
            .addHeader("Authorization", "Bearer $authToken")
            .build()

        return client.newCall(request).execute()
    }
    
    // Get or create friend attributes using our SQL function
    fun getOrCreateFriendAttributes(friendId: String, authToken: String): Response {
        Log.d("SupabaseClient", "Getting attributes for friend using SQL function: $friendId")
        
        // Use the SQL function we created in Supabase
        val rpcRequest = Request.Builder()
            .url("$supabaseUrl/rest/v1/rpc/get_friend_details")
            .post("""{"friend_id": "$friendId"}""".toRequestBody("application/json".toMediaType()))
            .addHeader("apikey", supabaseKey)
            .addHeader("Authorization", "Bearer $authToken")
            .addHeader("Content-Type", "application/json")
            .build()
        
        val response = client.newCall(rpcRequest).execute()
        Log.d("SupabaseClient", "Get friend details SQL function response code: ${response.code}")
        return response
    }
    
    // Get friend's username by user ID - this doesn't work for non-admin users
    fun getFriendUsername(friendId: String, authToken: String): Response {
        val request = Request.Builder()
            .url("$supabaseUrl/auth/v1/admin/users/$friendId")
            .get()
            .addHeader("apikey", supabaseKey)
            .addHeader("Authorization", "Bearer $authToken")
            .build()

        return client.newCall(request).execute()
    }
    
    // Get friendly display name for a friend
    // This uses a public function that should be created in Supabase
    fun getFriendDisplayName(friendId: String, authToken: String): Response {
        Log.d("SupabaseClient", "Getting display name for friend: $friendId")
        val request = Request.Builder()
            .url("$supabaseUrl/rest/v1/rpc/get_user_display_name")
            .post("""{"user_id": "$friendId"}""".toRequestBody("application/json".toMediaType()))
            .addHeader("apikey", supabaseKey)
            .addHeader("Authorization", "Bearer $authToken")
            .addHeader("Content-Type", "application/json")
            .build()

        val response = client.newCall(request).execute()
        Log.d("SupabaseClient", "Get display name response code: ${response.code}")

        return response
    }
    
    // Get multiple friends' details at once
    fun getFriendsDetails(friendIds: List<String>, authToken: String): Response {
        // Create a comma-separated list of UUIDs in parentheses for the SQL IN clause
        val friendIdsFormatted = friendIds.joinToString(",") { "\"$it\"" }
        
        val request = Request.Builder()
            .url("$supabaseUrl/rest/v1/rpc/get_friends_details")
            .post("""{"friend_ids": [$friendIdsFormatted]}""".toRequestBody("application/json".toMediaType()))
            .addHeader("apikey", supabaseKey)
            .addHeader("Authorization", "Bearer $authToken")
            .addHeader("Content-Type", "application/json")
            .build()

        return client.newCall(request).execute()
    }

    // Unlock a new reward
    fun unlockReward(userId: String, rewardId: String, authToken: String): Response {
        // Use the RPC function to unlock a reward
        val json = """{"input_user_id": "$userId", "input_reward_id": "$rewardId"}"""
        val requestBody = json.toRequestBody("application/json".toMediaType())
        
        val request = Request.Builder()
            .url("$supabaseUrl/rest/v1/rpc/unlock_user_reward")
            .post(requestBody)
            .addHeader("apikey", supabaseKey)
            .addHeader("Authorization", "Bearer $authToken")
            .addHeader("Content-Type", "application/json")
            .addHeader("Prefer", "return=minimal")
            .build()
            
        return client.newCall(request).execute()
    }

    // Get all friends for a user with forced refresh
    suspend fun getAllFriends(authToken: String): Response {
        val request = Request.Builder()
            .url("$supabaseUrl/rest/v1/rpc/get_all_friends")
            .post("{}".toRequestBody("application/json".toMediaType()))
            .addHeader("apikey", supabaseKey)
            .addHeader("Authorization", "Bearer $authToken")
            .addHeader("Content-Type", "application/json")
            .addHeader("Cache-Control", "no-cache, no-store")
            .addHeader("Pragma", "no-cache")
            .build()
            
        return withContext(Dispatchers.IO) {
            // Create a new client with no cache to ensure we get fresh data
            val freshClient = OkHttpClient.Builder()
                .cache(null)
                .build()
                
            freshClient.newCall(request).execute()
        }
    }

    // Simple direct query to get friendships
    suspend fun queryFriendships(authToken: String): Response {
        // Get user ID from token claim
        val userIdFromToken = getUserIdFromToken(authToken)
        
        // Add timestamp to prevent caching
        val timestamp = System.currentTimeMillis()
        
        val request = Request.Builder()
            .url("$supabaseUrl/rest/v1/friendships?select=friend_id&user_id=eq.$userIdFromToken&order=created_at.desc&_=$timestamp")
            .get()
            .addHeader("apikey", supabaseKey)
            .addHeader("Authorization", "Bearer $authToken")
            .addHeader("Content-Type", "application/json")
            .addHeader("Cache-Control", "no-cache, no-store")
            .addHeader("Pragma", "no-cache")
            .build()
            
        return withContext(Dispatchers.IO) {
            val freshClient = OkHttpClient.Builder()
                .cache(null)
                .build()
                
            freshClient.newCall(request).execute()
        }
    }
    
    // Extract user ID from JWT token
    private fun getUserIdFromToken(token: String): String {
        val parts = token.split(".")
        if (parts.size != 3) return ""
        
        try {
            val payload = parts[1]
            val decoded = android.util.Base64.decode(payload, android.util.Base64.URL_SAFE)
            val payloadJson = String(decoded)
            val jsonObject = JSONObject(payloadJson)
            return jsonObject.optString("sub", "")
        } catch (e: Exception) {
            Log.e("SupabaseClient", "Error decoding token: ${e.message}")
            return ""
        }
    }

    // Get friend IDs for the current user
    suspend fun getUserFriendIds(authToken: String): Response {
        val request = Request.Builder()
            .url("$supabaseUrl/rest/v1/rpc/get_user_friends")
            .post("{}".toRequestBody("application/json".toMediaType()))
            .addHeader("apikey", supabaseKey)
            .addHeader("Authorization", "Bearer $authToken")
            .addHeader("Content-Type", "application/json")
            .addHeader("Cache-Control", "no-cache, no-store")
            .addHeader("Pragma", "no-cache")
            .build()
            
        return withContext(Dispatchers.IO) {
            val freshClient = OkHttpClient.Builder()
                .cache(null)
                .build()
                
            freshClient.newCall(request).execute()
        }
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
