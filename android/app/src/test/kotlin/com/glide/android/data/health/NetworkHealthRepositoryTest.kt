package com.glide.android.data.health

import com.glide.android.data.network.ApiError
import com.glide.android.data.network.ApiResult
import com.glide.android.data.network.GlideApi
import com.glide.android.data.network.createOkHttpClient
import com.glide.android.data.network.createRetrofit
import com.glide.shared.health.HealthResponse
import com.glide.shared.health.HealthStatus
import kotlinx.coroutines.test.runTest
import mockwebserver3.MockResponse
import mockwebserver3.MockWebServer
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test

class NetworkHealthRepositoryTest {
    private val server = MockWebServer()
    private lateinit var repository: NetworkHealthRepository

    @Before
    fun setUp() {
        server.start()
        val api = createRetrofit(server.url("/").toString(), createOkHttpClient(false)).create(GlideApi::class.java)
        repository = NetworkHealthRepository(api)
    }

    @After
    fun tearDown() = server.close()

    @Test
    fun `200 is a healthy backend`() =
        runTest {
            server.enqueue(json(200, """{"status":"UP","version":"0.1.0","database":"UP"}"""))

            assertEquals(
                ApiResult.Success(HealthResponse(HealthStatus.UP, "0.1.0", HealthStatus.UP)),
                repository.health(),
            )
        }

    @Test
    fun `503 from our backend is a result, not an error`() =
        runTest {
            server.enqueue(json(503, """{"status":"DOWN","version":"0.1.0","database":"DOWN"}"""))

            assertEquals(
                ApiResult.Success(HealthResponse(HealthStatus.DOWN, "0.1.0", HealthStatus.DOWN)),
                repository.health(),
            )
        }

    @Test
    fun `503 from a proxy is an http error`() =
        runTest {
            server.enqueue(
                MockResponse
                    .Builder()
                    .code(503)
                    .body("<html>Service Unavailable</html>")
                    .build(),
            )

            assertEquals(ApiResult.Failure(ApiError.Http(503, null, null)), repository.health())
        }

    @Test
    fun `backend unreachable is a network error`() =
        runTest {
            server.close()

            assertEquals(ApiResult.Failure(ApiError.Network), repository.health())
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
