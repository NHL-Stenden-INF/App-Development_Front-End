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
import java.time.LocalDate

suspend fun SupabaseClient.getUserProgressResponse(userId: String, authToken: String): Response {
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

suspend fun SupabaseClient.getUserProgress(userId: String, authToken: String): JSONArray {
    val response = getUserProgressResponse(userId, authToken)
    val bodyString = response.body?.string()
    Log.d("SupabaseClient", "getUserProgress for userId=$userId: code=${response.code}, body=$bodyString")

    if (!response.isSuccessful) {
        Log.e("SupabaseClient", "Progress fetching failed with response: $bodyString")
        throw RuntimeException(bodyString ?: "Progress fetching failed with code ${response.code}")
    }

    val arr = JSONArray(bodyString)
    Log.d("SupabaseClient", "Parsed progress array length: ${arr.length()}")
    for (i in 0 until arr.length()) {
        val obj = arr.getJSONObject(i)
        Log.d("SupabaseClient", "Progress entry $i: $obj")
    }

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
            val broadArr = JSONArray(broadBody)
            Log.d("SupabaseClient", "Broad search found ${broadArr.length()} total progress records")
            for (i in 0 until minOf(broadArr.length(), 5)) {
                val obj = broadArr.getJSONObject(i)
                Log.d("SupabaseClient", "Sample progress record $i: $obj")
            }
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

fun SupabaseClient.updateUserProgress(userId: String, taskId: String, newProgress: Int, authToken: String): Response {
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

fun SupabaseClient.insertUserProgress(userId: String, courseId: String, progress: Int, authToken: String): Response {
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

fun SupabaseClient.insertUserProgressRPC(userId: String, courseId: String, progress: Int, authToken: String): Response {
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
    return response
}

suspend fun SupabaseClient.createUserProgress(userId: String, courseId: String, progress: Int, authToken: String): Response {
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
        Log.d("SupabaseClient", "getFriendProgressViaRPC: code=${response.code}, body=$responseBody")
        if (response.isSuccessful && !responseBody.isNullOrEmpty()) {
            val progressArray = JSONArray(responseBody)
            Result.success(progressArray)
        } else {
            Log.w("SupabaseClient", "RPC failed, falling back to direct query")
            val directProgress = getUserProgress(friendId, authToken)
            Result.success(directProgress)
        }
    } catch (e: Exception) {
        Log.e("SupabaseClient", "Error in getFriendProgressViaRPC", e)
        try {
            val directProgress = getUserProgress(friendId, authToken)
            Result.success(directProgress)
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
        Result.success(progressArray)
    } catch (e: Exception) {
        Log.e("SupabaseClient", "Error getting friend progress", e)
        Result.failure(e)
    }
}

fun SupabaseClient.calculateLevelFromXp(xp: Long): Int {
    var level = 1
    var requiredXp = 100L
    var totalXp = 0L
    while (xp >= totalXp + requiredXp) {
        totalXp += requiredXp
        level++
        requiredXp = (requiredXp * 1.1).toLong()
    }
    return level
}

@RequiresApi(Build.VERSION_CODES.O)
suspend fun SupabaseClient.updateUserDailyChallenge(userId: String, authToken: String): Response {
    val url = "$supabaseUrl/rest/v1/user_attributes?id=eq.$userId"
    val requestBody = JSONObject().apply { put("finished_daily_challenge_at", LocalDate.now()) }.toString()
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