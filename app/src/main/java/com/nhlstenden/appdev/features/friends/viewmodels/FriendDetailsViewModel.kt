package com.nhlstenden.appdev.features.friends.viewmodels

import android.app.Application
import android.util.Log
import com.nhlstenden.appdev.core.repositories.FriendsRepository
import com.nhlstenden.appdev.features.friends.models.CourseProgress
import com.nhlstenden.appdev.features.friends.models.FriendDetails
import com.nhlstenden.appdev.shared.ui.base.BaseViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject

@HiltViewModel
class FriendDetailsViewModel @Inject constructor(
    application: Application,
    private val friendsRepository: FriendsRepository
) : BaseViewModel(application) {

    private val _friendDetails = MutableStateFlow<FriendDetails?>(null)
    val friendDetails: StateFlow<FriendDetails?> = _friendDetails.asStateFlow()

    fun loadFriendDetails(friendId: String) {
        launchWithLoading {
            friendsRepository.getFriendDetails(friendId)
                .onSuccess { details ->
                    _friendDetails.value = details
                    Log.d("FriendDetailsViewModel", "Friend details loaded successfully for ${details.username}")
                }
                .onFailure { error ->
                    setError(error.message ?: "Failed to load friend details")
                    Log.e("FriendDetailsViewModel", "Failed to load friend details", error)
                }
        }
    }


} 