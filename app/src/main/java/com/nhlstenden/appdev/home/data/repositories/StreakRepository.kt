package com.nhlstenden.appdev.home.data.repositories

import android.os.Build
import androidx.annotation.RequiresApi
import com.nhlstenden.appdev.supabase.SupabaseClient
import java.time.LocalDate
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class StreakRepository @Inject constructor(
    private val supabaseClient: SupabaseClient
) {
    @RequiresApi(Build.VERSION_CODES.O)
    suspend fun getLastTaskDate(userId: String, authToken: String): LocalDate? {
        val response = supabaseClient.getUserAttributes(userId, authToken)
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

    suspend fun  updateLastTaskDate(userId: String, date: LocalDate, authToken: String): Boolean {
        val response = supabaseClient.updateUserLastTaskDate(userId, date.toString(), authToken)
        return response.code == 200 || response.code == 204
    }
}