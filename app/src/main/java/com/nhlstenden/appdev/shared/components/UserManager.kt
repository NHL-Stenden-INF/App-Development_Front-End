package com.nhlstenden.appdev.shared.components

import com.nhlstenden.appdev.supabase.User

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