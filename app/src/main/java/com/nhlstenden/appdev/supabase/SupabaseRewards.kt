package com.nhlstenden.appdev.supabase

import android.util.Log
import androidx.annotation.RestrictTo
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.Response

private val TAG = "SupabaseRewards"

fun SupabaseClient.unlockReward(userId: String, rewardId: Int, authToken: String): Result<Response> {
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

    return try {
        val response = client.newCall(request).execute()

        if (response.isSuccessful) {
            Result.success(response)
        } else {
            Result.failure(Exception("Failed to unlock reward: ${response.message}"))
        }
    } catch (e: Exception) {
        Log.e(TAG, "Error with unlocking reward")
        Result.failure(e)
    }
}

fun SupabaseClient.getUserUnlockedAchievements(userId: String, authToken: String): Result<Response> {

    return try {
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

        Result.success(response)
    } catch (e: Exception) {
        Log.e(TAG, "Error getting unlocked rewards")
        Result.failure(e)
    }
}

fun SupabaseClient.unlockAchievement(userId: String, achievementId: Int, authToken: String): Result<Response> {
    val rpcRequest = Request.Builder()
        .url("$supabaseUrl/rest/v1/rpc/unlock_achievement")
        .post("""{"user_uuid": "$userId", "ach_id": $achievementId}""".toRequestBody("application/json".toMediaType()))
        .addHeader("apikey", supabaseKey)
        .addHeader("Authorization", "Bearer $authToken")
        .addHeader("Content-Type", "application/json")
        .build()

    return try {
        val response = client.newCall(rpcRequest).execute()
        Log.d("SupabaseClient", "Unlock achievement function response code: ${response.code}")

        Result.success(response)
    } catch (e: Exception) {
        Log.e(TAG, "Error unlocking achievement")
        Result.failure(e)
    }
}

fun SupabaseClient.unlockAchievementIfNotExists(userId: String, achievementId: Int, title: String, authToken: String): Result<Response> {
    val rpcRequest = Request.Builder()
        .url("$supabaseUrl/rest/v1/rpc/unlock_achievement_if_not_exists")
        .post("""{"user_uuid": "$userId", "ach_id": $achievementId, "ach_title": "$title"}""".toRequestBody("application/json".toMediaType()))
        .addHeader("apikey", supabaseKey)
        .addHeader("Authorization", "Bearer $authToken")
        .addHeader("Content-Type", "application/json")
        .build()

    return try {
        val response = client.newCall(rpcRequest).execute()
        Log.d("SupabaseClient", "Unlock achievement if not exists function response code: ${response.code}")

        Result.success(response)
    } catch (e: Exception) {
        Log.e(TAG, "Error unlocking achievement that doesn't exist")
        Result.failure(e)
    }
}

fun SupabaseClient.checkStreakAchievement(userId: String, authToken: String): Result<Response> {
    val rpcRequest = Request.Builder()
        .url("$supabaseUrl/rest/v1/rpc/check_streak_achievement")
        .post("""{"user_uuid": "$userId"}""".toRequestBody("application/json".toMediaType()))
        .addHeader("apikey", supabaseKey)
        .addHeader("Authorization", "Bearer $authToken")
        .addHeader("Content-Type", "application/json")
        .build()

    return try {
        val response = client.newCall(rpcRequest).execute()
        Log.d("SupabaseClient", "Check streak achievement function response code: ${response.code}")

        Result.success(response)
    } catch (e: Exception) {
        Log.e(TAG, "Error in checking stream achievement")
        Result.failure(e)
    }
}