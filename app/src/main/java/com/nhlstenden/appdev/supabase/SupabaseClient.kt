package com.nhlstenden.appdev.supabase

import android.util.Log
import okhttp3.OkHttpClient
import org.json.JSONObject

class SupabaseClient {
    val client = OkHttpClient()

    val supabaseUrl = "https://ggpdstbylyiwkfcucoxd.supabase.co"
    val supabaseKey = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJpc3MiOiJzdXBhYmFzZSIsInJlZiI6ImdncGRzdGJ5bHlpd2tmY3Vjb3hkIiwicm9sZSI6ImFub24iLCJpYXQiOjE3NDczMDg4MzYsImV4cCI6MjA2Mjg4NDgzNn0.2ZGOttYWxBJkNcPmAtJh6dzlm3G6vwpIonEtRvtNNa8"

    internal fun getUserIdFromToken(token: String): String {
        val parts = token.split(".")
        if (parts.size != 3) return ""

        return try {
            val payload = parts[1]
            val decoded = android.util.Base64.decode(payload, android.util.Base64.URL_SAFE)
            val payloadJson = String(decoded)
            val jsonObject = JSONObject(payloadJson)
            jsonObject.optString("sub", "")
        } catch (e: Exception) {
            Log.e("SupabaseClient", "Error decoding token: ", e)
            ""
        }
    }
}

@kotlinx.parcelize.Parcelize
data class User(
    val authToken: String,
    val id: java.util.UUID,
    val username: String,
    val email: String,
    val points: Int,
    val friends: java.util.ArrayList<java.util.UUID>,
    val achievements: java.util.ArrayList<String>,
    val profilePicture: String
) : android.os.Parcelable 