package com.nhlstenden.appdev

import com.nhlstenden.appdev.supabase.SupabaseClient
import org.json.JSONException
import org.json.JSONObject
import org.junit.Assert.*
import org.junit.Test
import org.junit.jupiter.api.assertDoesNotThrow
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid

class SupabaseClientTest {
    private val supabaseClient = SupabaseClient()
    @OptIn(ExperimentalUuidApi::class)
    private val email = """${Uuid.random().toString()}@doe.com"""

    @Test
    fun createNewUser_ValidEmail_ShouldNotThrow() {
        assertDoesNotThrow {supabaseClient.createNewUser(
            this.email,
            "12345678"
        )}
        var userCredentials = ""
        assertDoesNotThrow {
            userCredentials = JSONObject(
                this.supabaseClient.login(
                    this.email,
                    "12345678"
                ).body?.string()
            ).getString("access_token")
        }
        assertEquals(200, this.supabaseClient.getUserAttributes(userCredentials).code)
    }

    @Test
    fun createNewUser_InvalidEmail_ShouldThrow() {
        assertThrows(IllegalArgumentException::class.java) {
            this.supabaseClient.createNewUser(
                "**!@#\$%^&*()_+=-`~[]\\{}|;':\",./<>?**@doe.com\n",
                "123456"
            )
        }
        var userCredentials = ""
        assertThrows(JSONException::class.java) {
            userCredentials = JSONObject(
                this.supabaseClient.login(
                    "**!@#\$%^&*()_+=-`~[]\\{}|;':\",./<>?**@doe.com\n",
                    "12345678"
                ).body?.string()
            ).getString("access_token")
        }
        val userAttributesResponse = this.supabaseClient.getUserAttributes(userCredentials)
        assertEquals(200, userAttributesResponse.code)
        assertEquals("[]", userAttributesResponse.body?.string())
    }
}