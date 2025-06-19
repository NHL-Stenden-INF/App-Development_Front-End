package com.nhlstenden.appdev.features.rewards.repositories

import android.content.Context
import android.util.Log
import com.nhlstenden.appdev.R
import dagger.hilt.android.qualifiers.ApplicationContext
import com.nhlstenden.appdev.core.models.Achievement
import com.nhlstenden.appdev.core.repositories.AchievementRepository
import com.nhlstenden.appdev.core.repositories.AuthRepository
import com.nhlstenden.appdev.core.repositories.UserRepository
import com.nhlstenden.appdev.supabase.*
import com.nhlstenden.appdev.features.courses.TaskParser
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AchievementRepositoryImpl @Inject constructor(
    @ApplicationContext private val context: Context,
    private val supabaseClient: SupabaseClient,
    private val authRepository: AuthRepository,
    private val userRepository: UserRepository,
    private val taskParser: TaskParser
) : AchievementRepository {

    private val TAG = "AchievementRepository"

    override suspend fun getUserUnlockedAchievements(userId: String): Result<List<Int>> {
        return try {
            withContext(Dispatchers.IO) {
                val currentUser = authRepository.getCurrentUserSync()
                    ?: return@withContext Result.failure(Exception("No authenticated user"))

                Log.d(TAG, "Fetching unlocked achievements for user: $userId")
                val responseResult = supabaseClient.getUserUnlockedAchievements(userId, currentUser.authToken)

                if (responseResult.isFailure) {
                    Log.e(TAG, "Network error while fetching achievements", responseResult.exceptionOrNull())
                    return@withContext Result.failure(responseResult.exceptionOrNull()!!)
                }

                val response =responseResult.getOrThrow()
                
                if (response.isSuccessful) {
                    val responseBody = response.body?.string() ?: ""
                    Log.d(TAG, "Achievement response: $responseBody")
                    
                    if (responseBody.trim().isEmpty() || responseBody.trim() == "[]") {
                        Log.d(TAG, "No achievements found for user")
                        return@withContext Result.success(emptyList())
                    }
                    
                    val jsonArray = JSONArray(responseBody)
                    val unlockedIds = mutableListOf<Int>()
                    
                    for (i in 0 until jsonArray.length()) {
                        val achievement = jsonArray.getJSONObject(i)
                        unlockedIds.add(achievement.getInt("achievement_id"))
                    }
                    
                    Log.d(TAG, "Found ${unlockedIds.size} unlocked achievements: $unlockedIds")
                    Result.success(unlockedIds)
                } else {
                    val errorBody = response.body?.string() ?: "Unknown error"
                    Log.e(TAG, "Failed to fetch achievements: ${response.code} - $errorBody")
                    
                    if (response.code == 401) {
                        authRepository.handleJWTExpiration()
                        Result.failure(Exception("Session expired"))
                    } else if (response.code == 404) {
                        // Table might not exist or no achievements yet
                        Log.w(TAG, "Achievements table not found or no achievements - this is normal for new users")
                        Result.success(emptyList())
                    } else {
                        Result.failure(Exception("Failed to fetch achievements: ${response.code} - $errorBody"))
                    }
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error fetching user achievements", e)
            // Return empty list instead of failure for better UX
            Result.success(emptyList())
        }
    }

    override suspend fun unlockAchievement(userId: String, achievementId: Int): Result<Unit> {
        return try {
            withContext(Dispatchers.IO) {
                val currentUser = authRepository.getCurrentUserSync()
                    ?: return@withContext Result.failure(Exception("No authenticated user"))

                Log.d(TAG, "Attempting to unlock achievement $achievementId for user $userId")


                val responseResult = supabaseClient.unlockAchievement(userId, achievementId, currentUser.authToken)

                if (responseResult.isFailure) {
                    Log.e(TAG, "Network error while unlocking achievement", responseResult.exceptionOrNull())
                }

                val response = responseResult.getOrThrow()
                
                if (response.isSuccessful) {
                    Log.d(TAG, "Successfully unlocked achievement $achievementId for user $userId")
                    Result.success(Unit)
                } else {
                    val errorBody = response.body?.string() ?: "Unknown error"
                    Log.e(TAG, "Failed to unlock achievement $achievementId: ${response.code} - $errorBody")
                    
                    if (response.code == 401) {
                        authRepository.handleJWTExpiration()
                        Result.failure(Exception("Session expired"))
                    } else if (response.code == 409) {
                        // Achievement already unlocked - treat as success
                        Log.d(TAG, "Achievement $achievementId already unlocked for user $userId")
                        Result.success(Unit)
                    } else if (response.code == 404) {
                        Log.e(TAG, "Achievement table not found - database may not be set up correctly")
                        Result.failure(Exception("Database not properly configured - run database_setup.sql"))
                    } else {
                        Result.failure(Exception("Failed to unlock achievement: ${response.code} - $errorBody"))
                    }
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error unlocking achievement $achievementId", e)
            Result.failure(e)
        }
    }

    override suspend fun getAllAchievements(): List<Achievement> {
        return try {
            val resources = context.resources
            val ids = resources.getIntArray(R.array.achievement_ids)
            val titles = resources.getStringArray(R.array.achievement_titles)
            val descriptions = resources.getStringArray(R.array.achievement_descriptions)
            val iconResourceNames = resources.getStringArray(R.array.achievement_icons)
            
            titles.indices.map { i ->
                val iconResId = resources.getIdentifier(
                    iconResourceNames[i], 
                    "drawable", 
                    context.packageName
                )
                Achievement(
                    id = ids[i].toString(),
                    title = titles[i],
                    description = descriptions[i],
                    iconResId = if (iconResId == 0) R.drawable.ic_achievement else iconResId,
                    unlocked = false
                )
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error loading achievements", e)
            emptyList()
        }
    }

    override suspend fun checkAndUnlockAchievements(userId: String): Result<List<Achievement>> {
        return try {
            Log.d(TAG, "🎯 === STARTING ACHIEVEMENT CHECK FOR USER: $userId ===")
            val newlyUnlocked = mutableListOf<Achievement>()
            
            Log.d(TAG, "Step 1: Checking course completion achievements...")
            val courseAchievements = checkCourseCompletionAchievements(userId)
            Log.d(TAG, "Course achievements found: ${courseAchievements.size}")
            newlyUnlocked.addAll(courseAchievements)
            
            Log.d(TAG, "Step 2: Checking streak achievement...")
            val streakAchievement = checkStreakAchievementInDatabase(userId)
            if (streakAchievement != null) {
                Log.d(TAG, "Streak achievement found: ${streakAchievement.title}")
                newlyUnlocked.add(streakAchievement)
            } else {
                Log.d(TAG, "No streak achievement unlocked")
            }
            
            Log.d(TAG, "🎯 === ACHIEVEMENT CHECK COMPLETE: ${newlyUnlocked.size} total newly unlocked ===")
            Result.success(newlyUnlocked)
        } catch (e: Exception) {
            Log.e(TAG, "Error checking achievements", e)
            Result.failure(e)
        }
    }

    private suspend fun checkCourseCompletionAchievements(userId: String): List<Achievement> {
        val newlyUnlocked = mutableListOf<Achievement>()
        
        try {
            Log.d(TAG, "=== Starting course completion achievement check for user: $userId ===")
            
            // Check HTML completion (achievement ID 1)
            Log.d(TAG, "Checking HTML course completion...")
            val htmlCompleted = isCourseCompleted(userId, "html")
            Log.d(TAG, "HTML course completed: $htmlCompleted")
            if (htmlCompleted) {
                Log.d(TAG, "Attempting to unlock HTML Hero achievement...")
                val htmlAchievement = tryUnlockAchievementIfNotExists(userId, 1, "HTML Hero")
                htmlAchievement?.let { 
                    newlyUnlocked.add(it)
                    Log.d(TAG, "HTML Hero achievement added to newly unlocked list")
                } ?: Log.d(TAG, "HTML Hero achievement was not newly unlocked")
            }
            
            // Check CSS completion (achievement ID 2)
            Log.d(TAG, "Checking CSS course completion...")
            val cssCompleted = isCourseCompleted(userId, "css")
            Log.d(TAG, "CSS course completed: $cssCompleted")
            if (cssCompleted) {
                Log.d(TAG, "Attempting to unlock CSS Sorcerer achievement...")
                val cssAchievement = tryUnlockAchievementIfNotExists(userId, 2, "CSS Sorcerer")
                cssAchievement?.let { 
                    newlyUnlocked.add(it)
                    Log.d(TAG, "CSS Sorcerer achievement added to newly unlocked list")
                } ?: Log.d(TAG, "CSS Sorcerer achievement was not newly unlocked")
            }
            
            // Check SQL completion (achievement ID 3)
            Log.d(TAG, "Checking SQL course completion...")
            val sqlCompleted = isCourseCompleted(userId, "sql")
            Log.d(TAG, "SQL course completed: $sqlCompleted")
            if (sqlCompleted) {
                Log.d(TAG, "Attempting to unlock SQL Sleuth achievement...")
                val sqlAchievement = tryUnlockAchievementIfNotExists(userId, 3, "SQL Sleuth")
                sqlAchievement?.let { 
                    newlyUnlocked.add(it)
                    Log.d(TAG, "SQL Sleuth achievement added to newly unlocked list")
                } ?: Log.d(TAG, "SQL Sleuth achievement was not newly unlocked")
            }
            
            Log.d(TAG, "=== Course completion check finished. Found ${newlyUnlocked.size} newly unlocked achievements ===")
            
        } catch (e: Exception) {
            Log.e(TAG, "Error checking course completion achievements", e)
        }
        
        return newlyUnlocked
    }

    private suspend fun checkStreakAchievementInDatabase(userId: String): Achievement? {
        return try {
            withContext(Dispatchers.IO) {
                val currentUser = authRepository.getCurrentUserSync() ?: return@withContext null

                val streakResponseResult = supabaseClient.checkStreakAchievement(userId, currentUser.authToken)

                if (streakResponseResult.isFailure) {
                    Log.e(TAG, "Error while checking streak achievent")
                    return@withContext null
                }

                val streakResponse = streakResponseResult.getOrThrow()


                if (streakResponse.isSuccessful) {
                    val responseBody = streakResponse.body?.string() ?: ""
                    Log.d(TAG, "Streak achievement response: $responseBody")
                    
                    if (responseBody.trim().isNotEmpty() && responseBody.trim() != "[]") {
                        val jsonArray = JSONArray(responseBody)
                        for (i in 0 until jsonArray.length()) {
                            val achievementData = jsonArray.getJSONObject(i)
                            val achievementId = achievementData.getInt("achievement_id")
                            val title = achievementData.getString("achievement_title")
                            val newlyUnlockedFlag = achievementData.getBoolean("newly_unlocked")
                            
                            if (newlyUnlockedFlag) {
                                Log.d(TAG, "Streak achievement unlocked: $title (ID: $achievementId)")
                                return@withContext createAchievement(achievementId, title, getDescriptionForAchievement(achievementId), getIconForAchievement(achievementId))
                            }
                        }
                    }
                } else {
                    Log.e(TAG, "Failed to check streak achievement: ${streakResponse.code}")
                }
                null
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error checking streak achievement", e)
            null
        }
    }

    private suspend fun tryUnlockAchievementIfNotExists(userId: String, achievementId: Int, title: String): Achievement? {
        return try {
            withContext(Dispatchers.IO) {
                val currentUser = authRepository.getCurrentUserSync() ?: return@withContext null

                val responseResult = supabaseClient.unlockAchievementIfNotExists(userId, achievementId, title, currentUser.authToken)

                if (responseResult.isFailure) {
                    Log.e(TAG, "Error while unlocking achievement $title", responseResult.exceptionOrNull())
                    return@withContext null
                }

                val response = responseResult.getOrThrow()

                if (response.isSuccessful) {
                    val responseBody = response.body?.string() ?: ""
                    Log.d(TAG, "Unlock achievement response: $responseBody")
                    
                    if (responseBody.trim().isNotEmpty() && responseBody.trim() != "[]") {
                        val jsonArray = JSONArray(responseBody)
                        if (jsonArray.length() > 0) {
                            val achievementData = jsonArray.getJSONObject(0)
                            val newlyUnlockedFlag = achievementData.getBoolean("result_newly_unlocked")
                            val returnedAchievementId = achievementData.getInt("result_achievement_id")
                            val returnedTitle = achievementData.getString("result_achievement_title")
                            
                            Log.d(TAG, "Function returned: ID=$returnedAchievementId, title='$returnedTitle', newly_unlocked=$newlyUnlockedFlag")
                            
                            if (newlyUnlockedFlag) {
                                Log.d(TAG, "Achievement unlocked: $title (ID: $achievementId)")
                                return@withContext createAchievement(achievementId, title, getDescriptionForAchievement(achievementId), getIconForAchievement(achievementId))
                            } else {
                                Log.d(TAG, "Achievement $title already unlocked for user")
                            }
                        }
                    }
                } else {
                    val errorBody = response.body?.string() ?: "No error details"
                    Log.e(TAG, "Failed to unlock achievement $title: ${response.code}")
                    Log.e(TAG, "Database function error response: $errorBody")
                }
                null
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error unlocking achievement $title", e)
            null
        }
    }

    private suspend fun isCourseCompleted(userId: String, courseId: String): Boolean {
        return try {
            Log.d(TAG, "Checking if course $courseId is completed for user $userId")
            val currentUser = authRepository.getCurrentUserSync() 
            if (currentUser == null) {
                Log.e(TAG, "No current user found - cannot check course completion")
                return false
            }
            
            Log.d(TAG, "Getting user progress for course completion check...")
            val progressResult = supabaseClient.getUserProgress(userId, currentUser.authToken)

            if (progressResult.isFailure) {
                Log.e(TAG, "Failed to fetch user progress", progressResult.exceptionOrNull())
                return false
            }

            val progressArray = progressResult.getOrNull() ?: return false
            Log.d(TAG, "Progress array length: ${progressArray.length()}")
            
            // Log all progress entries for debugging
            for (i in 0 until progressArray.length()) {
                val progress = progressArray.getJSONObject(i)
                val dbCourseId = progress.getString("course_id")
                val dbProgress = progress.getInt("progress")
                Log.d(TAG, "Found progress entry: course='$dbCourseId', progress=$dbProgress")
            }
            
            for (i in 0 until progressArray.length()) {
                val progress = progressArray.getJSONObject(i)
                val dbCourseId = progress.getString("course_id")
                
                if (dbCourseId.equals(courseId, ignoreCase = true)) {
                    val completedTasks = progress.getInt("progress")
                    val totalTasks = getTotalTasksForCourse(courseId)
                    Log.d(TAG, "Course $courseId match found: $completedTasks/$totalTasks tasks completed")
                    val isCompleted = completedTasks >= totalTasks
                    Log.d(TAG, "Course $courseId completion status: $isCompleted")
                    return isCompleted
                }
            }
            
            Log.d(TAG, "No progress entry found for course $courseId - course not started or course_id mismatch")
            false
        } catch (e: Exception) {
            Log.e(TAG, "Error checking course completion for $courseId", e)
            false
        }
    }

    private fun getTotalTasksForCourse(courseId: String): Int {
        return try {
            val tasks = taskParser.loadAllTasksOfCourse(courseId)
            Log.d(TAG, "Course $courseId has ${tasks.size} total tasks")
            tasks.size
        } catch (e: Exception) {
            Log.e(TAG, "Error getting total tasks for course $courseId", e)
            // Fallback values based on typical course sizes
            when (courseId.lowercase()) {
                "html" -> 10
                "css" -> 10  
                "sql" -> 10
                else -> 10
            }
        }
    }

    private fun createAchievement(id: Int, title: String, description: String, iconName: String): Achievement {
        val iconResId = context.resources.getIdentifier(iconName, "drawable", context.packageName)
        return Achievement(
            id = id.toString(),
            title = title,
            description = description,
            iconResId = if (iconResId == 0) R.drawable.ic_achievement else iconResId,
            unlocked = true
        )
    }
    
    private fun getDescriptionForAchievement(achievementId: Int): String {
        return when (achievementId) {
            1 -> "Complete all HTML challenges"
            2 -> "Complete all CSS challenges"
            3 -> "Complete all SQL challenges"
            5 -> "Maintain a 7-day learning streak"
            else -> "Achievement unlocked"
        }
    }
    
    private fun getIconForAchievement(achievementId: Int): String {
        return when (achievementId) {
            1 -> "ic_achievement_html"
            2 -> "ic_achievement_css"
            3 -> "ic_achievement_sql"
            5 -> "ic_achievement_streak"
            else -> "ic_achievement"
        }
    }
} 