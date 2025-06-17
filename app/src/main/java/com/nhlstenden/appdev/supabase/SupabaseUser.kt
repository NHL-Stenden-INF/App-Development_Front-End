package com.nhlstenden.appdev.supabase

import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.Response
import okhttp3.ResponseBody.Companion.toResponseBody
import org.json.JSONArray
import org.json.JSONObject
import com.nhlstenden.appdev.core.utils.UserManager

suspend fun SupabaseClient.getUserAttributes(userId: String, authToken: String): Response {
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
        okhttp3.Response.Builder()
            .request(request)
            .protocol(response.protocol)
            .code(response.code)
            .message(response.message)
            .body(responseBody?.toResponseBody("application/json".toMediaType()))
            .build()
    }
}

fun SupabaseClient.updateUserPoints(userId: String, points: Int, authToken: String): Response {
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

fun SupabaseClient.updateUserBellPeppers(userId: String, bellPeppers: Int, authToken: String): Response {
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

fun SupabaseClient.updateUserXp(userId: String, xp: Int, authToken: String): Response {
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

fun SupabaseClient.updateUserOpenedDaily(userId: String, date: String, authToken: String): Response {
    val json = """{"opened_daily_at": \"$date\"}"""
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

fun SupabaseClient.updateProfilePicture(userId: String, profilePicture: String, authToken: String): Response {
    Log.d("SupabaseClient", "Attempting to update profile picture for user: $userId")
    return try {
        val patchJson = JSONObject().apply { put("profile_picture", profilePicture) }
        val patchRequestBody = patchJson.toString().toRequestBody("application/json".toMediaType())
        val patchRequest = Request.Builder()
            .url("$supabaseUrl/rest/v1/user_attributes?id=eq.$userId")
            .patch(patchRequestBody)
            .addHeader("apikey", supabaseKey)
            .addHeader("Authorization", "Bearer $authToken")
            .addHeader("Content-Type", "application/json")
            .addHeader("Prefer", "return=minimal")
            .build()
        client.newCall(patchRequest).execute()
    } catch (e: Exception) {
        Log.e("SupabaseClient", "Error updating profile picture", e)
        throw e
    }
}

fun SupabaseClient.getUserUnlockedRewards(userId: String, authToken: String): Response {
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

suspend fun SupabaseClient.createProfile(authToken: String, displayName: String = "", email: String = ""): JSONObject {
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
    val arr = JSONArray(body)
    if (arr.length() == 0) throw RuntimeException("Profile creation failed")
    return arr.getJSONObject(0)
}

suspend fun SupabaseClient.fetchProfile(authToken: String): JSONObject {
    val request = Request.Builder()
        .url("$supabaseUrl/rest/v1/profile?select=*&id=eq.${getUserIdFromToken(authToken)}")
        .get()
        .addHeader("apikey", supabaseKey)
        .addHeader("Authorization", "Bearer $authToken")
        .build()
    val response = withContext(Dispatchers.IO) { client.newCall(request).execute() }
    if (!response.isSuccessful) throw RuntimeException(response.body?.string())
    val body = response.body?.string() ?: throw RuntimeException("No response body")
    val arr = JSONArray(body)
    if (arr.length() == 0) throw RuntimeException("Profile not found")
    return arr.getJSONObject(0)
}

suspend fun SupabaseClient.fetchProfileOrCreate(authToken: String, displayName: String = "", email: String = ""): JSONObject {
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

suspend fun SupabaseClient.createUserAttributes(authToken: String): JSONObject {
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
    val arr = JSONArray(body)
    if (arr.length() == 0) throw RuntimeException("User attributes creation failed")
    return arr.getJSONObject(0)
}

suspend fun SupabaseClient.fetchUserAttributes(authToken: String): JSONObject {
    val request = Request.Builder()
        .url("$supabaseUrl/rest/v1/user_attributes?select=*&id=eq.${getUserIdFromToken(authToken)}")
        .get()
        .addHeader("apikey", supabaseKey)
        .addHeader("Authorization", "Bearer $authToken")
        .build()
    val response = withContext(Dispatchers.IO) { client.newCall(request).execute() }
    if (!response.isSuccessful) throw RuntimeException(response.body?.string())
    val body = response.body?.string() ?: throw RuntimeException("No response body")
    val arr = JSONArray(body)
    if (arr.length() == 0) throw RuntimeException("User attributes not found")
    return arr.getJSONObject(0)
}

suspend fun SupabaseClient.fetchUserAttributesOrCreate(authToken: String): JSONObject {
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

suspend fun SupabaseClient.getProfileForCurrentUser(): JSONObject {
    val user = UserManager.getCurrentUser() ?: throw IllegalStateException("No user logged in")
    return fetchProfile(user.authToken)
}

suspend fun SupabaseClient.getUserAttributesForCurrentUser(): JSONObject {
    val user = UserManager.getCurrentUser() ?: throw IllegalStateException("No user logged in")
    return fetchUserAttributes(user.authToken)
}

suspend fun SupabaseClient.updateProfileForCurrentUser(
    displayName: String? = null,
    bio: String? = null,
    profilePicture: String? = null
): JSONObject {
    val user = UserManager.getCurrentUser() ?: throw IllegalStateException("No user logged in")
    return updateProfile(user.authToken, displayName, bio, profilePicture)
}

suspend fun SupabaseClient.updateProfilePictureForCurrentUser(profilePicture: String): Response {
    val user = UserManager.getCurrentUser() ?: throw IllegalStateException("No user logged in")
    return updateProfilePicture(user.id.toString(), profilePicture, user.authToken)
}

suspend fun SupabaseClient.getUserUnlockedRewardsForCurrentUser(): Response {
    val user = UserManager.getCurrentUser() ?: throw IllegalStateException("No user logged in")
    return getUserUnlockedRewards(user.id.toString(), user.authToken)
}

suspend fun SupabaseClient.updateProfile(
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
    val arr = JSONArray(body)
    if (arr.length() == 0) throw RuntimeException("Profile update failed")
    return arr.getJSONObject(0)
}

fun SupabaseClient.updateUserLastTaskDate(userID: String, lastTaskDate: String, authToken: String): Response {
    val url = "$supabaseUrl/rest/v1/user_attributes?id=eq.$userID"
    val requestBody = JSONObject().apply { put("last_task_date", lastTaskDate) }.toString()

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

fun SupabaseClient.updateUserStreak(userId: String, streak: Int, authToken: String): Response {
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