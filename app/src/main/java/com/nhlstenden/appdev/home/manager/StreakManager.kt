package com.nhlstenden.appdev.home.manager

import android.os.Build
import androidx.annotation.RequiresApi
import java.time.LocalDate

class StreakManager {
    @RequiresApi(Build.VERSION_CODES.O)
    private var currentDate: LocalDate = LocalDate.now()
    @RequiresApi(Build.VERSION_CODES.O)
    private var prevStreakDate: LocalDate = currentDate.minusDays(2)
    private var currentStreak: Int = 0

    @RequiresApi(Build.VERSION_CODES.O)
    fun streakChecker() {
        when {
            currentDate == prevStreakDate -> {
            }

            // First streak day
            currentStreak == 0 -> {
                currentStreak = 1
                prevStreakDate = currentDate
            }

            // Continued streak
            currentDate.minusDays(1) == prevStreakDate -> {
                currentStreak += 1
                prevStreakDate = currentDate
            }

            // Missed a day or more, reset streak
            else -> {
                currentStreak =1
                prevStreakDate = currentDate
            }
        }
    }
}