package com.nhlstenden.appdev.core.repositories

import androidx.datastore.preferences.core.Preferences

interface SettingsRepository {
    fun hasValue(key: String): Boolean
    suspend fun addValue(key: String)
    suspend fun removeValue(key: String)
    suspend fun toggleValue(key: String)
}
