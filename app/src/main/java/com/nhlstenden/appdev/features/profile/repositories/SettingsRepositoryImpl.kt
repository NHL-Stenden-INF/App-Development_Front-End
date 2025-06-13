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

    override fun hasValue(key: String): Boolean = runBlocking {
        val preferences = context.dataStore.data.first()
        preferences[booleanPreferencesKey(key)] == true
    }

    override fun addValue(key: String) = runBlocking {
        context.dataStore.edit { preferences ->
            preferences[booleanPreferencesKey(key)] = true
        }
    }

    override fun removeValue(key: String) = runBlocking {
        context.dataStore.edit { preferences ->
            preferences[booleanPreferencesKey(key)] = false
        }
    }

    override fun toggleValue(key: String) = runBlocking{
        context.dataStore.edit { preferences ->
            val currentValue = preferences[booleanPreferencesKey(key)] ?: false
            preferences[booleanPreferencesKey(key)] = !currentValue
        }
    }
}
