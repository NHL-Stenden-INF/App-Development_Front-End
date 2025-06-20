package com.nhlstenden.appdev.core.repositories

import com.nhlstenden.appdev.features.friends.models.Friend
import com.nhlstenden.appdev.features.friends.models.FriendDetails

interface FriendsRepository {
    suspend fun addFriend(friendId: String): Result<Unit>
    suspend fun getAllFriends(): Result<List<Friend>>
    suspend fun removeFriend(friendId: String): Result<Unit>
    suspend fun getCurrentUserQRCode(): Result<String>
    suspend fun getFriendDetails(friendId: String): Result<FriendDetails>
} 