package com.glide.android.data.auth

import kotlinx.coroutines.test.runTest
import mockwebserver3.MockResponse
import mockwebserver3.MockWebServer
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

/** The real Supabase client and repository against a fake Supabase server. */
class AuthRepositoryTest {
    private val server = MockWebServer()
    private val store = InMemorySessionStore()
    private var now = 1_000_000L
    private lateinit var repository: AuthRepository

    @Before
    fun setUp() {
        server.start()
        val api = createSupabaseAuthApi(server.url("/").toString(), "sb_publishable_test", debugLogging = false)
        repository = AuthRepository(api, store, EpochClock { now })
    }

    @After
    fun tearDown() = server.close()

    @Test
    fun `requesting an OTP sends the phone and the public key`() =
        runTest {
            server.enqueue(json(200, "{}"))

            val result = repository.requestOtp("919000000001")

            assertEquals(AuthResult.Success(Unit), result)
            val request = server.takeRequest()
            assertEquals("/auth/v1/otp", request.url.encodedPath)
            assertEquals("sb_publishable_test", request.headers["apikey"])
            assertEquals("""{"phone":"919000000001"}""", request.body?.utf8())
        }

    @Test
    fun `too many OTP requests is reported as rate limited`() =
        runTest {
            server.enqueue(json(429, """{"code":429,"error_code":"over_sms_send_rate_limit","msg":"slow down"}"""))

            assertEquals(AuthResult.Failure(AuthError.RATE_LIMITED), repository.requestOtp("919000000001"))
        }

    @Test
    fun `a phone Supabase refuses is an invalid phone`() =
        runTest {
            server.enqueue(json(400, """{"code":400,"error_code":"validation_failed","msg":"bad phone"}"""))

            assertEquals(AuthResult.Failure(AuthError.INVALID_PHONE), repository.requestOtp("91123"))
        }

    @Test
    fun `the right code signs in and stores the session`() =
        runTest {
            server.enqueue(json(200, sessionJson(access = "access-1", refresh = "refresh-1", expiresIn = 3600)))

            val result = repository.verifyOtp("919000000001", "123456")

            val expected = Session("access-1", "refresh-1", now + 3600, "user-1", "919000000001")
            assertEquals(AuthResult.Success(expected), result)
            assertEquals(expected, repository.session.value)
            assertEquals(expected, store.load())
            assertEquals(
                """{"phone":"919000000001","token":"123456","type":"sms"}""",
                server.takeRequest().body?.utf8(),
            )
        }

    @Test
    fun `a wrong or expired code is an invalid code and stays signed out`() =
        runTest {
            server.enqueue(
                json(403, """{"code":403,"error_code":"otp_expired","msg":"Token has expired or is invalid"}"""),
            )

            assertEquals(AuthResult.Failure(AuthError.INVALID_CODE), repository.verifyOtp("919000000001", "000000"))
            assertNull(repository.session.value)
        }

    @Test
    fun `no internet is a network error`() =
        runTest {
            server.close()

            assertEquals(AuthResult.Failure(AuthError.NETWORK), repository.requestOtp("919000000001"))
        }

    @Test
    fun `a fresh token is returned without refreshing`() =
        runTest {
            signIn(expiresIn = 3600)

            assertEquals("access-1", repository.accessToken())
            assertEquals(1, server.requestCount) // only the sign-in call
        }

    @Test
    fun `a token about to expire is refreshed first`() =
        runTest {
            signIn(expiresIn = 3600)
            now += 3590
            server.enqueue(json(200, sessionJson(access = "access-2", refresh = "refresh-2", expiresIn = 3600)))

            assertEquals("access-2", repository.accessToken())
            val refresh = server.takeRequest().let { server.takeRequest() }
            assertEquals("grant_type=refresh_token", refresh.url.encodedQuery)
            assertEquals("""{"refresh_token":"refresh-1"}""", refresh.body?.utf8())
            assertEquals("refresh-2", store.load()?.refreshToken)
        }

    @Test
    fun `a refresh token Supabase no longer accepts signs out`() =
        runTest {
            signIn(expiresIn = 3600)
            server.enqueue(json(400, """{"code":400,"error_code":"refresh_token_not_found","msg":"gone"}"""))

            assertNull(repository.refresh(staleToken = "access-1"))
            assertNull(repository.session.value)
            assertNull(store.load())
        }

    @Test
    fun `a refresh that fails for lack of internet keeps the session`() =
        runTest {
            signIn(expiresIn = 3600)
            server.close()

            assertNull(repository.refresh(staleToken = "access-1"))
            assertEquals("access-1", repository.session.value?.accessToken)
        }

    @Test
    fun `logout clears the session even when Supabase can't be reached`() =
        runTest {
            signIn(expiresIn = 3600)
            server.close()

            repository.logout()

            assertNull(repository.session.value)
            assertNull(store.load())
        }

    @Test
    fun `logout tells Supabase with the access token`() =
        runTest {
            signIn(expiresIn = 3600)
            server.enqueue(MockResponse.Builder().code(204).build())

            repository.logout()

            server.takeRequest()
            val logout = server.takeRequest()
            assertEquals("/auth/v1/logout", logout.url.encodedPath)
            assertTrue(logout.headers["Authorization"] == "Bearer access-1")
        }

    private suspend fun signIn(expiresIn: Long) {
        server.enqueue(json(200, sessionJson(access = "access-1", refresh = "refresh-1", expiresIn = expiresIn)))
        repository.verifyOtp("919000000001", "123456")
    }

    private fun sessionJson(
        access: String,
        refresh: String,
        expiresIn: Long,
    ) = """{"access_token":"$access","token_type":"bearer","expires_in":$expiresIn,"refresh_token":"$refresh",""" +
        """"user":{"id":"user-1","phone":"919000000001","role":"authenticated"}}"""

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

class InMemorySessionStore : SessionStore {
    private var session: Session? = null

    override fun load() = session

    override fun save(session: Session) {
        this.session = session
    }

    override fun clear() {
        session = null
    }
}
