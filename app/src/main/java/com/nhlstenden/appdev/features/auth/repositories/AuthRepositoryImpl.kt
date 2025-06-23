package com.nhlstenden.appdev.features.auth.repositories

import android.content.Context
import android.content.SharedPreferences
import android.util.Log
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import com.nhlstenden.appdev.core.models.User
import com.nhlstenden.appdev.core.repositories.AuthRepository
import com.nhlstenden.appdev.supabase.*
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import org.json.JSONObject
import javax.crypto.AEADBadTagException
import javax.inject.Inject
import javax.inject.Singleton
import java.util.Date
import java.util.concurrent.TimeUnit

@Singleton
class AuthRepositoryImpl @Inject constructor(
    @ApplicationContext private val context: Context,
    private val supabaseClient: SupabaseClient
) : AuthRepository {

    private val TAG = "AuthRepositoryImpl"
    
    private val _currentUser = MutableStateFlow<User?>(null)
    override fun getCurrentUser(): Flow<User?> = _currentUser.asStateFlow()
    
    private val sharedPreferences: SharedPreferences by lazy {
        createEncryptedSharedPreferences()
    }
    
    private fun createEncryptedSharedPreferences(): SharedPreferences {
        return try {
            val masterKey = MasterKey.Builder(context)
                .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
                .build()
                
            EncryptedSharedPreferences.create(
                context,
                "auth_prefs",
                masterKey,
                EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
                EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
            )
        } catch (e: Exception) {
            Log.w(TAG, "Failed to create encrypted shared preferences, attempting recovery", e)
            
            // If creation fails due to corrupted data, clear the preferences and try again
            when (e) {
                is AEADBadTagException, 
                is java.security.GeneralSecurityException -> {
                    Log.i(TAG, "Encryption error detected, clearing corrupted data and recreating preferences")
                    clearCorruptedEncryptedPreferences()
                    createEncryptedSharedPreferencesAfterClearing()
                }
                else -> {
                    Log.e(TAG, "Unexpected error creating encrypted shared preferences", e)
                    throw e
                }
            }
        }
    }
    
    private fun clearCorruptedEncryptedPreferences() {
        try {
            // Clear the corrupted encrypted shared preferences file
            val prefsFile = context.getSharedPreferences("auth_prefs", Context.MODE_PRIVATE)
            prefsFile.edit().clear().apply()
            
            // Also try to delete the actual file if possible
            val file = context.getFileStreamPath("auth_prefs.xml")
            if (file.exists()) {
                file.delete()
            }
        } catch (e: Exception) {
            Log.w(TAG, "Error clearing corrupted preferences", e)
        }
    }
    
    private fun createEncryptedSharedPreferencesAfterClearing(): SharedPreferences {
        return try {
            val masterKey = MasterKey.Builder(context)
                .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
                .build()
                
            EncryptedSharedPreferences.create(
                context,
                "auth_prefs",
                masterKey,
                EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
                EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
            )
        } catch (e: Exception) {
            Log.e(TAG, "Failed to create encrypted shared preferences after clearing corrupted data", e)
            throw e
        }
    }
    
    init {
        // Restore user session on initialization
        restoreUserSession()
    }
    
    override fun getCurrentUserSync(): User? = _currentUser.value
    
    override suspend fun login(email: String, password: String): Result<User> {
        val tokenResult = supabaseClient.login(email, password)

        if (tokenResult.isFailure) {
            val error = tokenResult.exceptionOrNull() ?: Exception("Unknown login error")
            Log.e(TAG, "Login failed", error)
            return Result.failure(error)
        }

        val accessToken = tokenResult.getOrThrow()
        val profileResult = supabaseClient.fetchProfileOrCreate(accessToken, "", email)

        if (profileResult.isFailure) {
            return Result.failure(profileResult.exceptionOrNull()!!)
        }

        val profile = profileResult.getOrThrow()

        return try {
            // Fetch user attributes for any future needs
            supabaseClient.fetchUserAttributesOrCreate(accessToken)
            
            val user = User(
                id = profile.getString("id"),
                email = email, // Use the email from login parameters
                username = profile.optString("display_name", ""),
                profilePicture = profile.optString("profile_picture"),
                authToken = accessToken
            )
            
            // Persist session
            saveUserSession(user)
            _currentUser.value = user
            
            Log.d(TAG, "User logged in successfully: ${user.email}")
            Result.success(user)
        } catch (e: Exception) {
            Log.e(TAG, "Login failed", e)
            Result.failure(e)
        }
    }
    
    override suspend fun register(email: String, password: String, displayName: String): Result<User> {
        val tokenResult = supabaseClient.register(email, password, displayName)

        if (tokenResult.isFailure) {
            val error = tokenResult.exceptionOrNull() ?: Exception("Unknown register error")
            Log.e(TAG, "Registration failed", error)
        }

        val accessToken = tokenResult.getOrThrow()
        val profileResult = supabaseClient.fetchProfileOrCreate(accessToken, displayName, email)

        if (profileResult.isFailure) {
            return Result.failure(profileResult.exceptionOrNull()!!)
        }

        val profile = profileResult.getOrThrow()

        return try {
            // Fetch user attributes for any future needs
            supabaseClient.fetchUserAttributesOrCreate(accessToken)
            val user = User(
                id = profile.getString("id"),
                email = email, // Use the email from register parameters
                username = profile.optString("display_name", displayName),
                profilePicture = profile.optString("profile_picture"),
                authToken = accessToken
            )
            
            // Persist session
            saveUserSession(user)
            _currentUser.value = user
            
            Log.d(TAG, "User registered successfully: ${user.email}")
            Result.success(user)
        } catch (e: Exception) {
            Log.e(TAG, "Registration failed", e)
            Result.failure(e)
        }
    }
    
    override suspend fun logout(): Result<Unit> {
        return try {
            clearUserSession()
            _currentUser.value = null
            Log.d(TAG, "User logged out successfully")
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "Logout failed", e)
            Result.failure(e)
        }
    }
    
    override fun isLoggedIn(): Boolean = _currentUser.value != null
    
    override suspend fun handleJWTExpiration() {
        Log.w(TAG, "JWT expired - clearing session and logging out user")
        clearUserSession()
        _currentUser.value = null
    }
    
    override suspend fun refreshUserData(): Result<User> {
        val currentUser = _currentUser.value ?: return Result.failure(Exception("No user logged in"))
        
        return try {
            val profileResult = supabaseClient.fetchProfileOrCreate(currentUser.authToken, currentUser.username, currentUser.email)

            if (profileResult.isFailure) {
                return Result.failure(profileResult.exceptionOrNull() ?: Exception("Failed to fetch profile"))
            }

            val profile = profileResult.getOrThrow()
            // Fetch user attributes for any future needs
            supabaseClient.fetchUserAttributesOrCreate(currentUser.authToken)
            
            val updatedUser = currentUser.copy(
                username = profile.optString("display_name", currentUser.username),
                profilePicture = profile.optString("profile_picture", currentUser.profilePicture)
                // Keep the existing email since it's not in the profile table
            )
            
            saveUserSession(updatedUser)
            _currentUser.value = updatedUser
            
            Log.d(TAG, "User data refreshed successfully")
            Result.success(updatedUser)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to refresh user data", e)
            Result.failure(e)
        }
    }
    
    private fun saveUserSession(user: User) {
        try {
            sharedPreferences.edit()
                .putString("user_id", user.id)
                .putString("user_email", user.email)
                .putString("user_username", user.username)
                .putString("user_profile_picture", user.profilePicture)
                .putString("auth_token", user.authToken)
                .apply()
        } catch (e: Exception) {
            Log.w(TAG, "Failed to save user session", e)
            // Don't crash the app, authentication will still work for this session
        }
    }
    
    private fun restoreUserSession() {
        try {
            val userId = sharedPreferences.getString("user_id", null)
            val email = sharedPreferences.getString("user_email", null)
            val authToken = sharedPreferences.getString("auth_token", null)
            
            if (userId != null && email != null && authToken != null) {
                val user = User(
                    id = userId,
                    email = email,
                    username = sharedPreferences.getString("user_username", "")!!,
                    profilePicture = sharedPreferences.getString("user_profile_picture", null),
                    authToken = authToken
                )
                _currentUser.value = user
                Log.d(TAG, "User session restored: ${user.email}")
            } else {
                Log.d(TAG, "No saved user session found")
            }
        } catch (e: Exception) {
            Log.w(TAG, "Failed to restore user session, user will need to log in again", e)
            // Don't crash the app, just leave currentUser as null
            // The user will need to log in again
        }
    }
    
    private fun clearUserSession() {
        try {
            sharedPreferences.edit().clear().apply()
        } catch (e: Exception) {
            Log.w(TAG, "Failed to clear user session", e)
            // Don't crash the app, user will still be logged out in memory
        }
    }
    
    private fun isTokenExpired(token: String): Boolean {
        try {
            val jwt = com.auth0.android.jwt.JWT(token)
            val expiresAt = jwt.expiresAt
            val now = Date()
            // If the token has an expiry, check if it's more than 24 hours old
            if (expiresAt != null) {
                val diff = expiresAt.time - now.time
                // 24 hours = 86400000 ms
                return diff < 0 || diff > 86400000
            }
            return false
        } catch (e: Exception) {
            Log.w(TAG, "Invalid JWT token", e)
            return true
        }
    }
} 