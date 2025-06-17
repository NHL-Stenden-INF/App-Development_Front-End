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

suspend fun SupabaseClient.addFriend(friendId: String, authToken: String): Response {
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

    return withContext(Dispatchers.IO) { client.newCall(request).execute() }
}

fun SupabaseClient.getOrCreateFriendAttributes(friendId: String, authToken: String): Response {
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

fun SupabaseClient.getFriendDetails(friendId: String, authToken: String): Response {
    val request = Request.Builder()
        .url("$supabaseUrl/rest/v1/user_attributes?select=id,points,profile_picture&id=eq.$friendId")
        .get()
        .addHeader("apikey", supabaseKey)
        .addHeader("Authorization", "Bearer $authToken")
        .build()
    return client.newCall(request).execute()
}

fun SupabaseClient.getFriendUsername(friendId: String, authToken: String): Response {
    val request = Request.Builder()
        .url("$supabaseUrl/auth/v1/admin/users/$friendId")
        .get()
        .addHeader("apikey", supabaseKey)
        .addHeader("Authorization", "Bearer $authToken")
        .build()
    return client.newCall(request).execute()
}

fun SupabaseClient.getFriendDisplayName(friendId: String, authToken: String): Response {
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

fun SupabaseClient.getFriendsDetails(friendIds: List<String>, authToken: String): Response {
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

suspend fun SupabaseClient.createMutualFriendship(targetFriendId: String, authToken: String): Response {
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

    return withContext(Dispatchers.IO) { client.newCall(request).execute() }
}

suspend fun SupabaseClient.getAllFriends(authToken: String): Response {
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
        val freshClient = OkHttpClient.Builder().cache(null).build()
        freshClient.newCall(request).execute()
    }
}

suspend fun SupabaseClient.queryFriendships(authToken: String): Response {
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
        val freshClient = OkHttpClient.Builder().cache(null).build()
        freshClient.newCall(request).execute()
    }
}

suspend fun SupabaseClient.getUserFriendIds(authToken: String): Response {
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
        val freshClient = OkHttpClient.Builder().cache(null).build()
        freshClient.newCall(request).execute()
    }
}

suspend fun SupabaseClient.addFriendWithCurrentUser(friendId: String): Response {
    val user = UserManager.getCurrentUser() ?: throw IllegalStateException("No user logged in")
    return createMutualFriendship(friendId, user.authToken)
}

suspend fun SupabaseClient.getAllFriendsForCurrentUser(): Response {
    val user = UserManager.getCurrentUser() ?: throw IllegalStateException("No user logged in")
    return getAllFriends(user.authToken)
}

fun SupabaseClient.getCurrentUserId(): String? {
    return UserManager.getCurrentUser()?.id?.toString()
}

suspend fun SupabaseClient.removeFriend(friendId: String, authToken: String): Response {
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

    return withContext(Dispatchers.IO) { client.newCall(request).execute() }
}

suspend fun SupabaseClient.removeFriendWithCurrentUser(friendId: String): Response {
    val user = UserManager.getCurrentUser() ?: throw IllegalStateException("No user logged in")
    return removeFriend(friendId, user.authToken)
}