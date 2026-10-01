package com.glide.backend.plugins

import com.glide.backend.config.AppConfig
import com.glide.backend.config.AppEnv
import com.glide.backend.fakeDependencies
import com.glide.backend.module
import com.glide.backend.testConfig
import com.glide.shared.api.ApiJson
import com.glide.shared.api.ApiRoutes
import com.glide.shared.error.ErrorCodes
import com.glide.shared.error.ErrorResponse
import com.glide.shared.health.HealthResponse
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.request.options
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.statement.HttpResponse
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.contentType
import io.ktor.server.request.receive
import io.ktor.server.response.respond
import io.ktor.server.routing.get
import io.ktor.server.routing.post
import io.ktor.server.routing.routing
import io.ktor.server.testing.ApplicationTestBuilder
import io.ktor.server.testing.testApplication
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

/** Each test proves one control from docs/SECURITY.md § Baseline controls. */
class SecurityBaselineTest {
    @Test
    fun `every response carries the security headers`() =
        withApp {
            val response = client.get(ApiRoutes.HEALTH_LIVE)

            assertEquals("nosniff", response.headers["X-Content-Type-Options"])
            assertEquals("DENY", response.headers["X-Frame-Options"])
            assertEquals("no-referrer", response.headers["Referrer-Policy"])
            assertEquals("default-src 'none'; frame-ancestors 'none'", response.headers["Content-Security-Policy"])
            assertEquals("no-store", response.headers[HttpHeaders.CacheControl])
            assertFalse(response.headers[HttpHeaders.Server].orEmpty().contains("Ktor"), "server version leaked")
        }

    @Test
    fun `HSTS is sent only on staging and production`() {
        withApp(testConfig(env = AppEnv.LOCAL)) {
            assertNull(client.get(ApiRoutes.HEALTH_LIVE).headers[HttpHeaders.StrictTransportSecurity])
        }
        withApp(testConfig(env = AppEnv.PRODUCTION, corsAllowedOrigins = emptyList())) {
            assertNotNull(client.get(ApiRoutes.HEALTH_LIVE).headers[HttpHeaders.StrictTransportSecurity])
        }
    }

    @Test
    fun `a request id is generated when the caller sends none`() =
        withApp {
            val id = client.get(ApiRoutes.HEALTH_LIVE).headers[HttpHeaders.XRequestId]

            assertNotNull(id)
            assertTrue(id.matches(Regex("[0-9a-f-]{36}")), "expected a UUID, got $id")
        }

    @Test
    fun `a safe caller request id is echoed back`() =
        withApp {
            val response = client.get(ApiRoutes.HEALTH_LIVE) { header(HttpHeaders.XRequestId, "android-abc_123") }

            assertEquals("android-abc_123", response.headers[HttpHeaders.XRequestId])
        }

    @Test
    fun `an unsafe caller request id is replaced, not rejected`() =
        withApp {
            val response = client.get(ApiRoutes.HEALTH_LIVE) { header(HttpHeaders.XRequestId, "x".repeat(65)) }

            assertEquals(HttpStatusCode.OK, response.status)
            assertTrue(response.headers[HttpHeaders.XRequestId]!!.matches(Regex("[0-9a-f-]{36}")))
        }

    @Test
    fun `unknown routes get the error envelope with the request id`() =
        withApp {
            val response = client.get("/v1/does-not-exist")

            assertEquals(HttpStatusCode.NotFound, response.status)
            val error = response.error()
            assertEquals(ErrorCodes.NOT_FOUND, error.error.code)
            assertEquals(response.headers[HttpHeaders.XRequestId], error.error.requestId)
        }

    @Test
    fun `unhandled exceptions return 500 without leaking internals`() =
        withApp {
            val response = client.get("/test/boom")

            assertEquals(HttpStatusCode.InternalServerError, response.status)
            val body = response.bodyAsText()
            assertEquals(ErrorCodes.INTERNAL, ApiJson.decodeFromString<ErrorResponse>(body).error.code)
            assertFalse(body.contains("hunter2"), "exception message leaked")
            assertFalse(body.contains("IllegalStateException"), "exception class leaked")
            assertFalse(body.contains("at com.glide"), "stack trace leaked")
        }

    @Test
    fun `malformed JSON is a 400 with the error envelope`() =
        withApp {
            val response =
                client.post("/test/echo") {
                    contentType(ContentType.Application.Json)
                    setBody("""{"status": "MAYBE"""")
                }

            assertEquals(HttpStatusCode.BadRequest, response.status)
            assertEquals(ErrorCodes.BAD_REQUEST, response.error().error.code)
        }

    @Test
    fun `bodies over the limit are rejected with 413`() =
        withApp {
            val response =
                client.post("/test/echo") {
                    contentType(ContentType.Application.Json)
                    setBody("""{"version":"${"x".repeat(MAX_REQUEST_BODY_BYTES.toInt())}","status":"UP"}""")
                }

            assertEquals(HttpStatusCode.PayloadTooLarge, response.status)
            assertEquals(ErrorCodes.PAYLOAD_TOO_LARGE, response.error().error.code)
        }

    @Test
    fun `clients over the rate limit get 429 with the error envelope`() =
        withApp(testConfig(rateLimitPerMinute = 3)) {
            repeat(3) { assertEquals(HttpStatusCode.OK, client.get(ApiRoutes.HEALTH_LIVE).status) }

            val response = client.get(ApiRoutes.HEALTH_LIVE)

            assertEquals(HttpStatusCode.TooManyRequests, response.status)
            assertEquals(ErrorCodes.RATE_LIMITED, response.error().error.code)
            assertNotNull(response.headers[HttpHeaders.RetryAfter])
        }

    @Test
    fun `CORS allows configured origins only`() =
        withApp(testConfig(corsAllowedOrigins = listOf("https://admin.example.com"))) {
            val allowed = client.preflight("https://admin.example.com")
            val denied = client.preflight("https://evil.example.com")

            assertEquals("https://admin.example.com", allowed.headers[HttpHeaders.AccessControlAllowOrigin])
            assertNull(denied.headers[HttpHeaders.AccessControlAllowOrigin])
            assertNull(allowed.headers[HttpHeaders.AccessControlAllowCredentials])
        }

    private fun withApp(
        config: AppConfig = testConfig(),
        block: suspend ApplicationTestBuilder.() -> Unit,
    ) = testApplication {
        application {
            module(config, fakeDependencies())
            routing {
                get("/test/boom") { error("db password is hunter2") }
                post("/test/echo") { call.respond(call.receive<HealthResponse>()) }
            }
        }
        block()
    }

    private suspend fun io.ktor.client.HttpClient.preflight(origin: String) =
        options(ApiRoutes.HEALTH_LIVE) {
            header(HttpHeaders.Origin, origin)
            header(HttpHeaders.AccessControlRequestMethod, "GET")
        }

    private suspend fun HttpResponse.error() = ApiJson.decodeFromString<ErrorResponse>(bodyAsText())
}
