package com.nhlstenden.appdev

import com.nhlstenden.appdev.supabase.*
import org.json.JSONException
import org.json.JSONObject
import org.junit.Assert.*
import org.junit.Test
import java.util.UUID
import android.util.Base64
import kotlinx.coroutines.runBlocking

class SupabaseClientTest {
    private val supabaseClient = SupabaseClient()
    private val email = "${UUID.randomUUID()}@doe.com"
    private val password = "12345678"
    private val displayName = "TestUser"

    // Helper to extract userId from JWT
    private fun getUserIdFromToken(token: String): String {
        val parts = token.split(".")
        if (parts.size != 3) return ""
        return try {
            val payload = parts[1]
            val decoded = Base64.decode(payload, Base64.URL_SAFE)
            val payloadJson = String(decoded)
            val jsonObject = JSONObject(payloadJson)
            jsonObject.optString("sub", "")
        } catch (e: Exception) {
            ""
        }
    }

    @Test
    fun registerAndLogin_ValidEmail_ShouldNotThrow() = runBlocking {
        // Register user
        val accessToken = try {
            supabaseClient.register(email, password, displayName)
        } catch (e: Exception) {
            fail("Exception during register: ${e.message}"); return@runBlocking
        }
        assertTrue(accessToken.isNotEmpty())
        val userId = getUserIdFromToken(accessToken)
        assertTrue(userId.isNotEmpty())
        // Login user
        val loginToken = try {
            supabaseClient.login(email, password)
        } catch (e: Exception) {
            fail("Exception during login: ${e.message}"); return@runBlocking
        }
        assertTrue(loginToken.isNotEmpty())
        // Get user attributes
        val response = supabaseClient.getUserAttributes(userId, loginToken)
        assertEquals(200, response.code)
    }

    @Test
    fun register_InvalidEmail_ShouldThrow() = runBlocking {
        val badEmail = "**!@#\$%^&*()_+=-`~[]\\{}|;':\",./<>?**@doe.com\n"
        try {
            supabaseClient.register(badEmail, password, displayName)
            fail("Expected exception for invalid email")
        } catch (e: Exception) {
            // Expected
        }
    }
} 