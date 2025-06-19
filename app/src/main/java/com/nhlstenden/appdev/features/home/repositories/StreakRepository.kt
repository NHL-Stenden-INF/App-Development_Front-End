package com.nhlstenden.appdev.features.home.repositories

import android.os.Build
import androidx.annotation.RequiresApi
import com.nhlstenden.appdev.supabase.*
import java.time.LocalDate
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class StreakRepository @Inject constructor(
    private val supabaseClient: SupabaseClient
) {
    @RequiresApi(Build.VERSION_CODES.O)
    suspend fun getLastTaskDate(userId: String, authToken: String): LocalDate? {
        val responseResult = supabaseClient.getUserAttributes(userId, authToken)

        if (responseResult.isFailure) return null

        val response = responseResult.getOrThrow()

        if (response.code == 200) {
            val responseBody = response.body?.string()
            val userData = org.json.JSONArray(responseBody).getJSONObject(0)
            val lastTaskDate = userData.optString("last_task_date")

            return if (lastTaskDate.isNotEmpty() && lastTaskDate != "null") {
                try {
                    LocalDate.parse(lastTaskDate)
                } catch (e: Exception) {
                    null
                }
            } else null
        }

        return null
    }

    suspend fun getCurrentStreak(userId: String, authToken: String): Int {
        val responseResult = supabaseClient.getUserAttributes(userId, authToken)

        if (responseResult.isFailure) return 0

        val response = responseResult.getOrThrow()

        if (response.code == 200) {
            val responseBody = response.body?.string()
            val userData = org.json.JSONArray(responseBody).getJSONObject(0)
            return userData.optInt("streak", 0)
        }

        return 0
    }

    suspend fun updateLastTaskDate(userId: String, date: LocalDate, authToken: String): Boolean {
        val responseResult = supabaseClient.updateUserLastTaskDate(userId, date.toString(), authToken)
        if (responseResult.isFailure) return false
        val response = responseResult.getOrThrow()
        return response.code == 200 || response.code == 204
    }

    suspend fun updateStreak(userId: String, streak: Int, authToken: String): Boolean {
        val responseResult = supabaseClient.updateUserStreak(userId, streak, authToken)
        if (responseResult.isFailure) return false
        val response = responseResult.getOrThrow()
        return response.code == 200 || response.code == 204
    }
}