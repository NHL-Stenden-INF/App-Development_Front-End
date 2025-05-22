package com.nhlstenden.appdev.models

import com.nhlstenden.appdev.User

/**
 * Singleton class for managing user state throughout the application
 */
object UserManager {
    private var currentUser: User? = null
    
    fun setCurrentUser(user: User?) {
        currentUser = user
    }
    
    fun getCurrentUser(): User? {
        return currentUser
    }
    
    fun isLoggedIn(): Boolean {
        return currentUser != null
    }
    
    fun logout() {
        currentUser = null
    }
} 