package com.nhlstenden.appdev.supabase

import android.net.http.HttpResponseCache
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
import kotlin.contracts.Returns
import kotlin.math.E

private val TAG = "SupabaseUser"

suspend fun SupabaseClient.getUserAttributes(userId: String, authToken: String): Result<Response> {
    return try {
        if (userId.isBlank() || userId == "null") {
            Log.e("SupabaseClient", "Invalid userId provided: $userId")
            return Result.failure(IllegalArgumentException("Invalid userId provided"))
        }

        withContext(Dispatchers.IO) {
            val request = Request.Builder()
                .url("$supabaseUrl/rest/v1/user_attributes?id=eq.$userId")
                .get()
                .addHeader("apikey", supabaseKey)
                .addHeader("Authorization", "Bearer $authToken")
                .build()

            val response = client.newCall(request).execute()
            Log.d("SupabaseClient", "getUserAttributes: userId=$userId, code=${response.code}")

            if (response.isSuccessful) {
                Result.success(response)
            } else {
                Result.failure(Exception("Error"))
            }
        }
    } catch (e: Exception) {
        Log.e(TAG, "Error with getting user attributes", e)
        Result.failure(e)
    }
}

suspend fun SupabaseClient.updateUserPoints(userId: String, points: Int, authToken: String): Result<Response> {
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
    return try {
        val response = withContext(Dispatchers.IO) { client.newCall(request).execute() }

        if (response.isSuccessful) {
            Result.success(response)
        } else {
            Result.failure(Exception("Failed to update points from user: ${response.message}"))
        }
    } catch (e: Exception) {
        Log.e(TAG, "Error with updating points from user", e)
        Result.failure(e)
    }
}

suspend fun SupabaseClient.updateUserBellPeppers(userId: String, bellPeppers: Int, authToken: String): Result<Response> {
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
    return try {
        val response = withContext(Dispatchers.IO) { client.newCall(request).execute() }

        if (response.isSuccessful) {
            Result.success(response)
        } else {
            Result.failure(Exception("Failed to update user bell peppers: ${response.message}"))
        }
    } catch (e: Exception) {
        Log.e(TAG, "Error updating users bell peppers", e)
        Result.failure(e)
    }
}

suspend fun SupabaseClient.updateUserXp(userId: String, xp: Int, authToken: String): Result<Response> {
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

    return try {
        val response = withContext(Dispatchers.IO) { client.newCall(request).execute() }

        if (response.isSuccessful) {
            Result.success(response)
        } else {
            Result.failure(Exception("Failed to update user XP: ${response.message}"))
        }
    } catch (e: Exception) {
        Log.e(TAG, "Error updating user XP", e)
        Result.failure(e)
    }
}

suspend fun SupabaseClient.updateUserOpenedDaily(userId: String, date: String, authToken: String): Result<Response> {
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

    return try {
        val response = withContext(Dispatchers.IO) { client.newCall(request).execute() }

        if (response.isSuccessful) {
            Result.success(response)
        } else {
            Result.failure(Exception("Failed to update user opened daily"))
        }
    } catch (e: Exception) {
        Log.e(TAG, "Error to update user opened daily", e)
        Result.failure(e)
    }
}

suspend fun SupabaseClient.updateProfilePicture(userId: String, profilePicture: String, authToken: String): Result<Response> {
    return try {
        Log.d("SupabaseClient", "Attempting to update profile picture for user: $userId")
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

        val response = withContext(Dispatchers.IO) { client.newCall(patchRequest).execute() }

        Result.success(response)
    } catch (e: Exception) {
        Log.e(TAG, "Error updating profile picture", e)
        Result.failure(e)
    }
}

suspend fun SupabaseClient.getUserUnlockedRewards(userId: String, authToken: String): Result<Response> {
    return try {
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

        val response = withContext(Dispatchers.IO) { client.newCall(request).execute() }

        Result.success(response)
    } catch (e: Exception) {
        Log.e(TAG, "Error getting user unlocked rewards")
        Result.failure(e)
    }
}

suspend fun SupabaseClient.createProfile(authToken: String, displayName: String = "", email: String = ""): Result<JSONObject> {
    return try {
        val userId = getUserIdFromToken(authToken).getOrThrow()

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

            throw RuntimeException(
                errorBody ?: "Profile creation failed with code ${response.code}"
            )
        }

        val body = response.body?.string() ?: throw RuntimeException("No response body")
        val arr = JSONArray(body)

        if (arr.length() == 0) throw RuntimeException("Failed to create profile: ${response.message}")

        val result = arr.getJSONObject(0)

        Result.success(result)
    } catch (e: Exception) {
        Log.e(TAG, "Error with creating profile", e)
        Result.failure(e)
    }
}

suspend fun SupabaseClient.fetchProfile(authToken: String): Result<JSONObject> {
    return try {
        val userId = getUserIdFromToken(authToken).getOrThrow()
        val request = Request.Builder()
            .url("$supabaseUrl/rest/v1/profile?select=*&id=eq.$userId")
            .get()
            .addHeader("apikey", supabaseKey)
            .addHeader("Authorization", "Bearer $authToken")
            .build()

        val response = withContext(Dispatchers.IO) { client.newCall(request).execute() }

        if (!response.isSuccessful) throw RuntimeException(response.body?.string())

        val body = response.body?.string() ?: throw RuntimeException("No response body")
        val arr = JSONArray(body)

        if (arr.length() == 0) throw RuntimeException("Failed to fetch profile: ${response.message}")

        val result = arr.getJSONObject(0)

        Result.success(result)
    } catch (e: Exception) {
        Log.e(TAG, "Error fetching profile", e)
        Result.failure(e)
    }
}

suspend fun SupabaseClient.fetchProfileOrCreate(authToken: String, displayName: String = "", email: String = ""): Result<JSONObject> {
    return try {
        fetchProfile(authToken)
    } catch (e: Exception) {
        if (e.message?.contains("Failed to fetch profile") == true) {
            Log.d(TAG, "Failed to fetch profile, creating new profile")
            createProfile(authToken, displayName, email)
        } else {
            Result.failure(e)
        }
    }
}

suspend fun SupabaseClient.createUserAttributes(authToken: String): Result<JSONObject> {
    return try {
        val userId = getUserIdFromToken(authToken).getOrThrow()
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
            throw RuntimeException(
                errorBody ?: "User attributes creation failed with code ${response.code}"
            )
        }

        val body = response.body?.string() ?: throw RuntimeException("No response body")
        val arr = JSONArray(body)

        if (arr.length() == 0) throw RuntimeException("Failed to create user attributes: ${response.message}")
        val result =  arr.getJSONObject(0)

        Result.success(result)
    } catch (e: Exception) {
        Log.e(TAG, "Error creating user attributes", e)
        Result.failure(e)
    }
}

suspend fun SupabaseClient.fetchUserAttributes(authToken: String): Result<JSONObject> {
    return try {
        val userId = getUserIdFromToken(authToken).getOrThrow()
        val request = Request.Builder()
            .url("$supabaseUrl/rest/v1/user_attributes?select=*&id=eq.$userId")
            .get()
            .addHeader("apikey", supabaseKey)
            .addHeader("Authorization", "Bearer $authToken")
            .build()

        val response = withContext(Dispatchers.IO) { client.newCall(request).execute() }

        if (!response.isSuccessful) throw RuntimeException(response.body?.string())

        val body = response.body?.string() ?: throw RuntimeException("No response body")
        val arr = JSONArray(body)

        if (arr.length() == 0) throw RuntimeException("Failed to fetch user attributes")

        val result = arr.getJSONObject(0)

        Result.success(result)
    } catch (e: Exception) {
        Log.e(TAG, "Error fetching user attributes")
        Result.failure(e)
    }
}

suspend fun SupabaseClient.fetchUserAttributesOrCreate(authToken: String): Result<JSONObject> {
    return try {
        fetchUserAttributes(authToken)
    } catch (e: Exception) {
        if (e.message?.contains("Failed to fetch user attributes") == true) {
            Log.d(TAG, "Failed to fetch user attributes, creating new user attributes")
            createUserAttributes(authToken)
        } else {
            Result.failure(e)
        }
    }
}

suspend fun SupabaseClient.getProfileForCurrentUser(): Result<JSONObject> {
    return try {
        val user = UserManager.getCurrentUser() ?: throw IllegalStateException("No user logged in")

        return fetchProfile(user.authToken)
    } catch (e: Exception) {
        Log.e(TAG, "Error with getting profile for user", e)
        Result.failure(e)
    }
}

suspend fun SupabaseClient.getUserAttributesForCurrentUser(): Result<JSONObject> {
    return try {
        val user = UserManager.getCurrentUser() ?: throw IllegalStateException("No user logged in")
        val userAttributes = fetchUserAttributes(user.authToken)

        return userAttributes
    } catch (e: Exception) {
        Log.e(TAG, "Error with getting user attributes", e)
        Result.failure(e)
    }
}

suspend fun SupabaseClient.updateProfileForCurrentUser(displayName: String? = null, bio: String? = null, profilePicture: String? = null): Result<JSONObject> {
    return try {
        val user = UserManager.getCurrentUser() ?: throw IllegalStateException("No user logged in")
        val profile = updateProfile(user.authToken, displayName, bio, profilePicture)

        return profile
    } catch (e: Exception) {
        Log.e(TAG, "Error with updating user profile")
        Result.failure(e)
    }
}

suspend fun SupabaseClient.updateProfilePictureForCurrentUser(profilePicture: String): Result<Response> {
    return try {
        val user = UserManager.getCurrentUser() ?: throw IllegalStateException("No user logged in")
        val profile = updateProfilePicture(user.id.toString(), profilePicture, user.authToken)

        return profile
    } catch (e: Exception) {
        Log.e(TAG, "Error with updating profile picture for user", e)
        Result.failure(e)
    }
}

suspend fun SupabaseClient.getUserUnlockedRewardsForCurrentUser(): Result<Response> {
    return try {
        val user = UserManager.getCurrentUser() ?: throw IllegalStateException("No user logged in")
        return getUserUnlockedRewards(user.id.toString(), user.authToken)
    } catch (e: Exception) {
        Log.e(TAG, "Error with getting user unlocked rewards", e)
        Result.failure(e)
    }
}

suspend fun SupabaseClient.updateProfile(authToken: String, displayName: String? = null, bio: String? = null, profilePicture: String? = null): Result<JSONObject> {
    return try {
        val userId = getUserIdFromToken(authToken).getOrThrow()
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

        if (arr.length() == 0) throw RuntimeException("Failed to update profile")

        val profile = arr.getJSONObject(0)

        Result.success(profile)
    } catch (e: Exception) {
        Log.e(TAG, "Error updating profile", e)
        Result.failure(e)
    }
}

suspend fun SupabaseClient.updateUserLastTaskDate(userID: String, lastTaskDate: String, authToken: String): Result<Response> {
    return try {
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

        val response = withContext(Dispatchers.IO) { client.newCall(request).execute() }

        if (response.isSuccessful) {
            Result.success(response)
        } else {
            Result.failure(Exception("Failed to update last task date: ${response.message}"))
        }
    } catch (e: Exception) {
        Log.e(TAG, "Error with updating last task date", e)
        Result.failure(e)
    }
}

suspend fun SupabaseClient.updateUserStreak(userId: String, streak: Int, authToken: String): Result<Response> {
    return try {
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

        val response = withContext(Dispatchers.IO) { client.newCall(request).execute() }

        if (response.isSuccessful) {
            Result.success(response)
        } else {
            Result.failure(Exception("Failed to update user streak: ${response.message}"))
        }
    } catch (e: Exception) {
        Log.e(TAG, "Error updating user streak", e)
        Result.failure(e)
    }
}

fun SupabaseClient.updateUserFriendMask(userId: String, friendMask: String, authToken: String): Result<Response> {
    val json = """{"friend_mask": "$friendMask"}"""
    val requestBody = json.toRequestBody("application/json".toMediaType())
    Log.d("SupabaseUser", json)
    val request = Request.Builder()
        .url("$supabaseUrl/rest/v1/profile?id=eq.$userId")
        .patch(requestBody)
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
            Log.d("SupabaseUser", response.body?.string().toString())
            Result.failure(Exception("Failed to update user friend mask"))
        }
    } catch (e: Exception) {
        Log.e(TAG, "Error to update user friend mask", e)
        Result.failure(e)
    }
}