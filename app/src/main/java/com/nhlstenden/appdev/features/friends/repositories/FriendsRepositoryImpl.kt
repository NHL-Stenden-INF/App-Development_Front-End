package com.nhlstenden.appdev.features.friends.repositories

import android.util.Log
import com.nhlstenden.appdev.core.repositories.FriendsRepository
import com.nhlstenden.appdev.features.friends.models.Friend
import com.nhlstenden.appdev.features.friends.models.FriendDetails
import com.nhlstenden.appdev.features.friends.models.CourseProgress
import com.nhlstenden.appdev.supabase.SupabaseClient
import com.nhlstenden.appdev.features.courses.CourseParser
import com.nhlstenden.appdev.features.courses.TaskParser
import org.json.JSONArray
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class FriendsRepositoryImpl @Inject constructor(
    private val supabaseClient: SupabaseClient,
    private val courseParser: CourseParser,
    private val taskParser: TaskParser
) : FriendsRepository {

    private val TAG = "FriendsRepositoryImpl"

    override suspend fun addFriend(friendId: String): Result<Unit> {
        return try {
            val response = supabaseClient.addFriendWithCurrentUser(friendId)
            if (response.isSuccessful) {
                Log.d(TAG, "Friend added successfully: $friendId")
                Result.success(Unit)
            } else {
                val error = "Failed to add friend: ${response.code}"
                Log.e(TAG, error)
                Result.failure(Exception(error))
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error adding friend", e)
            Result.failure(e)
        }
    }

    override suspend fun getAllFriends(): Result<List<Friend>> {
        return try {
            val response = supabaseClient.getAllFriendsForCurrentUser()
            if (!response.isSuccessful) {
                val error = "Failed to load friends: ${response.code}"
                Log.e(TAG, error)
                return Result.failure(Exception(error))
            }

            val body = response.body?.string() ?: "[]"
            Log.d(TAG, "Get all friends response: $body")
            
            val jsonArray = JSONArray(body)
            val friendsList = mutableListOf<Friend>()
            
            for (i in 0 until jsonArray.length()) {
                val obj = jsonArray.getJSONObject(i)
                val points = obj.optInt("points", 0)
                val level = supabaseClient.calculateLevelFromXp(points.toLong())
                
                // Calculate XP progress for current level
                var requiredXp = 100.0
                var totalXp = 0.0
                for (j in 1 until level) {
                    totalXp += requiredXp
                    requiredXp *= 1.1
                }
                val xpForCurrentLevel = points - totalXp.toInt()
                val xpForNextLevel = requiredXp.toInt()
                
                val friend = Friend(
                    id = obj.optString("id"),
                    username = obj.optString("display_name"),
                    profilePicture = obj.optString("profile_picture", null),
                    bio = obj.optString("bio", null),
                    progress = points,
                    level = level,
                    currentLevelProgress = xpForCurrentLevel.coerceAtLeast(0),
                    currentLevelMax = xpForNextLevel
                )
                friendsList.add(friend)
                Log.d(TAG, "Parsed friend: ${friend.username} (ID: ${friend.id})")
            }
            
            Result.success(friendsList)
        } catch (e: Exception) {
            Log.e(TAG, "Error loading friends", e)
            Result.failure(e)
        }
    }

    override suspend fun removeFriend(friendId: String): Result<Unit> {
        return try {
            val response = supabaseClient.removeFriendWithCurrentUser(friendId)
            if (response.isSuccessful) {
                Log.d(TAG, "Friend removed successfully: $friendId")
                Result.success(Unit)
            } else {
                val error = "Failed to remove friend: ${response.code}"
                Log.e(TAG, error)
                Result.failure(Exception(error))
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error removing friend", e)
            Result.failure(e)
        }
    }

    override suspend fun getCurrentUserQRCode(): Result<String> {
        return try {
            val userId = supabaseClient.getCurrentUserId()
                ?: return Result.failure(Exception("User not logged in"))
            
            Result.success(userId)
        } catch (e: Exception) {
            Log.e(TAG, "Error getting current user QR code", e)
            Result.failure(e)
        }
    }

    override suspend fun getFriendDetails(friendId: String): Result<FriendDetails> {
        return try {
            Log.d(TAG, "Loading friend details for ID: $friendId")
            
            // Initialize with default values
            var username = "Friend User"
            var profilePicture: String? = null
            var bio: String? = null
            var totalPoints = 0
            var level = 1
            var currentLevelProgress = 0
            var currentLevelMax = 100
            var streakDays = 0
            var joinDate: String? = null
            
            // Get friend's profile data (points, streak, join date, etc.)
            supabaseClient.getFriendProfileForCurrentUser(friendId)
                .onSuccess { profileJson ->
                    Log.d(TAG, "=== FRIEND PROFILE DATA ===")
                    Log.d(TAG, "Full profile JSON: $profileJson")
                    Log.d(TAG, "Available keys: ${profileJson.keys().asSequence().toList()}")
                    
                    username = profileJson.optString("display_name", "Friend User")
                    profilePicture = profileJson.optString("profile_picture", null)
                    bio = profileJson.optString("bio", null)
                    totalPoints = profileJson.optInt("points", 0)
                    streakDays = profileJson.optInt("streak", 0)
                    
                    Log.d(TAG, "Parsed profile data:")
                    Log.d(TAG, "  - username: $username")
                    Log.d(TAG, "  - points: $totalPoints") 
                    Log.d(TAG, "  - streak: $streakDays")
                    
                    // Calculate level from points
                    level = supabaseClient.calculateLevelFromXp(totalPoints.toLong())
                    
                    // Calculate level progress
                    var requiredXp = 100.0
                    var totalXp = 0.0
                    for (i in 1 until level) {
                        totalXp += requiredXp
                        requiredXp *= 1.1
                    }
                    currentLevelProgress = (totalPoints - totalXp.toInt()).coerceAtLeast(0)
                    currentLevelMax = requiredXp.toInt()
                    
                    // Use only last_task_date for last active
                    val lastTaskDate = profileJson.optString("last_task_date", null)
                    Log.d(TAG, "Last task date: '$lastTaskDate'")
                    
                    if (lastTaskDate != null && lastTaskDate != "null" && lastTaskDate.isNotEmpty()) {
                        try {
                            joinDate = lastTaskDate
                            Log.d(TAG, "Using last_task_date as last active: $joinDate")
                        } catch (e: Exception) {
                            Log.w(TAG, "Could not parse last_task_date: $lastTaskDate", e)
                            joinDate = "Unknown"
                        }
                    } else {
                        Log.w(TAG, "No last_task_date found")
                        joinDate = "Unknown"
                    }
                }
                .onFailure { error ->
                    Log.w(TAG, "Could not fetch friend profile data: ${error.message}")
                }
            
            // Get friend's course progress
            val courseProgressList = mutableListOf<CourseProgress>()
            
            Log.d(TAG, "=== DEBUGGING FRIEND PROGRESS API ===")
            Log.d(TAG, "About to call getFriendProgressForCurrentUser with ID: $friendId")
            
            // Try RPC first, fall back to direct query
            val authToken = com.nhlstenden.appdev.core.utils.UserManager.getCurrentUser()?.authToken
            if (authToken != null) {
                supabaseClient.getFriendProgressViaRPC(friendId, authToken)
                    .onSuccess { progressArray ->
                    Log.d(TAG, "=== FRIEND PROGRESS DATA ===")
                    Log.d(TAG, "Progress array: $progressArray")
                    Log.d(TAG, "Progress array length: ${progressArray.length()}")
                    Log.d(TAG, "Friend ID being queried: $friendId")
                    
                    Log.d(TAG, "Processing friend progress data via RPC...")
                    
                    // Create progress map exactly like CourseRepositoryImpl does
                    val userProgressMap = HashMap<String, Int>(progressArray.length())
                    for (i in 0 until progressArray.length()) {
                        val jsonObject = progressArray.getJSONObject(i)
                        Log.d(TAG, "Progress entry $i: $jsonObject")
                        Log.d(TAG, "Available keys in progress entry: ${jsonObject.keys().asSequence().toList()}")
                        
                        val courseId = jsonObject.getString("course_id")
                        val progress = jsonObject.getInt("progress") // Number of completed tasks
                        userProgressMap[courseId] = progress
                        Log.d(TAG, "Friend progress: course_id=$courseId, completed_tasks=$progress")
                    }
                    
                    Log.d(TAG, "User progress map: $userProgressMap")
                    
                    // Load all available courses and match with progress
                    val allCourses = courseParser.loadAllCourses()
                    Log.d(TAG, "Available courses: ${allCourses.map { "${it.id} -> ${it.title}" }}")
                    
                    allCourses.forEach { course ->
                        // Get total tasks for this course
                        val allTasks = taskParser.loadAllCoursesOfTask(course.id)
                        val totalTasks = allTasks.size
                        
                        // Get completed tasks from progress map (default to 0 if no progress)
                        val completedTasks = userProgressMap[course.id] ?: 0
                        
                        Log.d(TAG, "Course ${course.id} (${course.title}): $completedTasks/$totalTasks tasks completed")
                        
                        if (totalTasks > 0) { // Only show courses that have tasks
                            val progressPercent = if (completedTasks > 0) {
                                (completedTasks.toFloat() / totalTasks.toFloat() * 100).toInt().coerceAtMost(100)
                            } else 0
                            
                            val courseProgress = CourseProgress(
                                courseId = course.id,
                                courseName = course.title,
                                progress = progressPercent,
                                tasksCompleted = completedTasks,
                                totalTasks = totalTasks
                            )
                            courseProgressList.add(courseProgress)
                            Log.d(TAG, "Added course progress: ${course.title} - $progressPercent% ($completedTasks/$totalTasks)")
                        } else {
                            Log.d(TAG, "Skipping course ${course.id} (${course.title}) - no tasks available")
                        }
                    }
                    
                    // Log final progress data
                    if (courseProgressList.isNotEmpty()) {
                        Log.d(TAG, "Successfully loaded ${courseProgressList.size} courses with progress data")
                    } else {
                        Log.w(TAG, "No course progress data found for friend")
                    }
                }
                    .onFailure { error ->
                        Log.w(TAG, "Could not fetch friend progress via RPC: ${error.message}")
                        Log.w(TAG, "Error details: ", error)
                        
                        // No mock data - if RPC fails, we'll show empty progress
                        Log.d(TAG, "RPC failed, friend progress will show as empty")
                    }
            } else {
                Log.e(TAG, "No auth token available for RPC call")
            }

            val friendDetails = FriendDetails(
                id = friendId,
                username = username,
                profilePicture = profilePicture,
                bio = bio,
                totalPoints = totalPoints,
                level = level,
                currentLevelProgress = currentLevelProgress,
                currentLevelMax = currentLevelMax,
                courseProgress = courseProgressList,
                achievements = listOf("First Steps", "Week Warrior", "Task Master", "Social Learner"),
                joinDate = joinDate,
                streakDays = streakDays
            )

            Log.d(TAG, "=== FINAL FRIEND DETAILS ===")
            Log.d(TAG, "Friend details complete:")
            Log.d(TAG, "  - ID: $friendId")
            Log.d(TAG, "  - Username: $username")
            Log.d(TAG, "  - Points: $totalPoints")
            Log.d(TAG, "  - Level: $level")
            Log.d(TAG, "  - Streak: $streakDays days")
            Log.d(TAG, "  - Join/Last Active Date: $joinDate")
            Log.d(TAG, "  - Courses: ${courseProgressList.size}")
            courseProgressList.forEach { cp ->
                Log.d(TAG, "    * ${cp.courseName}: ${cp.progress}% (${cp.tasksCompleted}/${cp.totalTasks})")
            }
            
            Result.success(friendDetails)
        } catch (e: Exception) {
            Log.e(TAG, "Error getting friend details", e)
            Result.failure(e)
        }
    }


} 