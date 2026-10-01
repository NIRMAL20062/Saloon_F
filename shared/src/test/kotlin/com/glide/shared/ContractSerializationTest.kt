package com.glide.shared

import com.glide.shared.api.ApiJson
import com.glide.shared.error.ErrorBody
import com.glide.shared.error.ErrorResponse
import com.glide.shared.health.HealthResponse
import com.glide.shared.health.HealthStatus
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlin.test.Test
import kotlin.test.assertEquals

/**
 * Pins the exact JSON wire format. If one of these tests fails, you changed the API contract:
 * update docs/api/openapi.yaml and check that older app versions still work.
 */
class ContractSerializationTest {
    @Test
    fun `health response has the documented wire format`() {
        val json = ApiJson.encodeToString(HealthResponse(HealthStatus.UP, "0.1.0", HealthStatus.UP))

        assertJsonEquals("""{"status":"UP","version":"0.1.0","database":"UP"}""", json)
    }

    @Test
    fun `liveness response omits database instead of sending null`() {
        val json = ApiJson.encodeToString(HealthResponse(HealthStatus.UP, "0.1.0"))

        assertJsonEquals("""{"status":"UP","version":"0.1.0"}""", json)
    }

    @Test
    fun `old clients ignore fields added later by the backend`() {
        val fromNewerBackend = """{"status":"DOWN","version":"9.9.9","database":"DOWN","uptimeSeconds":42}"""

        val decoded = ApiJson.decodeFromString<HealthResponse>(fromNewerBackend)

        assertEquals(HealthResponse(HealthStatus.DOWN, "9.9.9", HealthStatus.DOWN), decoded)
    }

    @Test
    fun `error response has the documented wire format`() {
        val json = ApiJson.encodeToString(ErrorResponse(ErrorBody("NOT_FOUND", "No such route", "req-1")))

        assertJsonEquals("""{"error":{"code":"NOT_FOUND","message":"No such route","requestId":"req-1"}}""", json)
    }

    private fun assertJsonEquals(
        expected: String,
        actual: String,
    ) {
        assertEquals(Json.decodeFromString<JsonObject>(expected), Json.decodeFromString<JsonObject>(actual))
    }
}
