package com.nhlstenden.appdev.features.profile.viewmodels

import android.app.Application
import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.nhlstenden.appdev.core.models.Profile
import com.nhlstenden.appdev.core.models.Achievement
import com.nhlstenden.appdev.core.models.UserProfile
import com.nhlstenden.appdev.core.repositories.ProfileRepository
import com.nhlstenden.appdev.shared.ui.base.BaseViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject

@HiltViewModel
class ProfileViewModel @Inject constructor(
    application: Application,
    private val profileRepository: ProfileRepository
) : BaseViewModel(application) {
    private val _userProfile = MutableLiveData<UserProfile>()
    val userProfile: LiveData<UserProfile> = _userProfile

    private val _profileState = MutableStateFlow<ProfileState>(ProfileState.Loading)
    val profileState: StateFlow<ProfileState> = _profileState.asStateFlow()

    private val _achievements = MutableStateFlow<List<Achievement>>(emptyList())
    val achievements: StateFlow<List<Achievement>> = _achievements.asStateFlow()

    fun loadProfile() {
        launchWithLoading {
            profileRepository.getProfile()
                .onSuccess { profile ->
                    _profileState.value = ProfileState.Success(profile)
                    Log.d("ProfileViewModel", "Profile loaded successfully for ${profile.displayName}")
                }
                .onFailure { error ->
                    val errorMessage = error.message ?: "Failed to load profile"
                    _profileState.value = ProfileState.Error(errorMessage)
                    setError(errorMessage)
                    Log.e("ProfileViewModel", "Failed to load profile", error)
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
        launchWithLoading {
            profileRepository.logout()
                .onSuccess {
                    _profileState.value = ProfileState.LoggedOut
                    setSuccess("Logged out successfully")
                    Log.d("ProfileViewModel", "User logged out successfully")
                }
                .onFailure { error ->
                    val errorMessage = error.message ?: "Failed to logout"
                    _profileState.value = ProfileState.Error(errorMessage)
                    setError(errorMessage)
                    Log.e("ProfileViewModel", "Failed to logout", error)
                }
        }
    }

    fun updateProfile(displayName: String, bio: String?, profilePicture: String?) {
        launchWithLoading {
            profileRepository.updateProfile(displayName, bio, profilePicture)
                .onSuccess { profile ->
                    _profileState.value = ProfileState.Success(profile)
                    setSuccess("Profile updated successfully")
                    Log.d("ProfileViewModel", "Profile updated successfully")
                }
                .onFailure { error ->
                    val errorMessage = error.message ?: "Failed to update profile"
                    _profileState.value = ProfileState.Error(errorMessage)
                    setError(errorMessage)
                    Log.e("ProfileViewModel", "Failed to update profile", error)
                }
        }
    }

    fun updateProfilePicture(imagePath: String) {
        launchWithLoading {
            profileRepository.updateProfilePicture(imagePath)
                .onSuccess { profile ->
                    _profileState.value = ProfileState.Success(profile)
                    setSuccess("Profile picture updated successfully")
                    Log.d("ProfileViewModel", "Profile picture updated successfully")
                }
                .onFailure { error ->
                    val errorMessage = error.message ?: "Failed to update profile picture"
                    _profileState.value = ProfileState.Error(errorMessage)
                    setError(errorMessage)
                    Log.e("ProfileViewModel", "Failed to update profile picture", error)
                }
        }
    }

    fun updateBio(bio: String) {
        launchWithLoading {
            profileRepository.updateBio(bio)
                .onSuccess { profile ->
                    _profileState.value = ProfileState.Success(profile)
                    setSuccess("Bio updated successfully")
                    Log.d("ProfileViewModel", "Bio updated successfully")
                }
                .onFailure { error ->
                    val errorMessage = error.message ?: "Failed to update bio"
                    _profileState.value = ProfileState.Error(errorMessage)
                    setError(errorMessage)
                    Log.e("ProfileViewModel", "Failed to update bio", error)
                }
        }
    }

    fun updateDisplayName(displayName: String) {
        launchWithLoading {
            profileRepository.updateDisplayName(displayName)
                .onSuccess { profile ->
                    _profileState.value = ProfileState.Success(profile)
                    setSuccess("Display name updated successfully")
                    Log.d("ProfileViewModel", "Display name updated successfully")
                }
                .onFailure { error ->
                    val errorMessage = error.message ?: "Failed to update display name"
                    _profileState.value = ProfileState.Error(errorMessage)
                    setError(errorMessage)
                    Log.e("ProfileViewModel", "Failed to update display name", error)
                }
        }
    }

    sealed class ProfileState {
        object Loading : ProfileState()
        data class Success(val profile: Profile) : ProfileState()
        data class Error(val message: String) : ProfileState()
        object LoggedOut : ProfileState()
    }
} 