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
import com.nhlstenden.appdev.core.utils.UserManager

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

    fun updateUserPoints(userId: String, points: Int, authToken: String): Response {
        val json = """{"points": $points}"""
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

    fun updateUserBellPeppers(userId: String, bellPeppers: Int, authToken: String): Response {
        val json = """{"bell_peppers": $bellPeppers}"""
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

    fun updateUserXp(userId: String, xp: Int, authToken: String): Response {
        val json = """{"xp": $xp}"""
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

    fun getUserUnlockedAchievements(userId: String, authToken: String): Response {
        if (userId.isBlank() || userId == "null") {
            Log.e("SupabaseClient", "Invalid userId provided for unlocked achievements: $userId")
            throw IllegalArgumentException("Invalid userId provided")
        }
        
        // Use database function to get achievements with proper RLS handling
        val rpcRequest = Request.Builder()
            .url("$supabaseUrl/rest/v1/rpc/get_user_achievements")
            .post("""{"user_uuid": "$userId"}""".toRequestBody("application/json".toMediaType()))
            .addHeader("apikey", supabaseKey)
            .addHeader("Authorization", "Bearer $authToken")
            .addHeader("Content-Type", "application/json")
            .build()
        
        val response = client.newCall(rpcRequest).execute()
        Log.d("SupabaseClient", "Get user achievements function response code: ${response.code}")
        return response
    }

    fun unlockAchievement(userId: String, achievementId: Int, authToken: String): Response {
        // Use database function to unlock achievement with proper RLS handling
        val rpcRequest = Request.Builder()
            .url("$supabaseUrl/rest/v1/rpc/unlock_achievement")
            .post("""{"user_uuid": "$userId", "ach_id": $achievementId}""".toRequestBody("application/json".toMediaType()))
            .addHeader("apikey", supabaseKey)
            .addHeader("Authorization", "Bearer $authToken")
            .addHeader("Content-Type", "application/json")
            .build()
        
        val response = client.newCall(rpcRequest).execute()
        Log.d("SupabaseClient", "Unlock achievement function response code: ${response.code}")
        return response
    }

    fun unlockAchievementIfNotExists(userId: String, achievementId: Int, title: String, authToken: String): Response {
        // Use database function to unlock achievement only if not already unlocked
        val rpcRequest = Request.Builder()
            .url("$supabaseUrl/rest/v1/rpc/unlock_achievement_if_not_exists")
            .post("""{"user_uuid": "$userId", "ach_id": $achievementId, "ach_title": "$title"}""".toRequestBody("application/json".toMediaType()))
            .addHeader("apikey", supabaseKey)
            .addHeader("Authorization", "Bearer $authToken")
            .addHeader("Content-Type", "application/json")
            .build()
        
        val response = client.newCall(rpcRequest).execute()
        Log.d("SupabaseClient", "Unlock achievement if not exists function response code: ${response.code}")
        return response
    }

    fun checkStreakAchievement(userId: String, authToken: String): Response {
        // Use database function to check and unlock streak achievement
        val rpcRequest = Request.Builder()
            .url("$supabaseUrl/rest/v1/rpc/check_streak_achievement")
            .post("""{"user_uuid": "$userId"}""".toRequestBody("application/json".toMediaType()))
            .addHeader("apikey", supabaseKey)
            .addHeader("Authorization", "Bearer $authToken")
            .addHeader("Content-Type", "application/json")
            .build()
        
        val response = client.newCall(rpcRequest).execute()
        Log.d("SupabaseClient", "Check streak achievement function response code: ${response.code}")
        return response
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

    suspend fun createProfile(authToken: String, displayName: String = "", email: String = ""): JSONObject {
        val userId = getUserIdFromToken(authToken)
        
        val profileJson = JSONObject()
        profileJson.put("id", userId)
        profileJson.put("display_name", displayName)
        profileJson.put("email", email)
        profileJson.put("created_at", "now()")
        profileJson.put("updated_at", "now()")
        
        val requestBody = profileJson.toString().toRequestBody("application/json".toMediaType())
        val request = Request.Builder()
            .url("$supabaseUrl/rest/v1/profile")
            .post(requestBody)
            .addHeader("apikey", supabaseKey)
            .addHeader("Authorization", "Bearer $authToken")
            .addHeader("Content-Type", "application/json")
            .addHeader("Prefer", "return=representation")
            .build()
            
        val response = withContext(Dispatchers.IO) { client.newCall(request).execute() }
        if (!response.isSuccessful) {
            val errorBody = response.body?.string()
            Log.e("SupabaseClient", "Profile creation failed with response: $errorBody")
            throw RuntimeException(errorBody ?: "Profile creation failed with code ${response.code}")
        }
        
        val body = response.body?.string() ?: throw RuntimeException("No response body")
        val arr = org.json.JSONArray(body)
        if (arr.length() == 0) throw RuntimeException("Profile creation failed")
        return arr.getJSONObject(0)
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
    
    suspend fun fetchProfileOrCreate(authToken: String, displayName: String = "", email: String = ""): JSONObject {
        return try {
            fetchProfile(authToken)
        } catch (e: Exception) {
            if (e.message?.contains("Profile not found") == true) {
                Log.d("SupabaseClient", "Profile not found, creating new profile")
                createProfile(authToken, displayName, email)
            } else {
                throw e
            }
        }
    }

    suspend fun createUserAttributes(authToken: String): JSONObject {
        val userId = getUserIdFromToken(authToken)
        
        val attributesJson = JSONObject()
        attributesJson.put("id", userId)
        attributesJson.put("xp", 0)
        attributesJson.put("bell_peppers", 0)
        attributesJson.put("streak", 0)
        attributesJson.put("last_task_date", "")
        attributesJson.put("opened_daily_at", "")
        
        val requestBody = attributesJson.toString().toRequestBody("application/json".toMediaType())
        val request = Request.Builder()
            .url("$supabaseUrl/rest/v1/user_attributes")
            .post(requestBody)
            .addHeader("apikey", supabaseKey)
            .addHeader("Authorization", "Bearer $authToken")
            .addHeader("Content-Type", "application/json")
            .addHeader("Prefer", "return=representation")
            .build()
            
        val response = withContext(Dispatchers.IO) { client.newCall(request).execute() }
        if (!response.isSuccessful) {
            val errorBody = response.body?.string()
            Log.e("SupabaseClient", "User attributes creation failed with response: $errorBody")
            throw RuntimeException(errorBody ?: "User attributes creation failed with code ${response.code}")
        }
        
        val body = response.body?.string() ?: throw RuntimeException("No response body")
        val arr = org.json.JSONArray(body)
        if (arr.length() == 0) throw RuntimeException("User attributes creation failed")
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
    
    suspend fun fetchUserAttributesOrCreate(authToken: String): JSONObject {
        return try {
            fetchUserAttributes(authToken)
        } catch (e: Exception) {
            if (e.message?.contains("User attributes not found") == true) {
                Log.d("SupabaseClient", "User attributes not found, creating new user attributes")
                createUserAttributes(authToken)
            } else {
                throw e
            }
        }
    }

    // Convenience methods that use UserManager directly
    suspend fun addFriendWithCurrentUser(friendId: String): Response {
        val user = UserManager.getCurrentUser()
            ?: throw IllegalStateException("No user logged in")
        return createMutualFriendship(friendId, user.authToken)
    }

    suspend fun getAllFriendsForCurrentUser(): Response {
        val user = UserManager.getCurrentUser()
            ?: throw IllegalStateException("No user logged in")
        return getAllFriends(user.authToken)
    }

    fun getCurrentUserId(): String? {
        return UserManager.getCurrentUser()?.id?.toString()
    }

    suspend fun removeFriend(friendId: String, authToken: String): Response {
        val json = """{"friend_id": "${friendId.trim()}"}"""
        val requestBody = json.toRequestBody("application/json".toMediaType())

        val request = Request.Builder()
            .url("$supabaseUrl/rest/v1/rpc/remove_friend")
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

    suspend fun removeFriendWithCurrentUser(friendId: String): Response {
        val user = UserManager.getCurrentUser()
            ?: throw IllegalStateException("No user logged in")
        return removeFriend(friendId, user.authToken)
    }

    // Convenience methods for profile operations using UserManager
    suspend fun getProfileForCurrentUser(): JSONObject {
        val user = UserManager.getCurrentUser()
            ?: throw IllegalStateException("No user logged in")
        return fetchProfile(user.authToken)
    }

    suspend fun getUserAttributesForCurrentUser(): JSONObject {
        val user = UserManager.getCurrentUser()
            ?: throw IllegalStateException("No user logged in")
        return fetchUserAttributes(user.authToken)
    }

    suspend fun updateProfileForCurrentUser(
        displayName: String? = null,
        bio: String? = null,
        profilePicture: String? = null
    ): JSONObject {
        val user = UserManager.getCurrentUser()
            ?: throw IllegalStateException("No user logged in")
        return updateProfile(user.authToken, displayName, bio, profilePicture)
    }

    suspend fun updateProfilePictureForCurrentUser(profilePicture: String): Response {
        val user = UserManager.getCurrentUser()
            ?: throw IllegalStateException("No user logged in")
        return updateProfilePicture(user.id.toString(), profilePicture, user.authToken)
    }

    suspend fun getUserUnlockedRewardsForCurrentUser(): Response {
        val user = UserManager.getCurrentUser()
            ?: throw IllegalStateException("No user logged in")
        return getUserUnlockedRewards(user.id.toString(), user.authToken)
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

    suspend fun getUserProgressResponse(userId: String, authToken: String): Response {
        val url = "$supabaseUrl/rest/v1/user_progress?user_id=eq.$userId"
        Log.d("SupabaseClient", "getUserProgress URL: $url")
        Log.d("SupabaseClient", "getUserProgress for userId: $userId")
        
        val request = Request.Builder()
            .url(url)
            .get()
            .addHeader("apikey", supabaseKey)
            .addHeader("Authorization", "Bearer $authToken")
            .addHeader("Content-Type", "application/json")
            .addHeader("Prefer", "return=minimal")
            .build()

        return withContext(Dispatchers.IO) { client.newCall(request).execute() }
    }

    suspend fun getUserProgress(userId: String, authToken: String): JSONArray {
        val response = getUserProgressResponse(userId, authToken)
        val bodyString = response.body?.string()
        Log.d("SupabaseClient", "getUserProgress for userId=$userId: code=${response.code}, body=$bodyString")
        
        if (!response.isSuccessful) {
            Log.e("SupabaseClient", "Progress fetching failed with response: $bodyString")
            throw RuntimeException(bodyString ?: "Progress fetching failed with code ${response.code}")
        }

        val arr = org.json.JSONArray(bodyString)
        Log.d("SupabaseClient", "Parsed progress array length: ${arr.length()}")
        for (i in 0 until arr.length()) {
            val obj = arr.getJSONObject(i)
            Log.d("SupabaseClient", "Progress entry $i: $obj")
        }
        
        // Also try a broader query to see if there's any progress data for this user
        if (arr.length() == 0) {
            Log.d("SupabaseClient", "No progress found with exact query, trying broader search...")
            val broadRequest = Request.Builder()
                .url("$supabaseUrl/rest/v1/user_progress?select=*")
                .get()
                .addHeader("apikey", supabaseKey)
                .addHeader("Authorization", "Bearer $authToken")
                .addHeader("Content-Type", "application/json")
                .build()
            
            val broadResponse = withContext(Dispatchers.IO) { client.newCall(broadRequest).execute() }
            val broadBody = broadResponse.body?.string()
            if (broadResponse.isSuccessful) {
                val broadArr = org.json.JSONArray(broadBody)
                Log.d("SupabaseClient", "Broad search found ${broadArr.length()} total progress records")
                for (i in 0 until minOf(broadArr.length(), 5)) { // Show first 5 records
                    val obj = broadArr.getJSONObject(i)
                    Log.d("SupabaseClient", "Sample progress record $i: $obj")
                }
                
                // Check if any records match our user ID
                var foundMatches = 0
                for (i in 0 until broadArr.length()) {
                    val obj = broadArr.getJSONObject(i)
                    val recordUserId = obj.optString("user_id", "")
                    if (recordUserId == userId) {
                        foundMatches++
                        Log.d("SupabaseClient", "Found matching record for user $userId: $obj")
                    }
                }
                Log.d("SupabaseClient", "Found $foundMatches matching records for user $userId")
            }
        }
        
        return arr
    }

    // Convenience method for getting friend progress with current user's auth token
    suspend fun getFriendProgressForCurrentUser(friendId: String): Result<JSONArray> {
        return try {
            val currentUserId = getCurrentUserId()
                ?: return Result.failure(Exception("User not logged in"))
            
            val authToken = UserManager.getCurrentUser()?.authToken
                ?: return Result.failure(Exception("No auth token available"))
            
            val progressArray = getUserProgress(friendId, authToken)
            Result.success(progressArray)
        } catch (e: Exception) {
            Log.e("SupabaseClient", "Error getting friend progress", e)
            Result.failure(e)
        }
    }

    // Get friend's profile data including points, streak, etc.
    suspend fun getFriendProfileForCurrentUser(friendId: String): Result<org.json.JSONObject> {
        return try {
            val authToken = UserManager.getCurrentUser()?.authToken
                ?: return Result.failure(Exception("No auth token available"))
            
            // Fetch from profile table which has display_name, bio, profile_picture, etc.
            val profileRequest = Request.Builder()
                .url("$supabaseUrl/rest/v1/profile?select=*&id=eq.$friendId")
                .get()
                .addHeader("apikey", supabaseKey)
                .addHeader("Authorization", "Bearer $authToken")
                .addHeader("Content-Type", "application/json")
                .build()
            
            val profileResponse = withContext(Dispatchers.IO) { client.newCall(profileRequest).execute() }
            val profileBody = profileResponse.body?.string()
            Log.d("SupabaseClient", "getFriendProfile: code=${profileResponse.code}, body=$profileBody")
            
            if (profileResponse.isSuccessful && !profileBody.isNullOrEmpty()) {
                val profileArr = org.json.JSONArray(profileBody)
                if (profileArr.length() > 0) {
                    val profileData = profileArr.getJSONObject(0)
                    
                    // Also fetch user_attributes for points, streak, etc.
                    val attributesRequest = Request.Builder()
                        .url("$supabaseUrl/rest/v1/user_attributes?select=*&id=eq.$friendId")
                        .get()
                        .addHeader("apikey", supabaseKey)
                        .addHeader("Authorization", "Bearer $authToken")
                        .addHeader("Content-Type", "application/json")
                        .build()
                    
                    val attributesResponse = withContext(Dispatchers.IO) { client.newCall(attributesRequest).execute() }
                    val attributesBody = attributesResponse.body?.string()
                    Log.d("SupabaseClient", "getFriendAttributes: code=${attributesResponse.code}, body=$attributesBody")
                    
                    // Merge profile and attributes data
                    val mergedData = org.json.JSONObject()
                    
                    // Copy profile data (display_name, bio, profile_picture, etc.)
                    profileData.keys().forEach { key ->
                        mergedData.put(key, profileData.get(key))
                    }
                    
                    // Copy attributes data if available (includes last_task_date)
                    if (attributesResponse.isSuccessful && !attributesBody.isNullOrEmpty()) {
                        val attributesArr = org.json.JSONArray(attributesBody)
                        if (attributesArr.length() > 0) {
                            val attributesData = attributesArr.getJSONObject(0)
                            attributesData.keys().forEach { key ->
                                mergedData.put(key, attributesData.get(key))
                            }
                        }
                    }
                    
                    Result.success(mergedData)
                } else {
                    Result.failure(Exception("Friend profile not found"))
                }
            } else {
                Result.failure(Exception("Failed to fetch friend profile: ${profileResponse.code}"))
            }
        } catch (e: Exception) {
            Log.e("SupabaseClient", "Error getting friend profile", e)
            Result.failure(e)
        }
    }

    fun updateUserProgress(userId: String, taskId: String, newProgress: Int, authToken: String): Response {
        val courseId = taskId.substringBefore("_")
        val json = """{"progress": $newProgress}"""
        val requestBody = json.toRequestBody("application/json".toMediaType())
        val request = Request.Builder()
            .url("$supabaseUrl/rest/v1/user_progress?user_id=eq.$userId&course_id=eq.$courseId")
            .patch(requestBody)
            .addHeader("apikey", supabaseKey)
            .addHeader("Authorization", "Bearer $authToken")
            .addHeader("Content-Type", "application/json")
            .addHeader("Prefer", "return=minimal")
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

    fun insertUserProgress(userId: String, courseId: String, progress: Int, authToken: String): Response {
        val json = """{"user_id": "$userId", "course_id": "$courseId", "progress": $progress}"""
        val requestBody = json.toRequestBody("application/json".toMediaType())
        val request = Request.Builder()
            .url("$supabaseUrl/rest/v1/user_progress")
            .post(requestBody)
            .addHeader("apikey", supabaseKey)
            .addHeader("Authorization", "Bearer $authToken")
            .addHeader("Content-Type", "application/json")
            .addHeader("Prefer", "return=minimal")
            .build()
        return client.newCall(request).execute()
    }

    fun insertUserProgressRPC(userId: String, courseId: String, progress: Int, authToken: String): Response {
        val json = """{"_user_id": "$userId", "_course_id": "$courseId", "_progress": $progress}"""
        Log.d("SupabaseClient", "insertUserProgressRPC payload: $json")
        val requestBody = json.toRequestBody("application/json".toMediaType())
        val request = Request.Builder()
            .url("$supabaseUrl/rest/v1/rpc/insert_user_progress")
            .post(requestBody)
            .addHeader("apikey", supabaseKey)
            .addHeader("Authorization", "Bearer $authToken")
            .addHeader("Content-Type", "application/json")
            .addHeader("Prefer", "return=minimal")
            .build()
        val response = client.newCall(request).execute()
        val responseBodyString = response.body?.string()
        Log.d("SupabaseClient", "insertUserProgressRPC response: code=${response.code}, body=$responseBodyString")
        // Do not use response.body?.string() again after this!
        return response
    }

    suspend fun createUserProgress(userId: String, courseId: String, progress: Int, authToken: String): Response {
        return withContext(Dispatchers.IO) {
            val url = "$supabaseUrl/rest/v1/rpc/insert_user_progress"
            val jsonObject = JSONObject().apply {
                put("_user_id", userId)
                put("_course_id", courseId)
                put("_progress", progress)
            }

            val client = OkHttpClient.Builder()
                .addInterceptor { chain ->
                    val request = chain.request().newBuilder()
                        .addHeader("apikey", supabaseKey)
                        .addHeader("Authorization", "Bearer $authToken")
                        .addHeader("Content-Type", "application/json")
                        .addHeader("Prefer", "return=minimal")
                        .build()
                    chain.proceed(request)
                }
                .build()

            val request = Request.Builder()
                .url(url)
                .post(jsonObject.toString().toRequestBody("application/json".toMediaType()))
                .build()

            client.newCall(request).execute()
        }
    }

    // Get friend's progress using RPC to bypass RLS
    suspend fun getFriendProgressViaRPC(friendId: String, authToken: String): Result<org.json.JSONArray> {
        return try {
            val request = Request.Builder()
                .url("$supabaseUrl/rest/v1/rpc/get_friend_progress")
                .post("""{"friend_user_id": "$friendId"}""".toRequestBody("application/json".toMediaType()))
                .addHeader("apikey", supabaseKey)
                .addHeader("Authorization", "Bearer $authToken")
                .addHeader("Content-Type", "application/json")
                .build()
            
            val response = withContext(Dispatchers.IO) { client.newCall(request).execute() }
            val responseBody = response.body?.string()
            Log.d("SupabaseClient", "getFriendProgressViaRPC: code=${response.code}, body=$responseBody")
            
            if (response.isSuccessful && !responseBody.isNullOrEmpty()) {
                val progressArray = org.json.JSONArray(responseBody)
                Result.success(progressArray)
            } else {
                Log.w("SupabaseClient", "RPC failed, falling back to direct query")
                // Fallback to direct query
                val directProgress = getUserProgress(friendId, authToken)
                Result.success(directProgress)
            }
        } catch (e: Exception) {
            Log.e("SupabaseClient", "Error in getFriendProgressViaRPC", e)
            // Fallback to direct query
            try {
                val directProgress = getUserProgress(friendId, authToken)
                Result.success(directProgress)
            } catch (fallbackError: Exception) {
                Result.failure(fallbackError)
            }
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
