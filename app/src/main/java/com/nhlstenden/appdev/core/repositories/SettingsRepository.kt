package com.nhlstenden.appdev.core.repositories

interface SettingsRepository {
    suspend fun hasValue(key: String): Boolean
    suspend fun addValue(key: String)
    suspend fun removeValue(key: String)
    suspend fun toggleValue(key: String)
}
