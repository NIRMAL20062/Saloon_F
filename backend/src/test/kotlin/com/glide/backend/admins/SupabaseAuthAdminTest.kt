package com.glide.backend.admins

import com.glide.backend.config.SupabaseConfig
import com.sun.net.httpserver.HttpServer
import kotlinx.coroutines.runBlocking
import java.net.InetSocketAddress
import java.util.UUID
import java.util.concurrent.CopyOnWriteArrayList
import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertSame
import kotlin.test.assertTrue

/** The Supabase admin client against a local stand-in for Supabase's /auth/v1 API. */
class SupabaseAuthAdminTest {
    private data class Seen(
        val method: String,
        val path: String,
        val headers: Map<String, String?>,
        val body: String,
    )

    private val seen = CopyOnWriteArrayList<Seen>()
    private var status = 200
    private var answer = """{"id":"$USER_ID","email":"b@glide.test"}"""

    private val server =
        HttpServer.create(InetSocketAddress("127.0.0.1", 0), 0).apply {
            createContext("/") { exchange ->
                seen +=
                    Seen(
                        exchange.requestMethod,
                        exchange.requestURI.path,
                        listOf("apikey", "Authorization", "Content-Type").associateWith {
                            exchange.requestHeaders.getFirst(it)
                        },
                        exchange.requestBody.readAllBytes().decodeToString(),
                    )
                val bytes = answer.encodeToByteArray()
                exchange.responseHeaders.add("Content-Type", "application/json")
                exchange.sendResponseHeaders(status, bytes.size.toLong())
                exchange.responseBody.use { it.write(bytes) }
            }
            start()
        }

    private val url = "http://127.0.0.1:${server.address.port}"

    @AfterTest
    fun stop() = server.stop(0)

    @Test
    fun `invite posts the email with the secret key and returns the new login's id`() =
        runBlocking {
            val result = SupabaseAuthAdmin(url, SECRET_KEY).invite("b@glide.test")

            assertEquals(InviteResult.Invited(UUID.fromString(USER_ID)), result)
            val request = seen.single()
            assertEquals("POST /auth/v1/invite", "${request.method} ${request.path}")
            assertEquals("""{"email":"b@glide.test"}""", request.body)
            assertEquals(SECRET_KEY, request.headers["apikey"])
            assertEquals("application/json", request.headers["Content-Type"])
            // A new sb_secret_ key is never sent as a bearer token (Supabase rejects that).
            assertNull(request.headers["Authorization"])
        }

    @Test
    fun `the legacy service_role key is also sent as the bearer token`() =
        runBlocking {
            SupabaseAuthAdmin(url, LEGACY_KEY).invite("b@glide.test")

            assertEquals("Bearer $LEGACY_KEY", seen.single().headers["Authorization"])
            assertEquals(LEGACY_KEY, seen.single().headers["apikey"])
        }

    @Test
    fun `an email that already has a login is reported, not treated as a failure`() =
        runBlocking {
            status = 422
            answer =
                """{"code":422,"error_code":"email_exists","msg":"A user with this email address has already been registered"}"""

            assertSame(InviteResult.AlreadyRegistered, SupabaseAuthAdmin(url, SECRET_KEY).invite("b@glide.test"))
        }

    @Test
    fun `any other refusal is an AuthAdminException with only the status and Supabase's code`() {
        status = 500
        answer = """{"code":500,"error_code":"unexpected_failure","msg":"Error sending invite email to b@glide.test"}"""

        val error =
            assertFailsWith<AuthAdminException> {
                runBlocking {
                    SupabaseAuthAdmin(
                        url,
                        SECRET_KEY,
                    ).invite("b@glide.test")
                }
            }

        assertEquals(500, error.status)
        assertEquals("unexpected_failure", error.errorCode)
        assertFalse(error.message.orEmpty().contains("b@glide.test"), "the email must not end up in logs")
    }

    @Test
    fun `findOrCreateUser generates a magic link without sending it and returns only the id`() =
        runBlocking {
            answer =
                """{"id":"$USER_ID","email":"b@glide.test","action_link":"https://x/verify?token=t","email_otp":"123456"}"""

            val id = SupabaseAuthAdmin(url, SECRET_KEY).findOrCreateUser("b@glide.test")

            assertEquals(UUID.fromString(USER_ID), id)
            val request = seen.single()
            assertEquals("POST /auth/v1/admin/generate_link", "${request.method} ${request.path}")
            assertEquals("""{"type":"magiclink","email":"b@glide.test"}""", request.body)
        }

    @Test
    fun `an answer without a user id is a failure`() {
        answer = """{"email":"b@glide.test"}"""

        assertFailsWith<AuthAdminException> {
            runBlocking { SupabaseAuthAdmin(url, SECRET_KEY).findOrCreateUser("b@glide.test") }
        }
    }

    @Test
    fun `Supabase unreachable is an AuthAdminException without a status`() {
        server.stop(0)

        val error =
            assertFailsWith<AuthAdminException> {
                runBlocking {
                    SupabaseAuthAdmin(
                        url,
                        SECRET_KEY,
                    ).invite("b@glide.test")
                }
            }

        assertNull(error.status)
    }

    @Test
    fun `without a secret key every call says invites are unavailable`() {
        val admin = SupabaseAuthAdmin.forProject(SupabaseConfig("https://x.supabase.co", secretKey = null))

        assertSame(DisabledAuthAdmin, admin)
        assertFailsWith<AuthAdminUnavailableException> { runBlocking { admin.invite("b@glide.test") } }
        assertFailsWith<AuthAdminUnavailableException> { runBlocking { admin.findOrCreateUser("b@glide.test") } }
    }

    @Test
    fun `with a secret key the real client is used`() {
        assertTrue(
            SupabaseAuthAdmin.forProject(SupabaseConfig("https://x.supabase.co", SECRET_KEY)) is SupabaseAuthAdmin,
        )
    }

    private companion object {
        const val USER_ID = "3f0c9a52-7d1e-4b8a-9a0e-2c1d5b6e7f80"
        const val SECRET_KEY = "sb_secret_test_only_0123456789abcdef"
        const val LEGACY_KEY = "eyJhbGciOiJIUzI1NiJ9.eyJyb2xlIjoic2VydmljZV9yb2xlIn0.c2lnbmF0dXJl"
    }
}
