package com.nhlstenden.appdev.supabase

import android.util.Log
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.Response

fun SupabaseClient.unlockReward(userId: String, rewardId: Int, authToken: String): Response {
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

fun SupabaseClient.getUserUnlockedAchievements(userId: String, authToken: String): Response {
    if (userId.isBlank() || userId == "null") {
        Log.e("SupabaseClient", "Invalid userId provided for unlocked achievements: $userId")
        throw IllegalArgumentException("Invalid userId provided")
    }
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

fun SupabaseClient.unlockAchievement(userId: String, achievementId: Int, authToken: String): Response {
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

fun SupabaseClient.unlockAchievementIfNotExists(userId: String, achievementId: Int, title: String, authToken: String): Response {
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

fun SupabaseClient.checkStreakAchievement(userId: String, authToken: String): Response {
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