package com.nhlstenden.appdev.supabase

import android.util.Log
import okhttp3.OkHttpClient
import org.json.JSONObject

class SupabaseClient {
    val client = OkHttpClient()

    val supabaseUrl = "https://ggpdstbylyiwkfcucoxd.supabase.co"
    val supabaseKey = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJpc3MiOiJzdXBhYmFzZSIsInJlZiI6ImdncGRzdGJ5bHlpd2tmY3Vjb3hkIiwicm9sZSI6ImFub24iLCJpYXQiOjE3NDczMDg4MzYsImV4cCI6MjA2Mjg4NDgzNn0.2ZGOttYWxBJkNcPmAtJh6dzlm3G6vwpIonEtRvtNNa8"

    internal fun getUserIdFromToken(token: String): Result<String> {
        val parts = token.split(".")
        if (parts.size != 3) {
            return Result.failure(Exception("Invalid token format"))
        }

        return try {
            val payload = parts[1]
            val decoded = android.util.Base64.decode(payload, android.util.Base64.URL_SAFE)
            val payloadJson = String(decoded)
            val jsonObject = JSONObject(payloadJson)
            val userId = jsonObject.optString("sub", "")

            if (userId.isEmpty()) {
                Result.failure(Exception("User ID (sub) not found in token"))
            } else {
                Result.success(userId)
            }
        } catch (e: Exception) {
            Log.e("SupabaseClient", "Error decoding token: ", e)
            Result.failure(e)
        }
    }
}
