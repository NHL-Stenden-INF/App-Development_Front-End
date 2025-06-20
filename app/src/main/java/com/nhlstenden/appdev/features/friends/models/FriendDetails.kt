package com.nhlstenden.appdev.features.friends.models

data class FriendDetails(
    val id: String,
    val username: String,
    val profilePicture: String?,
    val bio: String? = null,
    val totalPoints: Int = 0,
    val level: Int = 1,
    val currentLevelProgress: Int = 0,
    val currentLevelMax: Int = 100,
    val lastActive: Long = System.currentTimeMillis(),
    val courseProgress: List<CourseProgress> = emptyList(),
    val achievements: List<String> = emptyList(),
    val joinDate: String? = null,
    val streakDays: Int = 0
)

data class CourseProgress(
    val courseId: String,
    val courseName: String,
    val progress: Int, // Percentage (0-100)
    val tasksCompleted: Int,
    val totalTasks: Int,
    val lastActivity: Long = System.currentTimeMillis()
) 