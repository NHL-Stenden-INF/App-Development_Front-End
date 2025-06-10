package com.nhlstenden.appdev.utils

/**
 * Centralized utility class for level calculations and progress tracking.
 * This ensures consistent level calculation logic across the entire application.
 */
object LevelCalculator {
    
    /**
     * Calculate the level from total XP using the standard progression formula.
     * Each level requires 10% more XP than the previous level.
     * 
     * @param xp Total experience points
     * @return Current level (minimum 1)
     */
    fun calculateLevelFromXp(xp: Long): Int {
        var level = 1
        var requiredXp = 100L
        var totalXp = 0L
        
        while (xp >= totalXp + requiredXp) {
            totalXp += requiredXp
            level++
            requiredXp = (requiredXp * 1.1).toLong() // 10% more XP per level
        }
        
        return level
    }
    
    /**
     * Calculate XP progress for the current level.
     * 
     * @param xp Total experience points
     * @return Pair of (currentLevelProgress, currentLevelMax)
     */
    fun calculateLevelProgress(xp: Long): Pair<Int, Int> {
        val level = calculateLevelFromXp(xp)
        
        // Calculate total XP required for current level
        var requiredXp = 100.0
        var totalXp = 0.0
        
        for (i in 1 until level) {
            totalXp += requiredXp
            requiredXp *= 1.1
        }
        
        val currentLevelProgress = (xp - totalXp.toLong()).coerceAtLeast(0L).toInt()
        val currentLevelMax = requiredXp.toInt()
        
        return Pair(currentLevelProgress, currentLevelMax)
    }
    
    /**
     * Calculate level and progress information in a single call.
     * 
     * @param xp Total experience points
     * @return Triple of (level, currentLevelProgress, currentLevelMax)
     */
    fun calculateLevelAndProgress(xp: Long): Triple<Int, Int, Int> {
        val level = calculateLevelFromXp(xp)
        val (currentLevelProgress, currentLevelMax) = calculateLevelProgress(xp)
        
        return Triple(level, currentLevelProgress, currentLevelMax)
    }
} 