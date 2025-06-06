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
            // If we have a streak but no last completed date, this is the first update
            // after initialization, so we should increment the streak
            if (currentStreak > 0) {
                currentStreak++
            } else {
                currentStreak = 1
            }
        } else {
            val daysBetween = ChronoUnit.DAYS.between(lastCompletedDate, taskCompletionDate)
            when {
                daysBetween == 0L -> {
                    // Same day, do nothing
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