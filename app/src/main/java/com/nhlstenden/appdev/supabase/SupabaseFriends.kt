package com.nhlstenden.appdev.supabase

import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.Response
import com.nhlstenden.appdev.core.utils.UserManager
import kotlin.math.E

private val TAG = "SupabaseFriends"

suspend fun SupabaseClient.addFriend(friendId: String, authToken: String): Result<Response> {
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

    return try {
        val response = withContext(Dispatchers.IO) { client.newCall(request).execute() }

        if (response.isSuccessful) {
            Result.success(response)
        } else {
            Result.failure(Exception("Failed to add friend: ${response.message}"))
        }
    } catch (e: Exception) {
        Log.e(TAG, "Error with adding friend", e)
        Result.failure(e)
    }
}

fun SupabaseClient.getOrCreateFriendAttributes(friendId: String, authToken: String): Result<Response> {
    Log.d("SupabaseClient", "Getting attributes for friend using SQL function: $friendId")
    val rpcRequest = Request.Builder()
        .url("$supabaseUrl/rest/v1/rpc/get_friend_details")
        .post("""{"friend_id": "$friendId"}""".toRequestBody("application/json".toMediaType()))
        .addHeader("apikey", supabaseKey)
        .addHeader("Authorization", "Bearer $authToken")
        .addHeader("Content-Type", "application/json")
        .build()

    return try {
        val response = client.newCall(rpcRequest).execute()
        Log.d("SupabaseClient", "Get friend details SQL function response code: ${response.code}")

        if (response.isSuccessful) {
            Result.success(response)
        } else {
            Result.failure(Exception("Failed to get or create friend attributes: ${response.message}"))
        }
    } catch (e: Exception) {
        Log.e(TAG, "Error with getting or creating friend attributes", e)
        Result.failure(e)
    }
}

fun SupabaseClient.getFriendDetails(friendId: String, authToken: String): Result<Response> {
    val request = Request.Builder()
        .url("$supabaseUrl/rest/v1/user_attributes?select=id,points,profile_picture&id=eq.$friendId")
        .get()
        .addHeader("apikey", supabaseKey)
        .addHeader("Authorization", "Bearer $authToken")
        .build()

    return try {
        val response = client.newCall(request).execute()

        if (response.isSuccessful) {
            Result.success(response)
        } else {
            Result.failure(Exception("Failed to get friend details: ${response.message}"))
        }
    } catch (e: Exception) {
        Log.e(TAG, "Error with getting friend details", e)
        Result.failure(e)
    }
}

fun SupabaseClient.getFriendUsername(friendId: String, authToken: String): Result<Response> {
    val request = Request.Builder()
        .url("$supabaseUrl/auth/v1/admin/users/$friendId")
        .get()
        .addHeader("apikey", supabaseKey)
        .addHeader("Authorization", "Bearer $authToken")
        .build()

    return try {
        val response = client.newCall(request).execute()

        if (response.isSuccessful) {
            Result.success(response)
        } else {
            Result.failure(Exception("Failed to get username from friend: ${response.message}"))
        }
    } catch (e: Exception) {
        Log.e(TAG, "Error with getting username from friend", e)
        Result.failure(e)
    }
}

fun SupabaseClient.getFriendDisplayName(friendId: String, authToken: String): Result<Response> {
    Log.d("SupabaseClient", "Getting display name for friend: $friendId")
    val request = Request.Builder()
        .url("$supabaseUrl/rest/v1/rpc/get_user_display_name")
        .post("""{"user_id": "$friendId"}""".toRequestBody("application/json".toMediaType()))
        .addHeader("apikey", supabaseKey)
        .addHeader("Authorization", "Bearer $authToken")
        .addHeader("Content-Type", "application/json")
        .build()

    return try {
    val response = client.newCall(request).execute()
    Log.d("SupabaseClient", "Get display name response code: ${response.code}")

        if (response.isSuccessful) {
            Result.success(response)
        } else {
            Result.failure(Exception("Failed to get display name: ${response.message}"))
        }
//
    } catch (e: Exception) {
        Log.e(TAG, "Error with getting display name from friend", e)
        Result.failure(e)
    }
}

fun SupabaseClient.getFriendsDetails(friendIds: List<String>, authToken: String): Result<Response> {
    val friendIdsFormatted = friendIds.joinToString(",") { "\"$it\"" }
    val request = Request.Builder()
        .url("$supabaseUrl/rest/v1/rpc/get_friends_details")
        .post("""{"friend_ids": [$friendIdsFormatted]}""".toRequestBody("application/json".toMediaType()))
        .addHeader("apikey", supabaseKey)
        .addHeader("Authorization", "Bearer $authToken")
        .addHeader("Content-Type", "application/json")
        .build()

    return try {
        val response = client.newCall(request).execute()

        if (response.isSuccessful){
            Result.success(response)
        } else {
            Result.failure(Exception("Failed to get the details of friend: ${response.message}"))
        }
    } catch (e: Exception) {
        Log.e(TAG, "Error with getting the details of friend", e)
        Result.failure(e)
    }
}

suspend fun SupabaseClient.createMutualFriendship(targetFriendId: String, authToken: String): Result<Response> {
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

    return try {
        val response = withContext(Dispatchers.IO) { client.newCall(request).execute() }

        if (response.isSuccessful) {
            Result.success(response)
        } else {
            Result.failure(Exception("Failed to create friendship: ${response.message}"))
        }
    } catch (e: Exception) {
        Log.e(TAG, "Error with creating friendship", e)
        Result.failure(e)
    }
}

suspend fun SupabaseClient.getAllFriends(authToken: String): Result<Response> {
    val request = Request.Builder()
        .url("$supabaseUrl/rest/v1/rpc/get_all_friends")
        .post("{}".toRequestBody("application/json".toMediaType()))
        .addHeader("apikey", supabaseKey)
        .addHeader("Authorization", "Bearer $authToken")
        .addHeader("Content-Type", "application/json")
        .addHeader("Cache-Control", "no-cache, no-store")
        .addHeader("Pragma", "no-cache")
        .build()

    return try {
        val freshClient = OkHttpClient.Builder().cache(null).build()
        val response = withContext(Dispatchers.IO) {freshClient.newCall(request).execute()}

        if (response.isSuccessful){
            Result.success(response)
        } else {
            Result.failure(Exception("Failed to get all friends: ${response.message}"))
        }
    } catch (e: Exception) {
        Log.e(TAG, "Error with attempt to get all friends", e)
        Result.failure(e)
    }
}

suspend fun SupabaseClient.queryFriendships(authToken: String): Result<Response> {
    val userIdFromToken = getUserIdFromToken(authToken).getOrThrow()
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

    return try{
        val freshClient = OkHttpClient.Builder().cache(null).build()
        val response = withContext(Dispatchers.IO) { freshClient.newCall(request).execute() }

        if (response.isSuccessful) {
            Result.success(response)
        } else {
            Result.failure(Exception("Failed to query friendships: ${response.message}"))
        }
    } catch (e: Exception) {
        Log.e(TAG, "Error with querieng al friendships", e)
        Result.failure(e)
    }
}

suspend fun SupabaseClient.getUserFriendIds(authToken: String): Result<Response> {
    val request = Request.Builder()
        .url("$supabaseUrl/rest/v1/rpc/get_user_friends")
        .post("{}".toRequestBody("application/json".toMediaType()))
        .addHeader("apikey", supabaseKey)
        .addHeader("Authorization", "Bearer $authToken")
        .addHeader("Content-Type", "application/json")
        .addHeader("Cache-Control", "no-cache, no-store")
        .addHeader("Pragma", "no-cache")
        .build()

    return try {
        val freshClient = OkHttpClient.Builder().cache(null).build()
        val response = withContext(Dispatchers.IO) { freshClient.newCall(request).execute() }

        if (response.isSuccessful) {
            Result.success(response)
        } else {
            Result.failure(Exception("Failed to get friend ids: ${response.message}"))
        }
    } catch (e: Exception) {
        Log.e(TAG, "Error getting friend ids", e)
        Result.failure(e)
    }
}

suspend fun SupabaseClient.addFriendWithCurrentUser(friendId: String): Result<Response> {
    val user = UserManager.getCurrentUser() ?: return Result.failure(IllegalStateException("No user logged in"))

    return try {
        val result = createMutualFriendship(friendId, user.authToken)

        result
    } catch (e: Exception) {
        Log.e(TAG, "Error with getting current user", e)
        Result.failure(e)
    }
}

suspend fun SupabaseClient.getAllFriendsForCurrentUser(): Result<Response> {
    val user = UserManager.getCurrentUser() ?: return Result.failure(IllegalStateException("No user logged in"))

    return try {
        val result = getAllFriends(user.authToken)

        result
    } catch (e: Exception) {
        Log.e(TAG, "Error with getting all friends", e)
        Result.failure(e)
    }
}

fun SupabaseClient.getCurrentUserId(): Result<String?> {
    return try {
        val userId = UserManager.getCurrentUser()?.id?.toString()

        if (userId != null) {
            Result.success(userId)
        } else {
            Result.failure(IllegalStateException("No user is logged in"))
        }
    } catch (e: Exception) {
        Log.e(TAG, "Error getting user ID")
        Result.failure(e)
    }
}

suspend fun SupabaseClient.removeFriend(friendId: String, authToken: String): Result<Response> {
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

    return try {
        val response = withContext(Dispatchers.IO) { client.newCall(request).execute() }

        if (response.isSuccessful) {
            Result.success(response)
        } else {
            Result.failure(Exception("Failed to remove friend: ${response.message}"))
        }
    } catch (e: Exception) {
        Log.e(TAG, "Error with removing friend", e)
        Result.failure(e)
    }
}

suspend fun SupabaseClient.removeFriendWithCurrentUser(friendId: String): Result<Response> {
    val user = UserManager.getCurrentUser() ?: return Result.failure(IllegalStateException("No user logged in"))

    return try {
        val response = removeFriend(friendId, user.authToken).getOrThrow()

        if (response.isSuccessful) {
            Result.success(response)
        } else {
            Result.failure(Exception("Failed to remove friend"))
        }
    } catch (e: Exception) {
        Log.e(TAG, "Error with removing friend", e)
        Result.failure(e)
    }
}