package com.glide.android.data.network

import com.glide.shared.health.HealthResponse
import com.glide.shared.health.HealthStatus
import kotlinx.coroutines.test.runTest
import mockwebserver3.MockResponse
import mockwebserver3.MockWebServer
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import retrofit2.http.GET

/** Exercises the real OkHttp + Retrofit + ApiJson setup the app uses, against a local fake server. */
class NetworkLayerTest {
    private interface ProbeApi {
        @GET("/probe")
        suspend fun probe(): HealthResponse
    }

    private val server = MockWebServer()
    private lateinit var probeApi: ProbeApi
    private lateinit var glideApi: GlideApi

    @Before
    fun setUp() {
        server.start()
        val retrofit = createRetrofit(server.url("/").toString(), createOkHttpClient(debugLogging = false))
        probeApi = retrofit.create(ProbeApi::class.java)
        glideApi = retrofit.create(GlideApi::class.java)
    }

    @After
    fun tearDown() {
        server.close()
    }

    @Test
    fun `success is decoded with the shared contract`() =
        runTest {
            server.enqueue(json(200, """{"status":"UP","version":"0.1.0","database":"UP"}"""))

            val result = apiCall { probeApi.probe() }

            assertEquals(ApiResult.Success(HealthResponse(HealthStatus.UP, "0.1.0", HealthStatus.UP)), result)
        }

    @Test
    fun `fields added later by the backend are ignored`() =
        runTest {
            server.enqueue(json(200, """{"status":"UP","version":"0.1.0","newField":{"a":1}}"""))

            val result = apiCall { probeApi.probe() }

            assertEquals(ApiResult.Success(HealthResponse(HealthStatus.UP, "0.1.0")), result)
        }

    @Test
    fun `error envelope becomes an Http error with code and request id`() =
        runTest {
            server.enqueue(
                json(404, """{"error":{"code":"NOT_FOUND","message":"No such endpoint.","requestId":"req-1"}}"""),
            )

            val result = apiCall { probeApi.probe() }

            assertEquals(ApiResult.Failure(ApiError.Http(404, "NOT_FOUND", "req-1")), result)
        }

    @Test
    fun `non-envelope error body still gives status and header request id`() =
        runTest {
            server.enqueue(
                MockResponse
                    .Builder()
                    .code(502)
                    .addHeader(REQUEST_ID_HEADER, "edge-7")
                    .body("<html>Bad gateway</html>")
                    .build(),
            )

            val result = apiCall { probeApi.probe() }

            assertEquals(ApiResult.Failure(ApiError.Http(502, null, "edge-7")), result)
        }

    @Test
    fun `no connection becomes a Network error`() =
        runTest {
            server.close()

            val result = apiCall { probeApi.probe() }

            assertEquals(ApiResult.Failure(ApiError.Network), result)
        }

    @Test
    fun `a body that breaks the contract becomes Unexpected`() =
        runTest {
            server.enqueue(json(200, """{"status":"MAYBE","version":"0.1.0"}"""))

            val result = apiCall { probeApi.probe() }

            assertEquals(ApiResult.Failure(ApiError.Unexpected), result)
        }

    @Test
    fun `every request carries a request id the backend accepts`() =
        runTest {
            server.enqueue(json(200, """{"status":"UP","version":"1"}"""))

            apiCall { probeApi.probe() }

            val id = server.takeRequest().headers[REQUEST_ID_HEADER]!!
            assertTrue("unexpected id $id", id.matches(Regex("^android-[0-9a-f-]{36}$")))
            // Backend rule (plugins/Monitoring.kt): 1..64 chars of letters, digits, '-' or '_'.
            assertTrue(id.length <= 64)
        }

    @Test
    fun `health calls the shared route and keeps the 503 body readable`() =
        runTest {
            server.enqueue(json(503, """{"status":"DOWN","version":"0.1.0","database":"DOWN"}"""))

            val response = glideApi.health()

            assertEquals("/health", server.takeRequest().url.encodedPath)
            assertEquals(503, response.code())
            assertTrue(response.errorBody()!!.string().contains("\"database\":\"DOWN\""))
        }

    private fun json(
        code: Int,
        body: String,
    ) = MockResponse
        .Builder()
        .code(code)
        .addHeader("Content-Type", "application/json")
        .body(body)
        .build()
}
