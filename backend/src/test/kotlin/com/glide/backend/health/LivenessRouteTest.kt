package com.glide.backend.health

import com.glide.backend.module
import com.glide.backend.testConfig
import com.glide.shared.api.ApiJson
import com.glide.shared.api.ApiRoutes
import com.glide.shared.health.HealthResponse
import com.glide.shared.health.HealthStatus
import io.ktor.client.request.get
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.HttpStatusCode
import io.ktor.http.contentType
import io.ktor.server.testing.testApplication
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class LivenessRouteTest {
    @Test
    fun `liveness reports UP with version and no database field`() =
        testApplication {
            application { module(testConfig()) }

            val response = client.get(ApiRoutes.HEALTH_LIVE)

            assertEquals(HttpStatusCode.OK, response.status)
            assertTrue(response.contentType()!!.match(ContentType.Application.Json))
            assertEquals(
                HealthResponse(status = HealthStatus.UP, version = "test"),
                ApiJson.decodeFromString<HealthResponse>(response.bodyAsText()),
            )
        }
}
