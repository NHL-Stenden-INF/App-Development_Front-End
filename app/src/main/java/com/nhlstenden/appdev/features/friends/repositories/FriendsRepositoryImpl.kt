package com.nhlstenden.appdev.features.friends.repositories

import android.util.Log
import com.nhlstenden.appdev.core.repositories.FriendsRepository
import com.nhlstenden.appdev.friends.domain.models.Friend
import com.nhlstenden.appdev.supabase.SupabaseClient
import org.json.JSONArray
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class FriendsRepositoryImpl @Inject constructor(
    private val supabaseClient: SupabaseClient
) : FriendsRepository {

    private val TAG = "FriendsRepositoryImpl"

    override suspend fun addFriend(friendId: String): Result<Unit> {
        return try {
            val response = supabaseClient.addFriendWithCurrentUser(friendId)
            if (response.isSuccessful) {
                Log.d(TAG, "Friend added successfully: $friendId")
                Result.success(Unit)
            } else {
                val error = "Failed to add friend: ${response.code}"
                Log.e(TAG, error)
                Result.failure(Exception(error))
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error adding friend", e)
            Result.failure(e)
        }
    }

    override suspend fun getAllFriends(): Result<List<Friend>> {
        return try {
            val response = supabaseClient.getAllFriendsForCurrentUser()
            if (!response.isSuccessful) {
                val error = "Failed to load friends: ${response.code}"
                Log.e(TAG, error)
                return Result.failure(Exception(error))
            }

            val body = response.body?.string() ?: "[]"
            Log.d(TAG, "Get all friends response: $body")
            
            val jsonArray = JSONArray(body)
            val friendsList = mutableListOf<Friend>()
            
            for (i in 0 until jsonArray.length()) {
                val obj = jsonArray.getJSONObject(i)
                val points = obj.optInt("points", 0)
                val level = supabaseClient.calculateLevelFromXp(points.toLong())
                
                // Calculate XP progress for current level
                var requiredXp = 100.0
                var totalXp = 0.0
                for (j in 1 until level) {
                    totalXp += requiredXp
                    requiredXp *= 1.1
                }
                val xpForCurrentLevel = points - totalXp.toInt()
                val xpForNextLevel = requiredXp.toInt()
                
                val friend = Friend(
                    id = obj.optString("id"),
                    username = obj.optString("display_name"),
                    profilePicture = obj.optString("profile_picture", null),
                    bio = obj.optString("bio", null),
                    progress = points,
                    level = level,
                    currentLevelProgress = xpForCurrentLevel.coerceAtLeast(0),
                    currentLevelMax = xpForNextLevel
                )
                friendsList.add(friend)
                Log.d(TAG, "Parsed friend: ${friend.username} (ID: ${friend.id})")
            }
            
            Result.success(friendsList)
        } catch (e: Exception) {
            Log.e(TAG, "Error loading friends", e)
            Result.failure(e)
        }
    }

    override suspend fun removeFriend(friendId: String): Result<Unit> {
        return try {
            val response = supabaseClient.removeFriendWithCurrentUser(friendId)
            if (response.isSuccessful) {
                Log.d(TAG, "Friend removed successfully: $friendId")
                Result.success(Unit)
            } else {
                val error = "Failed to remove friend: ${response.code}"
                Log.e(TAG, error)
                Result.failure(Exception(error))
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error removing friend", e)
            Result.failure(e)
        }
    }

    override suspend fun getCurrentUserQRCode(): Result<String> {
        return try {
            val userId = supabaseClient.getCurrentUserId()
                ?: return Result.failure(Exception("User not logged in"))
            
            Result.success(userId)
        } catch (e: Exception) {
            Log.e(TAG, "Error getting current user QR code", e)
            Result.failure(e)
        }
    }
} 