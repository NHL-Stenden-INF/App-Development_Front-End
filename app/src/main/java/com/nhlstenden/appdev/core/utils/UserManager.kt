package com.nhlstenden.appdev.core.utils

import com.nhlstenden.appdev.core.models.User

/**
 * @deprecated UserManager is deprecated. Use AuthRepository instead for better architecture.
 * 
 * Migration guide:
 * - Replace UserManager.getCurrentUser() with authRepository.getCurrentUserSync()
 * - Replace UserManager.setCurrentUser() with authRepository.login() or authRepository.register()
 * - Replace UserManager.logout() with authRepository.logout()
 * - Replace UserManager.isLoggedIn() with authRepository.isLoggedIn()
 * 
 * Benefits of using AuthRepository:
 * - Persistent session storage (survives app restart)
 * - Reactive state management with Flow
 * - Proper dependency injection
 * - Better testability
 * - Thread-safe operations
 */
@Deprecated(
    message = "Use AuthRepository instead for better architecture and persistent storage",
    replaceWith = ReplaceWith("AuthRepository"),
    level = DeprecationLevel.WARNING
)
object UserManager {
    private var currentUser: User? = null
    
    @Deprecated("Use authRepository.login() or authRepository.register() instead")
    fun setCurrentUser(user: User) {
        currentUser = user
    }
    
    @Deprecated("Use authRepository.getCurrentUserSync() instead")
    fun getCurrentUser(): User? = currentUser
    
    @Deprecated("Use authRepository.logout() instead")
    fun clearCurrentUser() {
        currentUser = null
    }
    
    @Deprecated("Use authRepository.isLoggedIn() instead")
    fun isLoggedIn(): Boolean {
        return currentUser != null
    }
    
    @Deprecated("Use authRepository.logout() instead")
    fun logout() {
        currentUser = null
    }
} 