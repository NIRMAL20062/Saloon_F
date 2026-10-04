package com.glide.shared

import com.glide.shared.admin.AdminResponse
import com.glide.shared.admin.AdminStatus
import com.glide.shared.admin.InviteAdminRequest
import com.glide.shared.api.ApiJson
import com.glide.shared.error.ErrorBody
import com.glide.shared.error.ErrorResponse
import com.glide.shared.health.HealthResponse
import com.glide.shared.health.HealthStatus
import com.glide.shared.me.MeResponse
import com.glide.shared.me.UpdateProfileRequest
import com.glide.shared.me.UpdateSideRequest
import com.glide.shared.me.UserSide
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

    @Test
    fun `me response has the documented wire format`() {
        val json =
            ApiJson.encodeToString(
                MeResponse("3f0c9a52-7d1e-4b8a-9a0e-2c1d5b6e7f80", "919000000001", UserSide.SALON),
            )

        assertJsonEquals(
            """{"id":"3f0c9a52-7d1e-4b8a-9a0e-2c1d5b6e7f80","phone":"919000000001","side":"SALON"}""",
            json,
        )
    }

    @Test
    fun `me with a profile adds name and email`() {
        val json =
            ApiJson.encodeToString(
                MeResponse("u-1", "919000000001", UserSide.CUSTOMER, "Priya Sharma", "priya@example.com"),
            )

        assertJsonEquals(
            """{"id":"u-1","phone":"919000000001","side":"CUSTOMER","name":"Priya Sharma","email":"priya@example.com"}""",
            json,
        )
    }

    @Test
    fun `profile update request has the documented wire format`() {
        assertJsonEquals("""{"name":"Priya"}""", ApiJson.encodeToString(UpdateProfileRequest("Priya")))
        assertJsonEquals(
            """{"name":"Priya","email":"p@x.in"}""",
            ApiJson.encodeToString(UpdateProfileRequest("Priya", "p@x.in")),
        )
    }

    @Test
    fun `a new user's side is omitted until onboarding`() {
        val json = ApiJson.encodeToString(MeResponse("u-1", "919000000001"))

        assertJsonEquals("""{"id":"u-1","phone":"919000000001"}""", json)
    }

    @Test
    fun `update side request has the documented wire format`() {
        assertJsonEquals("""{"side":"CUSTOMER"}""", ApiJson.encodeToString(UpdateSideRequest(UserSide.CUSTOMER)))
    }

    @Test
    fun `admin response has the documented wire format`() {
        val json =
            ApiJson.encodeToString(
                AdminResponse("3f0c9a52-7d1e-4b8a-9a0e-2c1d5b6e7f80", "admin@glide.test", AdminStatus.INVITED),
            )

        assertJsonEquals(
            """{"id":"3f0c9a52-7d1e-4b8a-9a0e-2c1d5b6e7f80","email":"admin@glide.test","status":"INVITED"}""",
            json,
        )
    }

    @Test
    fun `invite admin request has the documented wire format`() {
        assertJsonEquals("""{"email":"b@glide.test"}""", ApiJson.encodeToString(InviteAdminRequest("b@glide.test")))
    }

    private fun assertJsonEquals(
        expected: String,
        actual: String,
    ) {
        assertEquals(Json.decodeFromString<JsonObject>(expected), Json.decodeFromString<JsonObject>(actual))
    }
}
