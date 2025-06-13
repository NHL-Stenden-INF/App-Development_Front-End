package com.nhlstenden.appdev.core.repositories

import androidx.datastore.preferences.core.Preferences

interface SettingsRepository {
    fun hasValue(key: String): Boolean
    fun addValue(key: String): Preferences
    fun removeValue(key: String): Preferences
    fun toggleValue(key: String): Preferences
}
