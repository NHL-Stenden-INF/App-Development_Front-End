package com.nhlstenden.appdev.features.auth.repositories

import android.content.Context
import android.content.SharedPreferences
import android.util.Log
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import com.nhlstenden.appdev.core.models.User
import com.nhlstenden.appdev.core.repositories.AuthRepository
import com.nhlstenden.appdev.supabase.SupabaseClient
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import org.json.JSONObject
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AuthRepositoryImpl @Inject constructor(
    @ApplicationContext private val context: Context,
    private val supabaseClient: SupabaseClient
) : AuthRepository {

    private val TAG = "AuthRepositoryImpl"
    
    private val _currentUser = MutableStateFlow<User?>(null)
    override fun getCurrentUser(): Flow<User?> = _currentUser.asStateFlow()
    
    private val sharedPreferences: SharedPreferences by lazy {
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
    }
    
    init {
        // Restore user session on initialization
        restoreUserSession()
    }
    
    override fun getCurrentUserSync(): User? = _currentUser.value
    
    override suspend fun login(email: String, password: String): Result<User> {
        return try {
            val accessToken = supabaseClient.login(email, password)
            val profile = supabaseClient.fetchProfile(accessToken)
            // Fetch user attributes for any future needs
            supabaseClient.fetchUserAttributes(accessToken)
            
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
        return try {
            val accessToken = supabaseClient.register(email, password, displayName)
            val profile = supabaseClient.fetchProfile(accessToken)
            // Fetch user attributes for any future needs
            supabaseClient.fetchUserAttributes(accessToken)
            
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
            val profile = supabaseClient.fetchProfile(currentUser.authToken)
            // Fetch user attributes for any future needs
            supabaseClient.fetchUserAttributes(currentUser.authToken)
            
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
        sharedPreferences.edit()
            .putString("user_id", user.id)
            .putString("user_email", user.email)
            .putString("user_username", user.username)
            .putString("user_profile_picture", user.profilePicture)
            .putString("auth_token", user.authToken)
            .apply()
    }
    
    private fun restoreUserSession() {
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
        }
    }
    
    private fun clearUserSession() {
        sharedPreferences.edit().clear().apply()
    }
} 