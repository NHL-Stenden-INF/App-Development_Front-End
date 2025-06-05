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
import okhttp3.ResponseBody.Companion.toResponseBody
import org.json.JSONArray
import org.json.JSONObject
import java.util.UUID

class SupabaseClient() {
    val client = OkHttpClient()

    val supabaseUrl = "https://ggpdstbylyiwkfcucoxd.supabase.co"
    val supabaseKey = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJpc3MiOiJzdXBhYmFzZSIsInJlZiI6ImdncGRzdGJ5bHlpd2tmY3Vjb3hkIiwicm9sZSI6ImFub24iLCJpYXQiOjE3NDczMDg4MzYsImV4cCI6MjA2Mjg4NDgzNn0.2ZGOttYWxBJkNcPmAtJh6dzlm3G6vwpIonEtRvtNNa8"

    suspend fun getUser(email: String, password: String): User {
        // This method is not implemented. Use login, fetchProfile, and fetchUserAttributes instead.
        throw UnsupportedOperationException("Method not implemented")
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

    suspend fun login(email: String, password: String): String {
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

    suspend fun getUserAttributes(userId: String, authToken: String): Response {
        if (userId.isBlank() || userId == "null") {
            Log.e("SupabaseClient", "Invalid userId provided: $userId")
            throw IllegalArgumentException("Invalid userId provided")
        }
        return withContext(Dispatchers.IO) {
            val request = Request.Builder()
                .url("$supabaseUrl/rest/v1/user_attributes?id=eq.$userId")
                .get()
                .addHeader("apikey", supabaseKey)
                .addHeader("Authorization", "Bearer $authToken")
                .build()
            val response = client.newCall(request).execute()
            val responseBody = response.body?.string()
            Log.d("SupabaseClient", "getUserAttributes: userId=$userId, code=${response.code}, body=$responseBody")
            // Re-create the response with the consumed body for the caller
            okhttp3.Response.Builder()
                .request(request)
                .protocol(response.protocol)
                .code(response.code)
                .message(response.message)
                .body(responseBody?.toResponseBody("application/json".toMediaType()))
                .build()
        }
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
            .url("$supabaseUrl/rest/v1/user_attributes?id=eq.$userId")
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
            .url("$supabaseUrl/rest/v1/user_attributes?id=eq.$userId")
            .patch(requestBody)
            .addHeader("apikey", supabaseKey)
            .addHeader("Authorization", "Bearer $authToken")
            .addHeader("Content-Type", "application/json")
            .addHeader("Prefer", "return=minimal")
            .build()
        return client.newCall(request).execute()
    }

    fun updateProfilePicture(userId: String, profilePicture: String, authToken: String): Response {
        Log.d("SupabaseClient", "Attempting to update profile picture for user: $userId")
        try {
            val patchJson = org.json.JSONObject()
            patchJson.put("profile_picture", profilePicture)
            val patchRequestBody = patchJson.toString().toRequestBody("application/json".toMediaType())
            val patchRequest = Request.Builder()
                .url("$supabaseUrl/rest/v1/user_attributes?id=eq.$userId")
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

    fun getUserUnlockedRewards(userId: String, authToken: String): Response {
        if (userId.isBlank() || userId == "null") {
            Log.e("SupabaseClient", "Invalid userId provided for unlocked rewards: $userId")
            throw IllegalArgumentException("Invalid userId provided")
        }
        val request = Request.Builder()
            .url("$supabaseUrl/rest/v1/user_reward?user_id=eq.$userId&select=reward_id,unlocked_at")
            .get()
            .addHeader("apikey", supabaseKey)
            .addHeader("Authorization", "Bearer $authToken")
            .build()
        return client.newCall(request).execute()
    }

    fun getFriendDetails(friendId: String, authToken: String): Response {
        val request = Request.Builder()
            .url("$supabaseUrl/rest/v1/user_attributes?select=id,points,profile_picture&id=eq.$friendId")
            .get()
            .addHeader("apikey", supabaseKey)
            .addHeader("Authorization", "Bearer $authToken")
            .build()
        return client.newCall(request).execute()
    }
    
    fun getOrCreateFriendAttributes(friendId: String, authToken: String): Response {
        Log.d("SupabaseClient", "Getting attributes for friend using SQL function: $friendId")
        
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
    
    fun getFriendUsername(friendId: String, authToken: String): Response {
        val request = Request.Builder()
            .url("$supabaseUrl/auth/v1/admin/users/$friendId")
            .get()
            .addHeader("apikey", supabaseKey)
            .addHeader("Authorization", "Bearer $authToken")
            .build()

        return client.newCall(request).execute()
    }
    
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
    
    fun getFriendsDetails(friendIds: List<String>, authToken: String): Response {
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

    suspend fun createMutualFriendship(targetFriendId: String, authToken: String): Response {
        val json = """{"target_friend_id": "${targetFriendId.trim()}"}"""
        val requestBody = json.toRequestBody("application/json".toMediaType())

        val request = Request.Builder()
            .url("$supabaseUrl/rest/v1/rpc/create_mutual_friendship")
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

    fun unlockReward(userId: String, rewardId: Int, authToken: String): Response {
        val json = """{"user_id": "$userId", "reward_id": $rewardId}"""
        val requestBody = json.toRequestBody("application/json".toMediaType())
        val request = Request.Builder()
            .url("$supabaseUrl/rest/v1/user_reward")
            .post(requestBody)
            .addHeader("apikey", supabaseKey)
            .addHeader("Authorization", "Bearer $authToken")
            .addHeader("Content-Type", "application/json")
            .addHeader("Prefer", "return=minimal")
            .build()
        return client.newCall(request).execute()
    }

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
            val freshClient = OkHttpClient.Builder()
                .cache(null)
                .build()
                
            freshClient.newCall(request).execute()
        }
    }

    suspend fun queryFriendships(authToken: String): Response {
        val userIdFromToken = getUserIdFromToken(authToken)
        
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

    suspend fun register(email: String, password: String, displayName: String): String {
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

    suspend fun fetchProfile(authToken: String): JSONObject {
        val request = Request.Builder()
            .url("$supabaseUrl/rest/v1/profile?select=*&id=eq.${getUserIdFromToken(authToken)}")
            .get()
            .addHeader("apikey", supabaseKey)
            .addHeader("Authorization", "Bearer $authToken")
            .build()
        val response = withContext(Dispatchers.IO) { client.newCall(request).execute() }
        if (!response.isSuccessful) throw RuntimeException(response.body?.string())
        val body = response.body?.string() ?: throw RuntimeException("No response body")
        val arr = org.json.JSONArray(body)
        if (arr.length() == 0) throw RuntimeException("Profile not found")
        return arr.getJSONObject(0)
    }

    suspend fun fetchUserAttributes(authToken: String): JSONObject {
        val request = Request.Builder()
            .url("$supabaseUrl/rest/v1/user_attributes?select=*&id=eq.${getUserIdFromToken(authToken)}")
            .get()
            .addHeader("apikey", supabaseKey)
            .addHeader("Authorization", "Bearer $authToken")
            .build()
        val response = withContext(Dispatchers.IO) { client.newCall(request).execute() }
        if (!response.isSuccessful) throw RuntimeException(response.body?.string())
        val body = response.body?.string() ?: throw RuntimeException("No response body")
        val arr = org.json.JSONArray(body)
        if (arr.length() == 0) throw RuntimeException("User attributes not found")
        return arr.getJSONObject(0)
    }

    suspend fun updateProfile(
        authToken: String,
        displayName: String? = null,
        bio: String? = null,
        profilePicture: String? = null
    ): JSONObject {
        val userId = getUserIdFromToken(authToken)
        
        val updateJson = JSONObject()
        displayName?.let { updateJson.put("display_name", it) }
        bio?.let { updateJson.put("bio", it) }
        profilePicture?.let { updateJson.put("profile_picture", it) }
        updateJson.put("updated_at", "now()")
        
        val requestBody = updateJson.toString().toRequestBody("application/json".toMediaType())
        val request = Request.Builder()
            .url("$supabaseUrl/rest/v1/profile?id=eq.$userId")
            .patch(requestBody)
            .addHeader("apikey", supabaseKey)
            .addHeader("Authorization", "Bearer $authToken")
            .addHeader("Content-Type", "application/json")
            .addHeader("Prefer", "return=representation")
            .build()
            
        val response = withContext(Dispatchers.IO) { client.newCall(request).execute() }
        if (!response.isSuccessful) {
            val errorBody = response.body?.string()
            Log.e("SupabaseClient", "Profile update failed with response: $errorBody")
            throw RuntimeException(errorBody ?: "Profile update failed with code ${response.code}")
        }
        
        val body = response.body?.string() ?: throw RuntimeException("No response body")
        val arr = org.json.JSONArray(body)
        if (arr.length() == 0) throw RuntimeException("Profile update failed")
        return arr.getJSONObject(0)
    }

    fun updateUserLastTaskDate(userID: String, lastTaskDate: String, authToken: String): Response {
        val url = "$supabaseUrl/rest/v1/user_attributes?id=eq.$userID"
        val requestBody = JSONObject().apply {
            put("last_task_date", lastTaskDate)
        }.toString()

        val request = Request.Builder()
            .url(url)
            .patch(requestBody.toRequestBody("application/json".toMediaType()))
            .addHeader("apikey", supabaseKey)
            .addHeader("Authorization", "Bearer $authToken")
            .addHeader("Content-Type", "application/json")
            .addHeader("Prefer", "return=representation")
            .build()

        return client.newCall(request).execute()
    }

    fun updateUserStreak(userId: String, streak: Int, authToken: String): Response {
        val json = """{"streak": $streak}"""
        val requestBody = json.toRequestBody("application/json".toMediaType())
        val request = Request.Builder()
            .url("$supabaseUrl/rest/v1/user_attributes?id=eq.$userId")
            .patch(requestBody)
            .addHeader("apikey", supabaseKey)
            .addHeader("Authorization", "Bearer $authToken")
            .addHeader("Content-Type", "application/json")
            .addHeader("Prefer", "return=minimal")
            .build()
        return client.newCall(request).execute()
    }

    suspend fun getUserProgress(userId: String, authToken: String): JSONArray {
        val request = Request.Builder()
            .url("$supabaseUrl/rest/v1/user_progress?user_id=eq.$userId")
            .get()
            .addHeader("apikey", supabaseKey)
            .addHeader("Authorization", "Bearer $authToken")
            .addHeader("Content-Type", "application/json")
            .addHeader("Prefer", "return=minimal")
            .build()

        val response = withContext(Dispatchers.IO) { client.newCall(request).execute() }

        if (!response.isSuccessful) {
            val errorBody = response.body?.string()
            Log.e("SupabaseClient", "Progress fetching failed with response: $errorBody")
            throw RuntimeException(errorBody ?: "Progress fetching failed with code ${response.code}")
        }

        val body = response.body?.string() ?: throw RuntimeException("No response body")
        val arr = org.json.JSONArray(body)
        if (arr.length() == 0) throw RuntimeException("Progress fetching failed")
        return arr
    }

    fun updateUserProgress(userId: String, taskId: String, newProgress: Int, authToken: String): Response {
        val json = """{"task_id": ${taskId}, "progress": ${newProgress}}"""
        val requestBody = json.toRequestBody("application/json".toMediaType())
        val request = Request.Builder()
            .url("$supabaseUrl/rest/v1/user_progress?id=eq.$userId")
            .post(requestBody)
            .addHeader("apikey", supabaseKey)
            .addHeader("Authorization", "Bearer $authToken")
            .addHeader("Content-Type", "application/json")
            .addHeader("Prefer", "resolution=merge-duplicates,return=minimal")
            .build()

        return client.newCall(request).execute()   
    }
    
    fun calculateLevelFromXp(xp: Long): Int {
        // Example: Level 1 at 0 XP, each level requires 100 * level XP more than previous
        var level = 1
        var requiredXp = 100L
        var totalXp = 0L
        while (xp >= totalXp + requiredXp) {
            totalXp += requiredXp
            level++
            requiredXp = (requiredXp * 1.1).toLong() // 10% more XP per level
        }
        return level
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
