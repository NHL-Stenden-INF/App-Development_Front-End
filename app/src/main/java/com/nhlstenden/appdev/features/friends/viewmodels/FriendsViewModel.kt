package com.nhlstenden.appdev.friends.ui.viewmodels

import android.graphics.Bitmap
import com.google.zxing.BarcodeFormat
import com.google.zxing.MultiFormatWriter
import com.google.zxing.common.BitMatrix
import com.journeyapps.barcodescanner.BarcodeEncoder
import com.nhlstenden.appdev.friends.domain.models.Friend
import com.nhlstenden.appdev.shared.ui.base.BaseViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext
import java.util.UUID
import com.nhlstenden.appdev.supabase.SupabaseClient
import com.nhlstenden.appdev.core.utils.UserManager
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import android.util.Log
import android.app.Application

@HiltViewModel
class FriendsViewModel @Inject constructor(
    application: Application,
    private val supabaseClient: SupabaseClient
) : BaseViewModel(application) {
    private val _friends = MutableStateFlow<List<Friend>>(emptyList())
    val friends: StateFlow<List<Friend>> = _friends.asStateFlow()
    
    private val _qrCode = MutableStateFlow<Bitmap?>(null)
    val qrCode: StateFlow<Bitmap?> = _qrCode.asStateFlow()
    
    fun loadFriends() {
        launchWithLoading {
            val user = UserManager.getCurrentUser()
            if (user == null) {
                setError("User not logged in")
                return@launchWithLoading
            }
            val authToken = user.authToken
            val response = supabaseClient.getAllFriends(authToken)
            if (!response.isSuccessful) {
                setError("Failed to load friends: ${response.code}")
                Log.e("FriendsViewModel", "Failed to load friends: ${response.code}, Body: ${response.body?.string()}")
                return@launchWithLoading
            }
            val body = response.body?.string() ?: "[]"
            Log.d("FriendsViewModel", "Get all friends RPC response body: $body")
            val arr = org.json.JSONArray(body)
            val friendsList = mutableListOf<Friend>()
            for (i in 0 until arr.length()) {
                val obj = arr.getJSONObject(i)
                val friend = Friend(
                    id = obj.optString("id"),
                    username = obj.optString("display_name"),
                    profilePicture = obj.optString("profile_picture", null),
                    progress = obj.optInt("points", 0)
                )
                friendsList.add(friend)
                Log.d("FriendsViewModel", "Parsed friend: id=${friend.id}, username=${friend.username}, profilePicture=${friend.profilePicture}, points=${friend.progress}")
            }
            _friends.value = friendsList
        }
    }
    
    fun generateQRCode() {
        launchWithLoading {
            val userId = UserManager.getCurrentUser()?.id?.toString() ?: UUID.randomUUID().toString()
            _qrCode.value = withContext(Dispatchers.IO) {
                try {
                    val multiFormatWriter = MultiFormatWriter()
                    val bitMatrix: BitMatrix = multiFormatWriter.encode(
                        userId,
                        BarcodeFormat.QR_CODE,
                        500,
                        500
                    )
                    BarcodeEncoder().createBitmap(bitMatrix)
                } catch (e: Exception) {
                    null
                }
            }
        }
    }
    
    fun addFriend(friendId: String) {
        launchWithLoading {
            val user = UserManager.getCurrentUser()
            if (user == null) {
                setError("User not logged in")
                return@launchWithLoading
            }
            val authToken = user.authToken
            val response = supabaseClient.createMutualFriendship(friendId, authToken)
            if (!response.isSuccessful) {
                setError("Failed to add friend: ${response.code}")
                return@launchWithLoading
            }
            setSuccess("Friend added successfully!") // Optional success message
            loadFriends() // Reload friends after adding
        }
    }
    
    fun removeFriend(_friendId: String) {
        launchWithLoading {
            // TODO: Implement friend removal
            loadFriends() // Reload friends after removing
        }
    }

    fun acceptFriendRequest(_friendId: String) {
        // Implementation
    }
} 