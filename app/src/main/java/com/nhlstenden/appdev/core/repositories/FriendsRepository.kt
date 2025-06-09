package com.nhlstenden.appdev.core.repositories

import com.nhlstenden.appdev.friends.domain.models.Friend

interface FriendsRepository {
    suspend fun addFriend(friendId: String): Result<Unit>
    suspend fun getAllFriends(): Result<List<Friend>>
    suspend fun removeFriend(friendId: String): Result<Unit>
    suspend fun getCurrentUserQRCode(): Result<String>
} 