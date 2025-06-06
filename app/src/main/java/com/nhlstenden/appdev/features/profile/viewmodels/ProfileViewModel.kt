package com.nhlstenden.appdev.features.profile.viewmodels

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.nhlstenden.appdev.core.models.Profile
import com.nhlstenden.appdev.core.models.Achievement
import com.nhlstenden.appdev.core.models.UserProfile
import com.nhlstenden.appdev.core.repositories.ProfileRepository
import com.nhlstenden.appdev.core.utils.UserManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class ProfileViewModel @Inject constructor(
    private val profileRepository: ProfileRepository
) : ViewModel() {
    private val _userProfile = MutableLiveData<UserProfile>()
    val userProfile: LiveData<UserProfile> = _userProfile

    private val _profileState = MutableStateFlow<ProfileState>(ProfileState.Loading)
    val profileState: StateFlow<ProfileState> = _profileState.asStateFlow()

    private val _achievements = MutableStateFlow<List<Achievement>>(emptyList())
    val achievements: StateFlow<List<Achievement>> = _achievements.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _error = MutableLiveData<String>()
    val error: LiveData<String> = _error

    fun loadProfile() {
        val user = UserManager.getCurrentUser()
        if (user == null || user.authToken.isEmpty()) {
            _profileState.value = ProfileState.Error("No valid user data available")
            return
        }

        viewModelScope.launch {
            _profileState.value = ProfileState.Loading
            try {
                val profile = profileRepository.getProfile()
                _profileState.value = ProfileState.Success(profile)
            } catch (e: Exception) {
                android.util.Log.e("ProfileViewModel", "Profile load failed", e)
                _profileState.value = ProfileState.Error(e.message ?: "Failed to load profile")
            }
        }
    }

    fun onChangePhotoClicked() {
        // Handle photo change
    }

    fun onEditProfileClicked() {
        // Handle edit profile
    }

    fun logout() {
        viewModelScope.launch {
            try {
                profileRepository.logout()
                _profileState.value = ProfileState.LoggedOut
            } catch (e: Exception) {
                _profileState.value = ProfileState.Error(e.message ?: "Failed to logout")
            }
        }
    }

    fun updateProfile(displayName: String, bio: String?, profilePicture: String?) {
        viewModelScope.launch {
            _profileState.value = ProfileState.Loading
            try {
                val profile = profileRepository.updateProfile(displayName, bio, profilePicture)
                _profileState.value = ProfileState.Success(profile)
            } catch (e: Exception) {
                _profileState.value = ProfileState.Error(e.message ?: "Failed to update profile")
            }
        }
    }

    fun updateProfilePicture(imagePath: String) {
        viewModelScope.launch {
            _profileState.value = ProfileState.Loading
            try {
                val profile = profileRepository.updateProfilePicture(imagePath)
                _profileState.value = ProfileState.Success(profile)
            } catch (e: Exception) {
                _profileState.value = ProfileState.Error(e.message ?: "Failed to update profile picture")
            }
        }
    }

    fun setUserData(user: com.nhlstenden.appdev.core.models.User) {
        (profileRepository as? com.nhlstenden.appdev.features.profile.repositories.ProfileRepositoryImpl)?.setUserData(user)
    }

    sealed class ProfileState {
        object Loading : ProfileState()
        data class Success(val profile: Profile) : ProfileState()
        data class Error(val message: String) : ProfileState()
        object LoggedOut : ProfileState()
    }
} 