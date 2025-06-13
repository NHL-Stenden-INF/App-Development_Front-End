package com.nhlstenden.appdev.features.home

import android.os.Build
import androidx.annotation.RequiresApi
import java.time.LocalDate
import java.time.temporal.ChronoUnit

class StreakManager {
    private var currentStreak: Int = 0
    private var lastCompletedDate: LocalDate? = null

    @RequiresApi(Build.VERSION_CODES.O)
    fun updateStreak(taskCompletionDate: LocalDate, currentStreakFromDb: Int = 0) {
        val today = LocalDate.now()
        
        // If the task completion date is in the future, don't update
        if (taskCompletionDate.isAfter(today)) {
            return
        }

        // Initialize with the streak from the database
        currentStreak = currentStreakFromDb

        if (lastCompletedDate == null) {
            // First task ever or first after being initialized
            currentStreak = 1
        } else {
            val daysBetween = ChronoUnit.DAYS.between(lastCompletedDate, taskCompletionDate)
            when {
                daysBetween == 0L -> {
                    // Same day, do nothing - streak stays the same
                }
                daysBetween == 1L -> {
                    // Next day, increment streak
                    currentStreak++
                }
                else -> {
                    // More than one day has passed, reset streak to 1
                    currentStreak = 1
                }
            }
        }

        // Always update the last completed date
        lastCompletedDate = taskCompletionDate
    }

    @RequiresApi(Build.VERSION_CODES.O)
    fun checkAndResetStreak(lastTaskDate: LocalDate?): Int {
        val today = LocalDate.now()
        
        if (lastTaskDate == null) {
            return 0
        }
        
        val daysSinceLastTask = ChronoUnit.DAYS.between(lastTaskDate, today)
        
        // If more than 1 day has passed since last task, reset streak to 0
        return if (daysSinceLastTask > 1) {
            0
        } else {
            currentStreak
        }
    }

    fun getCurrentStreak(): Int = currentStreak

    fun getLastCompletedDate(): LocalDate? = lastCompletedDate

    fun resetStreak() {
        currentStreak = 0
        lastCompletedDate = null
    }

    // New function to initialize the streak manager with database values
    fun initializeFromDatabase(lastTaskDate: LocalDate?, currentStreak: Int) {
        this.lastCompletedDate = lastTaskDate
        this.currentStreak = currentStreak
    }
}