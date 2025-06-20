package com.nhlstenden.appdev.supabase

import android.os.Build
import android.util.Log
import androidx.annotation.RequiresApi
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.Response
import org.json.JSONArray
import org.json.JSONObject
import com.nhlstenden.appdev.core.utils.UserManager
import com.nhlstenden.appdev.core.utils.TaskToCourseMapper
import java.time.LocalDate

private val TAG = "SupabaseProgress"

suspend fun SupabaseClient.getUserProgressResponse(userId: String, authToken: String): Result<Response> {
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

    return try {
        val response = withContext(Dispatchers.IO) { client.newCall(request).execute() }

        if (response.isSuccessful) {
            Result.success(response)
        } else  {
            Result.failure(Exception("Failed to fetch user progress response: ${response.message}"))
        }
    } catch (e: Exception) {
        Log.e(TAG, "Error with getting user progress response")
        Result.failure(e)
    }
}

suspend fun SupabaseClient.getUserProgress(userId: String, authToken: String): Result<JSONArray> {
    return try {
        val responseResult = getUserProgressResponse(userId, authToken)
        val response = responseResult.getOrNull()

        if (response == null || !response.isSuccessful) {
            val errorBody = response?.body?.string() ?: "Unknown error"
            Log.e(TAG, "Error fetching user progress: $errorBody")
            return Result.failure(RuntimeException("Progress fetching failed: $errorBody"))
        }

        val bodyString = response.body?.string()
        Log.d(
            "SupabaseClient",
            "getUserProgress for userId=$userId: code=${response.code}, body=$bodyString"
        )

        val arr = JSONArray(bodyString)
        Log.d(TAG, "Parsed progress array length: ${arr.length()}")

        if (arr.length() == 0) {
            Log.d(TAG, "No progress found with exact query, trying broader search...")

            val broadRequest = Request.Builder()
                .url("$supabaseUrl/rest/v1/user_progress?select=*")
                .get()
                .addHeader("apikey", supabaseKey)
                .addHeader("Authorization", "Bearer $authToken")
                .addHeader("Content-Type", "application/json")
                .build()

            val broadResponse =
                withContext(Dispatchers.IO) { client.newCall(broadRequest).execute() }
            val broadBody = broadResponse.body?.string()

            if (broadResponse.isSuccessful) {
                val broadArr = JSONArray(broadBody)
                Log.d(TAG, "Broad search found ${broadArr.length()} total progress records")
                for (i in 0 until minOf(broadArr.length(), 5)) {
                    val obj = broadArr.getJSONObject(i)
                    Log.d(TAG, "Sample progress record $i: $obj")
                }

                var foundMatches = 0
                for (i in 0 until broadArr.length()) {
                    val obj = broadArr.getJSONObject(i)
                    val recordUserId = obj.optString("user_id", "")
                    if (recordUserId == userId) {
                        foundMatches++
                        Log.d(TAG, "Found matching record for user $userId: $obj")
                    }
                }
                Log.d(TAG, "Found $foundMatches matching records for user $userId")
                return Result.success(broadArr)
            }
        }
        Result.success(arr)
    } catch (e: Exception) {
        Log.e(TAG, "Error fetching user progress", e)
        Result.failure(e)
    }
}

fun SupabaseClient.updateUserProgress(userId: String, taskId: String, newProgress: Int, authToken: String): Result<Response> {
    val courseId = TaskToCourseMapper.mapTaskIdToCourseId(taskId)
    Log.d(TAG, "updateUserProgress: taskId='$taskId' mapped to courseId='$courseId', progress=$newProgress")
    val json = """{"progress": $newProgress}"""
    val requestBody = json.toRequestBody("application/json".toMediaType())
    val url = "$supabaseUrl/rest/v1/user_progress?user_id=eq.$userId&course_id=eq.$courseId"
    Log.d(TAG, "updateUserProgress: URL=$url, JSON=$json")
    val request = Request.Builder()
        .url(url)
        .patch(requestBody)
        .addHeader("apikey", supabaseKey)
        .addHeader("Authorization", "Bearer $authToken")
        .addHeader("Content-Type", "application/json")
        .addHeader("Prefer", "return=minimal")
        .build()

    return try {
        val response = client.newCall(request).execute()
        val responseBody = response.body?.string()
        Log.d(TAG, "updateUserProgress: response code=${response.code}, body=$responseBody")

        if (response.isSuccessful) {
            Result.success(response)
        } else {
            Log.e(TAG, "updateUserProgress failed: code=${response.code}, message=${response.message}, body=$responseBody")
            Result.failure(Exception("Failed to update user progress: ${response.message}"))
        }
    } catch (e: Exception) {
        Log.e(TAG, "Error with updating progress from user", e)
        Result.failure(e)
    }
}

fun SupabaseClient.insertUserProgress(userId: String, courseId: String, progress: Int, authToken: String): Result<Response> {
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

    return try {
        val response = client.newCall(request).execute()

        if (response.isSuccessful) {
            Result.success(response)
        } else {
            Result.failure(Exception("Failed to insert user progress: ${response.message}"))
        }
    } catch (e: Exception) {
        Log.e(TAG, "Error inserting user progress", e)
        Result.failure(e)
    }
}

suspend fun SupabaseClient.insertUserProgressRPC(userId: String, courseId: String, progress: Int, authToken: String): Result<Response> {
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

    return try {
        val response = withContext(Dispatchers.IO) { client.newCall(request).execute() }
        val responseBodyString = response.body?.string()
        Log.d("SupabaseClient", "insertUserProgressRPC response: code=${response.code}, body=$responseBodyString")

        if (response.isSuccessful) {
            Result.success(response)
        } else {
            Result.failure(Exception("Failed to insert user progress RPC: ${response.message}"))
        }
    } catch (e: Exception) {
        Log.e(TAG, "Error inserting user progress RPC", e)
        Result.failure(e)
    }
}

suspend fun SupabaseClient.createUserProgress(userId: String, courseId: String, progress: Int, authToken: String): Result<Response> {
    return try {
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

        val response = withContext(Dispatchers.IO) { client.newCall(request).execute() }

        if (response.isSuccessful) {
            Result.success(response)
        } else {
            Result.failure(Exception("Failed to create user progress: ${response.message}"))
        }
    } catch (e: Exception) {
        Log.e(TAG, "Error with creating user progress")
        Result.failure(e)
    }
}

suspend fun SupabaseClient.getFriendProgressViaRPC(friendId: String, authToken: String): Result<JSONArray> {
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
        Log.d(TAG, "getFriendProgressViaRPC: code=${response.code}, body=$responseBody")

        if (response.isSuccessful && !responseBody.isNullOrEmpty()) {
            val progressArray = JSONArray(responseBody)
            Result.success(progressArray)
        } else {
            Log.w(TAG, "RPC failed, falling back to direct query: ${response.message}")
            val directProgress = getUserProgress(friendId, authToken)
            directProgress
        }
    } catch (e: Exception) {
        Log.e(TAG, "Error in getFriendProgressViaRPC", e)
        try {
            val directProgress = getUserProgress(friendId, authToken)
            directProgress
        } catch (fallbackError: Exception) {
            Result.failure(fallbackError)
        }
    }
}

suspend fun SupabaseClient.getFriendProgressForCurrentUser(friendId: String): Result<JSONArray> {
    return try {
        val currentUserId = getCurrentUserId() ?: return Result.failure(Exception("User not logged in"))
        val authToken = UserManager.getCurrentUser()?.authToken ?: return Result.failure(Exception("No auth token available"))
        val progressArray = getUserProgress(friendId, authToken)

        progressArray
    } catch (e: Exception) {
        Log.e(TAG, "Error getting friend progress", e)
        Result.failure(e)
    }
}

fun SupabaseClient.calculateLevelFromXp(xp: Long): Result<Int> {
    return try {
        var level = 1
        var requiredXp = 100L
        var totalXp = 0L

        while (xp >= totalXp + requiredXp) {
            totalXp += requiredXp
            level++
            requiredXp = (requiredXp * 1.1).toLong()
        }

        Result.success(level)
    } catch (e: Exception) {
        Log.e(TAG, "Error calculating level from XP", e)
        Result.failure(e)
    }
}

@RequiresApi(Build.VERSION_CODES.O)
suspend fun SupabaseClient.updateUserDailyChallenge(userId: String, authToken: String): Result<Response> {
    val url = "$supabaseUrl/rest/v1/user_attributes?id=eq.$userId"
    val requestBody =
        JSONObject().apply { put("finished_daily_challenge_at", LocalDate.now()) }.toString()
    val request = Request.Builder()
        .url(url)
        .patch(requestBody.toRequestBody("application/json".toMediaType()))
        .addHeader("apikey", supabaseKey)
        .addHeader("Authorization", "Bearer $authToken")
        .addHeader("Content-Type", "application/json")
        .addHeader("Prefer", "return=representation")
        .build()

    return try {
        val response = client.newCall(request).execute()

        if (response.isSuccessful) {
            Result.success(response)
        } else {
            Result.failure(Exception("Failed to update user daily challenge: ${response.message}"))
        }
    } catch (e: Exception) {
        Log.d(TAG, "Error updating daily challenge", e)
        Result.failure(e)
    }
}