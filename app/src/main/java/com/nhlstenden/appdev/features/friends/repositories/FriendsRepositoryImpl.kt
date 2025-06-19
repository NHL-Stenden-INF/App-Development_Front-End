package com.nhlstenden.appdev.features.friends.repositories

import android.util.Log
import com.nhlstenden.appdev.core.repositories.FriendsRepository
import com.nhlstenden.appdev.features.friends.models.Friend
import com.nhlstenden.appdev.features.friends.models.FriendDetails
import com.nhlstenden.appdev.features.friends.models.CourseProgress
import com.nhlstenden.appdev.supabase.*
import com.nhlstenden.appdev.features.courses.CourseParser
import com.nhlstenden.appdev.features.courses.TaskParser
import org.json.JSONArray
import javax.inject.Inject
import javax.inject.Singleton
import com.nhlstenden.appdev.core.repositories.AuthRepository
import com.nhlstenden.appdev.utils.LevelCalculator
import okhttp3.Request
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

@Singleton
class FriendsRepositoryImpl @Inject constructor(
    private val supabaseClient: SupabaseClient,
    private val courseParser: CourseParser,
    private val taskParser: TaskParser,
    private val authRepository: AuthRepository
) : FriendsRepository {

    private val TAG = "FriendsRepositoryImpl"

    override suspend fun addFriend(friendId: String): Result<Unit> {
        return try {
            val currentUser = authRepository.getCurrentUserSync()
                ?: return Result.failure(Exception("User not logged in"))
            
            val result = supabaseClient.createMutualFriendship(friendId, currentUser.authToken)

            result.fold(
                onSuccess = {
                    Log.d(TAG, "Friend added successfully: $friendId")
                    Result.success(Unit)
                },
                onFailure = { error ->
                    Log.e(TAG, "Failed to add friend", error)
                    Result.failure(error)
                })
        } catch (e: Exception) {
            Log.e(TAG, "Error adding friend", e)
            Result.failure(e)
        }
    }

    override suspend fun getAllFriends(): Result<List<Friend>> {
        return try {
            val currentUser = authRepository.getCurrentUserSync()
                ?: return Result.failure(Exception("User not logged in"))
            
            val response = supabaseClient.getAllFriends(currentUser.authToken).getOrElse {
                Log.e(TAG, "Failed to load friends", it)
                return Result.failure(it)}

            val body = response.body?.string() ?: "[]"
            Log.d(TAG, "Get all friends response: $body")
            
            val friendsJson = org.json.JSONArray(body)
            val friends = mutableListOf<Friend>()
            
            for (i in 0 until friendsJson.length()) {
                val friendObj = friendsJson.getJSONObject(i)
                val friendId = friendObj.getString("id")
                
                // Fetch XP from user_attributes table to ensure consistency with friend details
                var actualXp = 0
                try {
                    val attributesRequest = okhttp3.Request.Builder()
                        .url("${supabaseClient.supabaseUrl}/rest/v1/user_attributes?select=xp&id=eq.$friendId")
                        .get()
                        .addHeader("apikey", supabaseClient.supabaseKey)
                        .addHeader("Authorization", "Bearer ${currentUser.authToken}")
                        .addHeader("Content-Type", "application/json")
                        .build()
                    
                    val attributesResponse = withContext(Dispatchers.IO) {
                        supabaseClient.client.newCall(attributesRequest).execute()
                    }
                    val attributesBody = attributesResponse.body?.string()
                    
                    if (attributesResponse.isSuccessful && !attributesBody.isNullOrEmpty()) {
                        val attributesArr = org.json.JSONArray(attributesBody)
                        if (attributesArr.length() > 0) {
                            val attributesData = attributesArr.getJSONObject(0)
                            actualXp = attributesData.optInt("xp", 0)
                        }
                    }
                } catch (e: Exception) {
                    Log.w(TAG, "Could not fetch XP for friend $friendId: ${e.message}")
                    // Fallback to RPC points field if attributes fetch fails
                    actualXp = friendObj.optInt("points", 0)
                }
                
                val (level, currentLevelProgress, currentLevelMax) = LevelCalculator.calculateLevelAndProgress(actualXp.toLong())
                
                Log.d(TAG, "=== FRIEND CARD DATA (getAllFriends) ===")
                Log.d(TAG, "Friend ${i}: id=$friendId")
                Log.d(TAG, "  - XP SOURCE: user_attributes 'xp' field = $actualXp")
                Log.d(TAG, "  - CALCULATED LEVEL: $level")
                Log.d(TAG, "=========================================")
                
                val friend = Friend(
                    id = friendId,
                    username = friendObj.optString("display_name", "Friend"),
                    profilePicture = friendObj.optString("profile_picture"),
                    bio = friendObj.optString("bio"),
                    progress = actualXp,
                    level = level,
                    currentLevelProgress = currentLevelProgress,
                    currentLevelMax = currentLevelMax,
                    lastActive = System.currentTimeMillis()
                )
                friends.add(friend)
            }
            
            Log.d(TAG, "Loaded ${friends.size} friends successfully")
            Result.success(friends)
        } catch (e: Exception) {
            Log.e(TAG, "Error loading friends", e)
            Result.failure(e)
        }
    }

    override suspend fun removeFriend(friendId: String): Result<Unit> {
        return try {
            val currentUser = authRepository.getCurrentUserSync()
                ?: return Result.failure(Exception("User not logged in"))
            
            val result = supabaseClient.removeFriend(friendId, currentUser.authToken)

            result.fold(
                onSuccess = {
                    Log.d(TAG, "Friend removed successfully: $friendId")
                    Result.success(Unit)
                },
                onFailure = { error ->
                    Log.e(TAG, "Failed to remove friend", error)
                    Result.failure(error)
                })
        } catch (e: Exception) {
            Log.e(TAG, "Error removing friend", e)
            Result.failure(e)
        }
    }

    override suspend fun getCurrentUserQRCode(): Result<String> {
        return try {
            val currentUser = authRepository.getCurrentUserSync()
                ?: return Result.failure(Exception("User not logged in"))
            
            // Return the user ID directly from the authenticated user
            Result.success(currentUser.id)
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
            val currentUser = authRepository.getCurrentUserSync()
                ?: return Result.failure(Exception("User not logged in"))
            
            // Get friend's profile data using base methods with auth token
            try {
                // Fetch from profile table which has display_name, bio, profile_picture, etc.
                val profileRequest = Request.Builder()
                    .url("${supabaseClient.supabaseUrl}/rest/v1/profile?select=*&id=eq.$friendId")
                    .get()
                    .addHeader("apikey", supabaseClient.supabaseKey)
                    .addHeader("Authorization", "Bearer ${currentUser.authToken}")
                    .addHeader("Content-Type", "application/json")
                    .build()
                
                val profileResponse = withContext(Dispatchers.IO) {
                    supabaseClient.client.newCall(profileRequest).execute()
                }
                val profileBody = profileResponse.body?.string()
                Log.d(TAG, "getFriendProfile: code=${profileResponse.code}, body=$profileBody")
                
                if (profileResponse.isSuccessful && !profileBody.isNullOrEmpty()) {
                    val profileArr = org.json.JSONArray(profileBody)
                    if (profileArr.length() > 0) {
                        val profileData = profileArr.getJSONObject(0)
                        
                        // Also fetch user_attributes for points, streak, etc.
                        val attributesRequest = Request.Builder()
                            .url("${supabaseClient.supabaseUrl}/rest/v1/user_attributes?select=*&id=eq.$friendId")
                            .get()
                            .addHeader("apikey", supabaseClient.supabaseKey)
                            .addHeader("Authorization", "Bearer ${currentUser.authToken}")
                            .addHeader("Content-Type", "application/json")
                            .build()
                        
                        val attributesResponse = withContext(Dispatchers.IO) {
                            supabaseClient.client.newCall(attributesRequest).execute()
                        }
                        val attributesBody = attributesResponse.body?.string()
                        Log.d(TAG, "getFriendAttributes: code=${attributesResponse.code}, body=$attributesBody")
                        
                        Log.d(TAG, "=== FRIEND PROFILE DATA ===")
                        Log.d(TAG, "Full profile JSON: $profileData")
                        Log.d(TAG, "Available keys: ${profileData.keys().asSequence().toList()}")
                        
                        username = profileData.optString("display_name", "Friend User")
                        profilePicture = profileData.optString("profile_picture", null)
                        bio = profileData.optString("bio", null)
                        
                        // Get attributes data if available
                        if (attributesResponse.isSuccessful && !attributesBody.isNullOrEmpty()) {
                            val attributesArr = org.json.JSONArray(attributesBody)
                            if (attributesArr.length() > 0) {
                                val attributesData = attributesArr.getJSONObject(0)
                                
                                Log.d(TAG, "=== ATTRIBUTES DATA EXTRACTION ===")
                                Log.d(TAG, "Full attributes JSON: $attributesData")
                                Log.d(TAG, "Available attribute keys: ${attributesData.keys().asSequence().toList()}")
                                
                                totalPoints = attributesData.optInt("xp", 0)  // Changed from "points" to "xp"
                                streakDays = attributesData.optInt("streak", 0)
                                
                                Log.d(TAG, "Extracted values:")
                                Log.d(TAG, "  - totalPoints (from 'xp'): $totalPoints")
                                Log.d(TAG, "  - streakDays (from 'streak'): $streakDays")
                                
                                // Use last_task_date for last active
                                val lastTaskDate = attributesData.optString("last_task_date", null)
                                Log.d(TAG, "  - lastTaskDate (from 'last_task_date'): '$lastTaskDate'")
                                
                                if (lastTaskDate != null && lastTaskDate != "null" && lastTaskDate.isNotEmpty()) {
                                    joinDate = lastTaskDate
                                    Log.d(TAG, "  - Using last_task_date as joinDate: $joinDate")
                                } else {
                                    joinDate = "Unknown"
                                    Log.d(TAG, "  - Setting joinDate to 'Unknown' due to empty/null last_task_date")
                                }
                            } else {
                                Log.w(TAG, "Attributes array is empty")
                            }
                        } else {
                            Log.w(TAG, "Attributes response failed or empty. Code: ${attributesResponse.code}, Body: '$attributesBody'")
                        }
                        
                        Log.d(TAG, "Parsed profile data:")
                        Log.d(TAG, "  - username: $username")
                        Log.d(TAG, "  - totalPoints: $totalPoints") 
                        Log.d(TAG, "  - streakDays: $streakDays")
                        
                        // Calculate level and progress from points
                        val (calculatedLevel, calculatedProgress, calculatedMax) = LevelCalculator.calculateLevelAndProgress(totalPoints.toLong())
                        level = calculatedLevel
                        currentLevelProgress = calculatedProgress  
                        currentLevelMax = calculatedMax
                        
                        Log.d(TAG, "=== FRIEND INFO CARD DATA (getFriendDetails) ===")
                        Log.d(TAG, "Friend ID: $friendId")
                        Log.d(TAG, "  - XP SOURCE: user_attributes 'xp' field = $totalPoints")
                        Log.d(TAG, "  - CALCULATED LEVEL: $level") 
                        Log.d(TAG, "  - level progress: $currentLevelProgress / $currentLevelMax XP")
                        Log.d(TAG, "=================================================")
                    }
                }
            } catch (e: Exception) {
                Log.w(TAG, "Could not fetch friend profile data: ${e.message}", e)
            }
            
            // Get friend's course progress
            val courseProgressList = mutableListOf<CourseProgress>()
            
            Log.d(TAG, "=== DEBUGGING FRIEND PROGRESS API ===")
            Log.d(TAG, "About to call getFriendProgressForCurrentUser with ID: $friendId")
            
            // Try RPC first, fall back to direct query
            if (currentUser != null) {
                supabaseClient.getFriendProgressViaRPC(friendId, currentUser.authToken)
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
                Log.e(TAG, "No current user available for RPC call")
            }

            Log.d(TAG, "=== CREATING FRIEND DETAILS OBJECT ===")
            Log.d(TAG, "About to create FriendDetails with:")
            Log.d(TAG, "  - id: $friendId")
            Log.d(TAG, "  - username: $username")
            Log.d(TAG, "  - totalPoints: $totalPoints")
            Log.d(TAG, "  - level: $level")
            Log.d(TAG, "  - currentLevelProgress: $currentLevelProgress")
            Log.d(TAG, "  - currentLevelMax: $currentLevelMax")
            Log.d(TAG, "  - streakDays: $streakDays")
            Log.d(TAG, "  - joinDate: $joinDate")
            Log.d(TAG, "  - courseProgressList.size: ${courseProgressList.size}")

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