package com.nhlstenden.appdev.features.profile.repositories

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.preferencesDataStore
import com.nhlstenden.appdev.core.repositories.SettingsRepository
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import javax.inject.Inject
import javax.inject.Singleton

val Context.dataStore by preferencesDataStore("settings")

@Singleton
class SettingsRepositoryImpl @Inject constructor(
    private val context: Context
) : SettingsRepository {
    object SettingsConstants {
        const val BIOMETRICS = "biometric_enabled"
        const val ACHIEVEMENTS_NOTIFICATIONS = "achievement_notifications"
        const val PROGRESS_NOTIFICATIONS = "progress_notifications"
        const val FRIENDS_ACTIVITY = "friend_activity"
        const val COURSE_LOBBY_MUSIC = "course_lobby_music"
    }

    override fun hasValue(key: String): Boolean = runBlocking {
        val preferences = context.dataStore.data.first()
        preferences[booleanPreferencesKey(key)] == true
    }

    override suspend fun addValue(key: String) {
        context.dataStore.edit { preferences ->
            preferences[booleanPreferencesKey(key)] = true
        }
    }

    override suspend fun removeValue(key: String) {
        context.dataStore.edit { preferences ->
            preferences[booleanPreferencesKey(key)] = false
        }
    }

    override suspend fun toggleValue(key: String) {
        context.dataStore.edit { preferences ->
            val currentValue = preferences[booleanPreferencesKey(key)] ?: false
            preferences[booleanPreferencesKey(key)] = !currentValue
        }
    }
}
