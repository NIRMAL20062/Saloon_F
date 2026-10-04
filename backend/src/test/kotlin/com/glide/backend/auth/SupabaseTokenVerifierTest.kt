package com.glide.backend.auth

import com.glide.backend.TestTokens
import com.glide.backend.config.SupabaseConfig
import com.sun.net.httpserver.HttpServer
import java.net.InetSocketAddress
import java.util.UUID
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.time.Duration.Companion.minutes

/** One test per way a token can be wrong. Only a genuine, current, signed-in user's token may pass. */
class SupabaseTokenVerifierTest {
    private val verifier = TestTokens.verifier

    @Test
    fun `a valid token gives the user id and phone`() {
        val id = UUID.randomUUID()

        val user = verifier.verify(TestTokens.token(userId = id, phone = "919000000001"))

        assertEquals(AuthenticatedUser(id, "919000000001"), user)
    }

    @Test
    fun `an admin login gives the email in lower case and says MFA was done`() {
        val id = UUID.randomUUID()

        val user =
            verifier.verify(
                TestTokens.token(userId = id, phone = null, email = "Admin@Glide.test", aal = "aal2"),
            )

        assertEquals(AuthenticatedUser(id, phone = null, email = "admin@glide.test", mfaVerified = true), user)
    }

    @Test
    fun `MFA counts only at level aal2`() {
        assertEquals(false, verifier.verify(TestTokens.token(aal = "aal1"))?.mfaVerified)
        assertEquals(false, verifier.verify(TestTokens.token(aal = null))?.mfaVerified)
        assertEquals(false, verifier.verify(TestTokens.token(aal = "AAL2 "))?.mfaVerified)
    }

    @Test
    fun `an empty email claim means no email`() {
        assertNull(verifier.verify(TestTokens.token(email = ""))?.email)
    }

    @Test
    fun `an expired token is refused`() {
        assertNull(verifier.verify(TestTokens.token(expiresIn = (-5).minutes)))
    }

    @Test
    fun `a token from another Supabase project is refused`() {
        assertNull(verifier.verify(TestTokens.token(issuer = "https://someone-else.supabase.co/auth/v1")))
    }

    @Test
    fun `a token for another audience is refused`() {
        assertNull(verifier.verify(TestTokens.token(audience = "service")))
    }

    @Test
    fun `the public anon role is refused`() {
        assertNull(verifier.verify(TestTokens.token(role = "anon")))
    }

    @Test
    fun `a token signed with a different key is refused`() {
        assertNull(verifier.verify(TestTokens.tokenSignedByOtherKey()))
    }

    @Test
    fun `an HS256 token is refused even with valid claims`() {
        assertNull(verifier.verify(TestTokens.hs256Token()))
    }

    @Test
    fun `garbage is refused`() {
        assertNull(verifier.verify("not-a-token"))
        assertNull(verifier.verify(""))
    }

    @Test
    fun `production verifier waits for a slow key server instead of locking everyone out`() {
        // Regression: Nimbus's default 500 ms timeout failed on a real network (750 ms) and then blocked retries for 30 s.
        val server = HttpServer.create(InetSocketAddress("127.0.0.1", 0), 0)
        server.createContext("/auth/v1/.well-known/jwks.json") { exchange ->
            Thread.sleep(SLOW_KEYS_MS)
            val body = TestTokens.publicJwksJson.toByteArray()
            exchange.responseHeaders.add("Content-Type", "application/json")
            exchange.sendResponseHeaders(200, body.size.toLong())
            exchange.responseBody.use { it.write(body) }
        }
        server.start()
        try {
            val url = "http://127.0.0.1:${server.address.port}"
            val production = SupabaseTokenVerifier.forProject(SupabaseConfig(url))
            val id = UUID.randomUUID()

            val user = production.verify(TestTokens.token(userId = id, issuer = "$url/auth/v1"))

            assertEquals(id, user?.id)
        } finally {
            server.stop(0)
        }
    }

    private companion object {
        const val SLOW_KEYS_MS = 1_200L
    }
}
