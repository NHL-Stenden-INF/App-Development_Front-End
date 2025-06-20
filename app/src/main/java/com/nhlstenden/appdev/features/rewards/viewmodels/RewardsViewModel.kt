package com.nhlstenden.appdev.features.rewards.viewmodels

import android.content.Context
import android.content.SharedPreferences
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.nhlstenden.appdev.core.models.RewardType
import com.nhlstenden.appdev.core.repositories.AuthRepository
import com.nhlstenden.appdev.core.repositories.RewardsRepository
import com.nhlstenden.appdev.core.repositories.UserRepository
import com.nhlstenden.appdev.core.utils.ErrorHandler
import com.nhlstenden.appdev.features.rewards.handlers.PurchaseResult
import com.nhlstenden.appdev.features.rewards.dialogs.ThemeCustomizationDialog
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.time.LocalDate
import javax.inject.Inject

@HiltViewModel
class RewardsViewModel @Inject constructor(
    private val authRepository: AuthRepository,
    private val userRepository: UserRepository,
    private val rewardsRepository: RewardsRepository,
    @ApplicationContext private val context: Context
) : ViewModel() {

    private val TAG = "RewardsViewModel"

    private val _uiState = MutableStateFlow(RewardsUiState())
    val uiState: StateFlow<RewardsUiState> = _uiState.asStateFlow()

    private val sharedPreferences: SharedPreferences = context.getSharedPreferences("theme_prefs", Context.MODE_PRIVATE)
    private val THEME_COLOR_KEY = "custom_theme_color"

    init {
        loadUserData()
        loadUnlockedRewards()
    }

    private fun loadUserData() {
        viewModelScope.launch {
            try {
                val currentUser = authRepository.getCurrentUserSync()
                if (currentUser == null) {
                    updateErrorState("User not authenticated")
                    return@launch
                }

                updateLoadingState(true)

                val userAttributesResult = userRepository.getUserAttributes(currentUser.id)
                
                ErrorHandler.handleResult(
                    result = userAttributesResult,
                    tag = TAG,
                    operation = "loading user data",
                    onSuccess = { attributes ->
                        val points = attributes.optInt("points", 0)
                        val bellPeppers = attributes.optInt("bell_peppers", 0)
                        val openedDailyAt = attributes.optString("opened_daily_at", null)

                        _uiState.value = _uiState.value.copy(
                            isLoading = false,
                            points = points,
                            bellPeppers = bellPeppers,
                            openedDailyAt = if (openedDailyAt == "null") null else openedDailyAt,
                            error = null
                        )
                    },
                    onFailure = { errorMessage ->
                        updateErrorState(errorMessage)
                    }
                )
            } catch (e: Exception) {
                val errorMessage = ErrorHandler.createErrorMessage("loading user data", e)
                updateErrorState(errorMessage)
            }
        }
    }

    private fun loadUnlockedRewards() {
        viewModelScope.launch {
            try {
                val result = rewardsRepository.getUserUnlockedRewards()
                if (result.isSuccess) {
                    val unlockedRewards = result.getOrThrow()
                    val unlockedRewardIds = mutableSetOf<Int>()
                    
                    for (i in 0 until unlockedRewards.length()) {
                        val rewardObj = unlockedRewards.getJSONObject(i)
                        unlockedRewardIds.add(rewardObj.optInt("reward_id"))
                    }
                    
                    _uiState.value = _uiState.value.copy(
                        unlockedRewardIds = unlockedRewardIds
                    )
                } else {
                    Log.e(TAG, "Failed to load unlocked rewards: ${result.exceptionOrNull()?.message}")
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error loading unlocked rewards", e)
            }
        }
    }

    fun collectDailyReward() {
        viewModelScope.launch {
            try {
                val currentUser = authRepository.getCurrentUserSync() ?: return@launch
                
                _uiState.value = _uiState.value.copy(isCollectingReward = true)
                
                val todayDate = LocalDate.now().toString()
                val updateDailyResult = userRepository.updateUserOpenedDaily(currentUser.id, todayDate)
                
                if (updateDailyResult.isSuccess) {
                    val rewardPoints = (1..100).random()
                    val newPoints = _uiState.value.points + rewardPoints
                    
                    val updatePointsResult = userRepository.updateUserPoints(currentUser.id, newPoints)
                    
                    if (updatePointsResult.isSuccess) {
                        _uiState.value = _uiState.value.copy(
                            isCollectingReward = false,
                            points = newPoints,
                            openedDailyAt = todayDate,
                            lastRewardAmount = rewardPoints,
                            error = null
                        )
                    } else {
                        _uiState.value = _uiState.value.copy(
                            isCollectingReward = false,
                            error = "Failed to update points"
                        )
                    }
                } else {
                    _uiState.value = _uiState.value.copy(
                        isCollectingReward = false,
                        error = "Failed to collect daily reward"
                    )
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error collecting daily reward", e)
                _uiState.value = _uiState.value.copy(
                    isCollectingReward = false,
                    error = "Error collecting reward: ${e.message}"
                )
            }
        }
    }

    fun purchaseBellPepper(cost: Int) {
        viewModelScope.launch {
            try {
                val currentUser = authRepository.getCurrentUserSync() ?: return@launch
                
                if (_uiState.value.bellPeppers >= 3) {
                    _uiState.value = _uiState.value.copy(
                        error = "You already have the maximum number of bell peppers!"
                    )
                    return@launch
                }
                
                if (_uiState.value.points < cost) {
                    _uiState.value = _uiState.value.copy(
                        error = "Not enough points!"
                    )
                    return@launch
                }
                
                val newPoints = _uiState.value.points - cost
                val updatePointsResult = userRepository.updateUserPoints(currentUser.id, newPoints)
                
                if (updatePointsResult.isSuccess) {
                    val newBellPeppers = _uiState.value.bellPeppers + 1
                    val updateBellPeppersResult = userRepository.updateUserBellPeppers(currentUser.id, newBellPeppers)
                    
                    if (updateBellPeppersResult.isSuccess) {
                        _uiState.value = _uiState.value.copy(
                            points = newPoints,
                            bellPeppers = newBellPeppers,
                            error = null
                        )
                    } else {
                        userRepository.updateUserPoints(currentUser.id, _uiState.value.points)
                        _uiState.value = _uiState.value.copy(
                            error = "Failed to update bell peppers"
                        )
                    }
                } else {
                    _uiState.value = _uiState.value.copy(
                        error = "Failed to update points"
                    )
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error purchasing bell pepper", e)
                _uiState.value = _uiState.value.copy(
                    error = "Error purchasing bell pepper: ${e.message}"
                )
            }
        }
    }

    fun purchaseReward(rewardId: Int, cost: Int) {
        viewModelScope.launch {
            try {
                val result = rewardsRepository.purchaseReward(rewardId, cost)
                
                if (result.isSuccess) {
                    val newPoints = _uiState.value.points - cost
                    val newUnlockedRewards = _uiState.value.unlockedRewardIds.toMutableSet()
                    newUnlockedRewards.add(rewardId)
                    
                    _uiState.value = _uiState.value.copy(
                        points = newPoints,
                        unlockedRewardIds = newUnlockedRewards,
                        error = null
                    )
                } else {
                    _uiState.value = _uiState.value.copy(
                        error = result.exceptionOrNull()?.message ?: "Failed to purchase reward"
                    )
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error purchasing reward", e)
                _uiState.value = _uiState.value.copy(
                    error = "Error purchasing reward: ${e.message}"
                )
            }
        }
    }

    fun clearError() {
        _uiState.value = _uiState.value.copy(error = null)
    }

    fun refreshData() {
        loadUserData()
        loadUnlockedRewards()
    }
    
    fun purchaseRewardByType(rewardType: RewardType, rewardId: Int, cost: Int) {
        viewModelScope.launch {
            when (rewardType) {
                is RewardType.BellPepper -> purchaseBellPepper(cost)
                is RewardType.ThemeCustomization -> purchaseThemeCustomization(rewardId, cost)
                is RewardType.StandardUnlock -> purchaseReward(rewardId, cost)
                is RewardType.Special -> purchaseReward(rewardId, cost) // Handle special rewards through standard flow for now
            }
        }
    }
    
    private fun updateLoadingState(isLoading: Boolean) {
        _uiState.value = _uiState.value.copy(isLoading = isLoading)
    }
    
    private fun updateErrorState(errorMessage: String) {
        _uiState.value = _uiState.value.copy(
            isLoading = false,
            error = errorMessage
        )
    }

    private fun purchaseThemeCustomization(rewardId: Int, cost: Int) {
        viewModelScope.launch {
            try {
                val currentUser = authRepository.getCurrentUserSync() ?: return@launch
                
                if (_uiState.value.points < cost) {
                    _uiState.value = _uiState.value.copy(
                        error = "Not enough points!"
                    )
                    return@launch
                }
                
                val newPoints = _uiState.value.points - cost
                val updatePointsResult = userRepository.updateUserPoints(currentUser.id, newPoints)
                
                if (updatePointsResult.isSuccess) {
                    val unlockResult = rewardsRepository.unlockReward(rewardId)
                    
                    if (unlockResult.isSuccess) {
                        _uiState.value = _uiState.value.copy(
                            points = newPoints,
                            error = null
                        )
                        
                        // Show theme customization dialog
                        showThemeCustomizationDialog()
                    } else {
                        // Rollback points if unlocking failed
                        userRepository.updateUserPoints(currentUser.id, _uiState.value.points)
                        _uiState.value = _uiState.value.copy(
                            error = "Failed to unlock theme customization"
                        )
                    }
                } else {
                    _uiState.value = _uiState.value.copy(
                        error = "Failed to update points"
                    )
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error purchasing theme customization", e)
                _uiState.value = _uiState.value.copy(
                    error = "Error purchasing theme customization: ${e.message}"
                )
            }
        }
    }

    private fun showThemeCustomizationDialog() {
        // Show the theme customization dialog
        val dialog = ThemeCustomizationDialog.newInstance()
        dialog.setOnThemeAppliedListener { colorValue ->
            // Handle the applied theme color
            applyCustomTheme(colorValue)
        }
        
        // Show the dialog handled by the fragment observing the state
        _uiState.value = _uiState.value.copy(
            showThemeDialog = true
        )
    }

    fun applyCustomTheme(colorValue: String) {
        // Save the custom color to SharedPreferences
        sharedPreferences.edit().putString(THEME_COLOR_KEY, colorValue).apply()
        
        _uiState.value = _uiState.value.copy(
            error = "Theme color saved: $colorValue. Restart app to see changes."
        )
        
        Log.d(TAG, "Custom theme color saved: $colorValue")
    }

    fun getCustomThemeColor(): String? {
        return sharedPreferences.getString(THEME_COLOR_KEY, null)
    }

    fun clearCustomTheme() {
        sharedPreferences.edit().remove(THEME_COLOR_KEY).apply()
        _uiState.value = _uiState.value.copy(
            error = "Theme reset to default"
        )
    }

    fun clearThemeDialog() {
        _uiState.value = _uiState.value.copy(
            showThemeDialog = false
        )
    }
}

data class RewardsUiState(
    val isLoading: Boolean = false,
    val isCollectingReward: Boolean = false,
    val points: Int = 0,
    val bellPeppers: Int = 0,
    val openedDailyAt: String? = null,
    val lastRewardAmount: Int = 0,
    val unlockedRewardIds: Set<Int> = emptySet(),
    val error: String? = null,
    val showThemeDialog: Boolean = false
) {
    val canCollectDailyReward: Boolean
        get() = openedDailyAt != LocalDate.now().toString()
} 