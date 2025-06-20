package com.nhlstenden.appdev.core.utils

object DifficultyFormatter {
    private const val FILLED_STAR = "★"
    private const val EMPTY_STAR = "☆"
    private const val MAX_STARS = 5
    
    fun formatStars(difficulty: Int): String {
        val clampedDifficulty = difficulty.coerceIn(1, MAX_STARS)
        return FILLED_STAR.repeat(clampedDifficulty) + EMPTY_STAR.repeat(MAX_STARS - clampedDifficulty)
    }
    
    fun getStarOptions(): Array<String> = 
        (1..MAX_STARS).map { formatStars(it) }.toTypedArray()
} 