package com.nhlstenden.appdev.core.utils

import android.util.Log
import com.nhlstenden.appdev.core.repositories.AuthRepository
import okhttp3.Response
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class JwtHandler @Inject constructor(
    private val authRepository: AuthRepository
) {
    companion object {
        private const val TAG = "JwtHandler"
        private const val JWT_EXPIRED_MESSAGE = "JWT expired"
        private const val SESSION_EXPIRED_MESSAGE = "Session expired. Please login again."
    }

    suspend fun handleJwtResponse(response: Response): Boolean {
        if (response.code == 401) {
            val body = response.body?.string()
            if (body?.contains(JWT_EXPIRED_MESSAGE) == true) {
                Log.w(TAG, "JWT expired detected, clearing session")
                authRepository.handleJWTExpiration()
                return true
            }
        }
        return false
    }

    fun createSessionExpiredException(): Exception {
        return Exception(SESSION_EXPIRED_MESSAGE)
    }
} 