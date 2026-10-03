package com.glide.android.data.network

import com.glide.shared.me.MeResponse
import kotlinx.coroutines.test.runTest
import mockwebserver3.MockResponse
import mockwebserver3.MockWebServer
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test

/** The backend client with login tokens: bearer header, one refresh on 401, no loops. */
class AuthenticatedClientTest {
    private val server = MockWebServer()
    private val tokens = FakeTokens()
    private lateinit var api: GlideApi

    @Before
    fun setUp() {
        server.start()
        api = createRetrofit(server.url("/").toString(), createOkHttpClient(false, tokens)).create(GlideApi::class.java)
    }

    @After
    fun tearDown() = server.close()

    @Test
    fun `calls carry the signed-in user's token`() =
        runTest {
            tokens.token = "token-1"
            server.enqueue(me())

            apiCall { api.me() }

            assertEquals("Bearer token-1", server.takeRequest().headers["Authorization"])
        }

    @Test
    fun `signed out means no Authorization header`() =
        runTest {
            tokens.token = null
            server.enqueue(me())

            apiCall { api.me() }

            assertNull(server.takeRequest().headers["Authorization"])
        }

    @Test
    fun `a 401 refreshes the token once and retries`() =
        runTest {
            tokens.token = "old"
            tokens.refreshed = "new"
            server.enqueue(MockResponse.Builder().code(401).build())
            server.enqueue(me())

            val result = apiCall { api.me() }

            assertEquals(ApiResult.Success(MeResponse("user-1", "919000000001")), result)
            assertEquals("Bearer old", server.takeRequest().headers["Authorization"])
            assertEquals("Bearer new", server.takeRequest().headers["Authorization"])
            assertEquals(listOf("old"), tokens.rejected)
        }

    @Test
    fun `if the refresh doesn't help the 401 reaches the caller and nothing loops`() =
        runTest {
            tokens.token = "old"
            tokens.refreshed = "still-bad"
            repeat(3) { server.enqueue(MockResponse.Builder().code(401).build()) }

            val result = apiCall { api.me() }

            assertEquals(401, ((result as ApiResult.Failure).error as ApiError.Http).status)
            assertEquals(2, server.requestCount)
        }

    @Test
    fun `no refresh possible means the 401 is returned`() =
        runTest {
            tokens.token = "old"
            tokens.refreshed = null
            server.enqueue(MockResponse.Builder().code(401).build())

            val result = apiCall { api.me() }

            assertEquals(401, ((result as ApiResult.Failure).error as ApiError.Http).status)
            assertEquals(1, server.requestCount)
        }

    private fun me() =
        MockResponse
            .Builder()
            .code(200)
            .addHeader("Content-Type", "application/json")
            .body("""{"id":"user-1","phone":"919000000001"}""")
            .build()

    private class FakeTokens : AccessTokens {
        var token: String? = null
        var refreshed: String? = null
        val rejected = mutableListOf<String?>()

        override suspend fun current() = token

        override suspend fun refreshAfterRejection(rejected: String?): String? {
            this.rejected += rejected
            refreshed?.let { token = it }
            return refreshed
        }
    }
}
