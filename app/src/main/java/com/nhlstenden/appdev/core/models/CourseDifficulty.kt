package com.nhlstenden.appdev.core.models

enum class CourseDifficulty(val stars: Int, val displayName: String) {
    VERY_EASY(1, "Very Easy"),
    EASY(2, "Easy"),
    MEDIUM(3, "Medium"),
    HARD(4, "Hard"),
    VERY_HARD(5, "Very Hard");
    
    companion object {
        fun fromStars(stars: Int): CourseDifficulty? = 
            values().find { it.stars == stars }
    }
} 