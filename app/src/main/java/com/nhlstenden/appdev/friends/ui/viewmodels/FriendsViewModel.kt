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

class FriendsViewModel : BaseViewModel() {
    private val _friends = MutableStateFlow<List<Friend>>(emptyList())
    val friends: StateFlow<List<Friend>> = _friends.asStateFlow()
    
    private val _qrCode = MutableStateFlow<Bitmap?>(null)
    val qrCode: StateFlow<Bitmap?> = _qrCode.asStateFlow()
    
    fun loadFriends() {
        launchWithLoading {
            // TODO: Implement friend loading from repository
            // For now, using dummy data
            _friends.value = listOf(
                Friend(
                    id = "1",
                    username = "John Doe",
                    profilePicture = null,
                    progress = 75
                ),
                Friend(
                    id = "2",
                    username = "Jane Smith",
                    profilePicture = null,
                    progress = 90
                )
            )
        }
    }
    
    fun generateQRCode() {
        launchWithLoading {
            val userId = UUID.randomUUID().toString()
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
            // TODO: Implement friend adding
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