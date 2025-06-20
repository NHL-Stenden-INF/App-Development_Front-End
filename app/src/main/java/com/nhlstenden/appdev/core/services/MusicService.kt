package com.nhlstenden.appdev.core.services

import android.content.Context
import android.media.AudioAttributes
import android.media.AudioFocusRequest
import android.media.AudioManager
import android.media.MediaPlayer
import android.os.Build
import android.util.Log
import com.nhlstenden.appdev.R
import javax.inject.Inject
import javax.inject.Singleton

interface MusicService {
    fun startMusic(courseId: String)
    fun stopMusic()
    fun pauseMusic()
    fun resumeMusic()
    fun isPlaying(): Boolean
}

@Singleton
class MusicServiceImpl @Inject constructor(
    private val context: Context
) : MusicService {
    
    private var mediaPlayer: MediaPlayer? = null
    private var audioManager: AudioManager? = null
    private var audioFocusRequest: AudioFocusRequest? = null
    private var currentCourseId: String? = null
    
    companion object {
        private const val TAG = "MusicService"
        private const val VOLUME_LEVEL = 0.3f
        
        private val COURSE_MUSIC_MAP = mapOf(
            "html" to R.raw.html_themesong,
            "css" to R.raw.css_themesong,
            "javascript" to R.raw.javascript_themesong,
            "python" to R.raw.python_themesong,
            "java" to R.raw.java_themesong,
            "sql" to R.raw.sql_themesong
        )
    }
    
    init {
        audioManager = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager
        setupAudioFocus()
    }
    
    private fun setupAudioFocus() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            audioFocusRequest = AudioFocusRequest.Builder(AudioManager.AUDIOFOCUS_GAIN)
                .setAudioAttributes(AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_GAME)
                    .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                    .build())
                .setOnAudioFocusChangeListener { focusChange ->
                    handleAudioFocusChange(focusChange)
                }
                .build()
        }
    }
    
    private fun handleAudioFocusChange(focusChange: Int) {
        when (focusChange) {
            AudioManager.AUDIOFOCUS_LOSS -> stopMusic()
            AudioManager.AUDIOFOCUS_LOSS_TRANSIENT -> pauseMusic()
            AudioManager.AUDIOFOCUS_GAIN -> resumeMusic()
        }
    }
    
    override fun startMusic(courseId: String) {
        if (currentCourseId == courseId && mediaPlayer?.isPlaying == true) {
            return // Already playing the correct music
        }
        
        stopMusic() // Stop any existing music
        currentCourseId = courseId
        
        val musicResourceId = COURSE_MUSIC_MAP[courseId] ?: R.raw.default_themesong
        
        try {
            requestAudioFocus { playMusic(musicResourceId) }
        } catch (e: Exception) {
            Log.e(TAG, "Error starting music for course $courseId", e)
        }
    }
    
    private fun requestAudioFocus(onSuccess: () -> Unit) {
        val result = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            audioFocusRequest?.let { audioManager?.requestAudioFocus(it) }
        } else {
            @Suppress("DEPRECATION")
            audioManager?.requestAudioFocus(
                null,
                AudioManager.STREAM_MUSIC,
                AudioManager.AUDIOFOCUS_GAIN
            )
        }
        
        if (result == AudioManager.AUDIOFOCUS_REQUEST_GRANTED) {
            onSuccess()
        }
    }
    
    private fun playMusic(resourceId: Int) {
        try {
            mediaPlayer = MediaPlayer.create(context, resourceId).apply {
                isLooping = true
                setVolume(VOLUME_LEVEL, VOLUME_LEVEL)
                start()
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error creating MediaPlayer", e)
        }
    }
    
    override fun stopMusic() {
        try {
            mediaPlayer?.stop()
            mediaPlayer?.release()
            mediaPlayer = null
            abandonAudioFocus()
            currentCourseId = null
        } catch (e: Exception) {
            Log.e(TAG, "Error stopping music", e)
        }
    }
    
    override fun pauseMusic() {
        try {
            mediaPlayer?.pause()
        } catch (e: Exception) {
            Log.e(TAG, "Error pausing music", e)
        }
    }
    
    override fun resumeMusic() {
        try {
            mediaPlayer?.start()
        } catch (e: Exception) {
            Log.e(TAG, "Error resuming music", e)
        }
    }
    
    override fun isPlaying(): Boolean {
        return mediaPlayer?.isPlaying ?: false
    }
    
    private fun abandonAudioFocus() {
        audioManager?.let { am ->
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                audioFocusRequest?.let { request ->
                    am.abandonAudioFocusRequest(request)
                }
            } else {
                @Suppress("DEPRECATION")
                am.abandonAudioFocus(null)
            }
        }
    }
} 