package com.nhlstenden.appdev.features.friends.viewmodels

import android.graphics.Bitmap
import com.google.zxing.BarcodeFormat
import com.google.zxing.MultiFormatWriter
import com.google.zxing.common.BitMatrix
import com.journeyapps.barcodescanner.BarcodeEncoder
import com.nhlstenden.appdev.features.friends.models.Friend
import com.nhlstenden.appdev.shared.ui.base.BaseViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext
import java.util.UUID
import com.nhlstenden.appdev.core.repositories.FriendsRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import android.util.Log
import android.app.Application

@HiltViewModel
class FriendsViewModel @Inject constructor(
    application: Application,
    private val friendsRepository: FriendsRepository
) : BaseViewModel(application) {
    private val _friends = MutableStateFlow<List<Friend>>(emptyList())
    val friends: StateFlow<List<Friend>> = _friends.asStateFlow()
    
    private val _qrCode = MutableStateFlow<Bitmap?>(null)
    val qrCode: StateFlow<Bitmap?> = _qrCode.asStateFlow()
    
    fun loadFriends() {
        launchWithLoading {
            friendsRepository.getAllFriends()
                .onSuccess { friends ->
                    _friends.value = friends
                    Log.d("FriendsViewModel", "Loaded ${friends.size} friends successfully")
                }
                .onFailure { error ->
                    setError(error.message ?: "Failed to load friends")
                    Log.e("FriendsViewModel", "Failed to load friends", error)
                }
        }
    }
    
    fun generateQRCode() {
        launchWithLoading {
            friendsRepository.getCurrentUserQRCode()
                .onSuccess { userId ->
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
                            Log.e("FriendsViewModel", "Error generating QR code", e)
                            null
                        }
                    }
                }
                .onFailure { error ->
                    Log.e("FriendsViewModel", "Error getting user ID for QR code", error)
                    setError("Failed to generate QR code")
                }
        }
    }
    
    fun addFriend(friendId: String) {
        launchWithLoading {
            friendsRepository.addFriend(friendId)
                .onSuccess {
                    setSuccess("Friend added successfully!")
                    loadFriends() // Reload friends after adding
                    Log.d("FriendsViewModel", "Friend added successfully: $friendId")
                }
                .onFailure { error ->
                    setError(error.message ?: "Failed to add friend")
                    Log.e("FriendsViewModel", "Failed to add friend: $friendId", error)
                }
        }
    }
    
    fun removeFriend(friendId: String) {
        launchWithLoading {
            friendsRepository.removeFriend(friendId)
                .onSuccess {
                    setSuccess("Friend removed successfully!")
                    loadFriends() // Reload friends after removing
                    Log.d("FriendsViewModel", "Friend removed successfully: $friendId")
                }
                .onFailure { error ->
                    setError(error.message ?: "Failed to remove friend")
                    Log.e("FriendsViewModel", "Failed to remove friend: $friendId", error)
                }
        }
    }

    fun acceptFriendRequest(_friendId: String) {
        // Implementation
    }
} 