package com.nhlstenden.appdev.features.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject
import com.nhlstenden.appdev.features.home.repositories.StreakRepository
import com.nhlstenden.appdev.features.home.StreakManager
import com.nhlstenden.appdev.core.repositories.AuthRepository
import java.time.LocalDate
import com.nhlstenden.appdev.features.courses.repositories.CoursesRepository
import com.nhlstenden.appdev.features.home.HomeCourse
import kotlinx.coroutines.flow.update
import com.nhlstenden.appdev.core.repositories.FriendsRepository
import com.nhlstenden.appdev.core.repositories.UserRepository
import com.nhlstenden.appdev.utils.LevelCalculator
import java.time.temporal.ChronoUnit
import com.nhlstenden.appdev.features.home.StreakDay
import com.nhlstenden.appdev.features.home.MotivationalMessage

@HiltViewModel
class HomeViewModel @Inject constructor(
    private val streakRepository: StreakRepository,
    private val authRepository: AuthRepository,
    private val streakManager: StreakManager,
    private val coursesRepository: CoursesRepository,
    private val friendsRepository: FriendsRepository,
    private val userRepository: UserRepository
) : ViewModel() {
    private val _streakState = MutableStateFlow<StreakState>(StreakState.Loading)
    val streakState: StateFlow<StreakState> = _streakState

    private val _homeCourses = MutableStateFlow<List<HomeCourse>>(emptyList())
    val homeCourses: StateFlow<List<HomeCourse>> = _homeCourses

    private val _motivationalMessage = MutableStateFlow(MotivationalMessage(""))
    val motivationalMessage: StateFlow<MotivationalMessage> = _motivationalMessage

    private val _dailyChallengeState = MutableStateFlow<DailyChallengeState>(DailyChallengeState.Loading)
    val dailyChallengeState: StateFlow<DailyChallengeState> = _dailyChallengeState

    private val _streakDays = MutableStateFlow<List<StreakDay>>(emptyList())
    val streakDays: StateFlow<List<StreakDay>> = _streakDays

    fun loadStreak() {
        viewModelScope.launch {
            val user = authRepository.getCurrentUserSync()
            if (user != null) {
                val lastTaskDate = streakRepository.getLastTaskDate(user.id, user.authToken)
                val currentStreak = streakRepository.getCurrentStreak(user.id, user.authToken)
                streakManager.initializeFromDatabase(lastTaskDate, currentStreak)
                _streakState.value = StreakState.Success(currentStreak, lastTaskDate)
                _streakDays.value = calculateStreakDays(currentStreak, lastTaskDate)
            } else {
                _streakState.value = StreakState.Error("No user found")
                _streakDays.value = emptyList()
            }
        }
    }

    private fun calculateStreakDays(currentStreak: Int, lastTaskDate: LocalDate?): List<StreakDay> {
        val days = listOf("Mon", "Tue", "Wed", "Thu", "Fri", "Sat", "Sun")
        val today = LocalDate.now()
        val startOfWeek = today.minusDays(today.dayOfWeek.value.toLong() - 1)
        return (0..6).map { i ->
            val currentDate = startOfWeek.plusDays(i.toLong())
            val isCompleted = if (lastTaskDate != null) {
                val daysFromLastTask = java.time.temporal.ChronoUnit.DAYS.between(currentDate, lastTaskDate)
                daysFromLastTask >= 0 && daysFromLastTask < currentStreak
            } else {
                false
            }
            StreakDay(days[i], isCompleted)
        }
    }

    fun loadHomeCourses() {
        viewModelScope.launch {
            val user = authRepository.getCurrentUserSync()
            if (user != null) {
                val courses = coursesRepository.getCourses(user)
                if (courses != null) {
                    val activeCourses = courses.filter { it.progress > 0 && it.progress < it.totalTasks }
                    val homeCourses = activeCourses.map { course ->
                        HomeCourse(
                            id = course.id,
                            title = course.title,
                            progressText = "${course.progress}/${course.totalTasks} tasks",
                            progressPercent = if (course.totalTasks > 0) (course.progress.toFloat() / course.totalTasks * 100).toInt() else 0,
                            iconResId = course.imageResId,
                            accentColor = -1 // Set in fragment for now
                        )
                    }.sortedByDescending { it.progressPercent }.take(3)
                    _homeCourses.value = homeCourses
                } else {
                    _homeCourses.value = emptyList()
                }
            } else {
                _homeCourses.value = emptyList()
            }
        }
    }

    fun loadMotivationalMessage() {
        viewModelScope.launch {
            val currentUser = authRepository.getCurrentUserSync() ?: return@launch
            val userStreak = try { streakRepository.getCurrentStreak(currentUser.id, currentUser.authToken) } catch (e: Exception) { 0 }
            val userXp = userRepository.getUserAttributes(currentUser.id).getOrNull()?.optInt("xp", 0) ?: 0
            val userLevel = LevelCalculator.calculateLevelFromXp(userXp.toLong())
            val userCourseProgress: Map<String, Int> = try {
                val list = coursesRepository.getCourses(currentUser) ?: emptyList()
                list.associate { c -> c.id to if (c.totalTasks > 0) (c.progress.toFloat() / c.totalTasks * 100).toInt() else 0 }
            } catch (e: Exception) { emptyMap() }
            val friendsResult = friendsRepository.getAllFriends()
            if (friendsResult.isFailure) return@launch
            val friends = friendsResult.getOrNull()?.filter { it.username.isNotBlank() } ?: emptyList()
            val candidateMessages = mutableListOf<MotivationalMessage>()
            for (friend in friends) {
                val friendName = friend.username
                val friendDetails = friendsRepository.getFriendDetails(friend.id).getOrNull()
                val friendPic = friend.profilePicture ?: friendDetails?.profilePicture
                if (friend.level > userLevel) {
                    candidateMessages.add(MotivationalMessage("$friendName hit level ${friend.level}, can you level up and pass them?", friendPic, friend.profileMask))
                }
                val friendStreak = try { streakRepository.getCurrentStreak(friend.id, currentUser.authToken) } catch (e: Exception) { 0 }
                if (friendStreak > userStreak) {
                    candidateMessages.add(MotivationalMessage("$friendName is on a ${friendStreak}-day streak, think you can keep up?", friendPic, friend.profileMask))
                }
                val coursesOfInterest = listOf("sql", "css", "html")
                if (coursesOfInterest.isNotEmpty()) {
                    val friendCourseMap = friendDetails?.courseProgress?.associate { cp -> cp.courseId to cp.progress } ?: emptyMap()
                    for (courseId in coursesOfInterest) {
                        val friendProgress = friendCourseMap[courseId] ?: continue
                        val userProgress = userCourseProgress[courseId] ?: 0
                        if (friendProgress > userProgress) {
                            val courseName = courseId.uppercase()
                            candidateMessages.add(MotivationalMessage("$friendName is ahead of you in $courseName, time to close the gap!", friendPic, friend.profileMask))
                        }
                    }
                }
            }
            val selection: MotivationalMessage? = if (candidateMessages.isNotEmpty()) {
                candidateMessages.random()
            } else {
                val message = if (friends.isEmpty()) {
                    "Add some friends to start friendly competitions and boost your learning!"
                } else {
                    listOf(
                        "You're leading the pack! Can you keep your top spot?",
                        "You're the highest level among your friends—keep it up!",
                        "Your streak beats all your friends right now—don't slow down!"
                    ).random()
                }
                MotivationalMessage(message)
            }
            _motivationalMessage.value = selection ?: MotivationalMessage("")
        }
    }

    fun loadDailyChallengeState() {
        viewModelScope.launch {
            val currentUser = authRepository.getCurrentUserSync()
            if (currentUser != null) {
                val startDate = userRepository.getUserAttributes(currentUser.id).getOrNull()?.getString("finished_daily_challenge_at")
                val lastCompletedDate = if (startDate == "null" || startDate == null) java.time.LocalDate.now().minusDays(1) else java.time.LocalDate.parse(startDate)
                val isTodayTheDay = ChronoUnit.DAYS.between(lastCompletedDate, java.time.LocalDate.now()) != 0L
                _dailyChallengeState.value = DailyChallengeState.Success(isTodayTheDay)
            } else {
                _dailyChallengeState.value = DailyChallengeState.Error
            }
        }
    }

    sealed class StreakState {
        object Loading : StreakState()
        data class Success(val streak: Int, val lastTaskDate: LocalDate?) : StreakState()
        data class Error(val message: String) : StreakState()
    }

    sealed class DailyChallengeState {
        object Loading : DailyChallengeState()
        data class Success(val isAvailable: Boolean) : DailyChallengeState()
        object Error : DailyChallengeState()
    }
} 