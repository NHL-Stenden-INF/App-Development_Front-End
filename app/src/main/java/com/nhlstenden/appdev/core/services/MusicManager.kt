package com.nhlstenden.appdev.core.services

import android.content.Context
import android.util.Log
import com.nhlstenden.appdev.core.repositories.SettingsRepository
import com.nhlstenden.appdev.features.profile.repositories.SettingsRepositoryImpl.SettingsConstants
import com.nhlstenden.appdev.utils.RewardChecker
import javax.inject.Inject
import javax.inject.Singleton

interface MusicManager {
    suspend fun startCourseMusic(courseId: String)
    fun stopMusic()
    fun pauseMusic()
    fun resumeMusic()
    suspend fun isMusicEnabled(): Boolean
}

@Singleton
class MusicManagerImpl @Inject constructor(
    private val context: Context,
    private val musicService: MusicService,
    private val settingsRepository: SettingsRepository,
    private val rewardChecker: RewardChecker
) : MusicManager {
    
    companion object {
        private const val TAG = "MusicManager"
    }
    
    override suspend fun startCourseMusic(courseId: String) {
        try {
            if (isMusicEnabled()) {
                musicService.startMusic(courseId)
                Log.d(TAG, "Started music for course: $courseId")
            } else {
                Log.d(TAG, "Music disabled - not starting for course: $courseId")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error starting course music", e)
        }
    }
    
    override fun stopMusic() {
        try {
            musicService.stopMusic()
            Log.d(TAG, "Music stopped")
        } catch (e: Exception) {
            Log.e(TAG, "Error stopping music", e)
        }
    }
    
    override fun pauseMusic() {
        try {
            musicService.pauseMusic()
            Log.d(TAG, "Music paused")
        } catch (e: Exception) {
            Log.e(TAG, "Error pausing music", e)
        }
    }
    
    override fun resumeMusic() {
        // Note: We can't easily check if music is enabled here since it's a suspend function
        // For now, just resume - the music service will handle the state
        try {
            musicService.resumeMusic()
            Log.d(TAG, "Music resumed")
        } catch (e: Exception) {
            Log.e(TAG, "Error resuming music", e)
        }
    }
    
    override suspend fun isMusicEnabled(): Boolean {
        return try {
            val isRewardUnlocked = rewardChecker.isRewardUnlocked(RewardChecker.MUSIC_LOBBY_REWARD_ID)
            val isSettingEnabled = settingsRepository.hasValue(SettingsConstants.COURSE_LOBBY_MUSIC)
            
            val enabled = isRewardUnlocked && isSettingEnabled
            Log.d(TAG, "Music enabled check - Reward unlocked: $isRewardUnlocked, Setting enabled: $isSettingEnabled, Final: $enabled")
            
            enabled
        } catch (e: Exception) {
            Log.e(TAG, "Error checking if music is enabled", e)
            false
        }
    }
} 