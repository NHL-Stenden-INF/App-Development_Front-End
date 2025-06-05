package com.nhlstenden.appdev.core.utils

import com.nhlstenden.appdev.core.models.User

object UserManager {
    private var currentUser: User? = null
    
    fun setCurrentUser(user: User) {
        currentUser = user
    }
    
    fun getCurrentUser(): User? = currentUser
    
    fun clearCurrentUser() {
        currentUser = null
    }
    
    fun isLoggedIn(): Boolean {
        return currentUser != null
    }
    
    fun logout() {
        currentUser = null
    }
} 