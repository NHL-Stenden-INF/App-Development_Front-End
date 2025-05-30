package com.nhlstenden.appdev.home.manager

import android.os.Build
import androidx.annotation.RequiresApi
import java.time.LocalDate
import java.time.temporal.ChronoUnit

class StreakManager {
    private var currentStreak: Int = 0
    private var lastCompletedDate: LocalDate? = null

    @RequiresApi(Build.VERSION_CODES.O)
    fun updateStreak(taskCompletionDate: LocalDate) {
        if (lastCompletedDate == null) {
            currentStreak = 1
        } else {
            val daysBetween = ChronoUnit.DAYS.between(lastCompletedDate, taskCompletionDate)
            when {
                daysBetween == 0L -> {
                    // Is the same day, so nothing
                }

                // The next day, so add 1
                daysBetween == 1L -> {
                    currentStreak += 1
                }

                // Two or more days ago, so reset to 1
                else -> {
                    currentStreak = 1
                }
            }
        }

        lastCompletedDate = taskCompletionDate
    }

    fun getCurrentStreak(): Int = currentStreak

    fun getLastCompletedDate(): LocalDate? = lastCompletedDate

    fun resetStreak() {
        currentStreak = 0
        lastCompletedDate = null
    }
}