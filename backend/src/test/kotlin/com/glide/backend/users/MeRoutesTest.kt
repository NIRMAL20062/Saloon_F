package com.glide.backend.users

import com.glide.backend.TestTokens
import com.glide.backend.db.TestDatabase
import com.glide.backend.fakeDependencies
import com.glide.backend.module
import com.glide.backend.testConfig
import com.glide.shared.api.ApiJson
import com.glide.shared.api.ApiRoutes
import com.glide.shared.error.ErrorCodes
import com.glide.shared.error.ErrorResponse
import com.glide.shared.me.MeErrorCodes
import com.glide.shared.me.MeResponse
import com.glide.shared.me.UserSide
import io.ktor.client.request.bearerAuth
import io.ktor.client.request.get
import io.ktor.client.request.put
import io.ktor.client.request.setBody
import io.ktor.client.statement.HttpResponse
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.HttpStatusCode
import io.ktor.http.contentType
import io.ktor.server.testing.ApplicationTestBuilder
import io.ktor.server.testing.testApplication
import java.util.UUID
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.time.Duration.Companion.minutes

/** /v1/me end to end: real auth plugin, real repository, real PostgreSQL. */
class MeRoutesTest {
    @Test
    fun `no token is 401 with the error envelope`() =
        withApp {
            val response = client.get(ApiRoutes.ME)

            assertEquals(HttpStatusCode.Unauthorized, response.status)
            assertEquals(ErrorCodes.UNAUTHORIZED, response.error().error.code)
        }

    @Test
    fun `an invalid or expired token is 401`() =
        withApp {
            assertEquals(HttpStatusCode.Unauthorized, client.get(ApiRoutes.ME) { bearerAuth("garbage") }.status)
            assertEquals(
                HttpStatusCode.Unauthorized,
                client.get(ApiRoutes.ME) { bearerAuth(TestTokens.token(expiresIn = (-1).minutes)) }.status,
            )
            assertEquals(
                HttpStatusCode.Unauthorized,
                client.get(ApiRoutes.ME) { bearerAuth(TestTokens.hs256Token()) }.status,
            )
        }

    @Test
    fun `a new user gets their id and phone and no side yet`() =
        withApp {
            val id = UUID.randomUUID()

            val response =
                client.get(
                    ApiRoutes.ME,
                ) { bearerAuth(TestTokens.token(userId = id, phone = "919000000001")) }

            assertEquals(HttpStatusCode.OK, response.status)
            assertEquals(MeResponse(id.toString(), "919000000001", null), response.me())
        }

    @Test
    fun `the onboarding choice is saved and returned afterwards`() =
        withApp {
            val token = TestTokens.token(phone = "919000000002")

            val saved =
                client.put(ApiRoutes.ME_SIDE) {
                    bearerAuth(token)
                    contentType(ContentType.Application.Json)
                    setBody("""{"side":"SALON"}""")
                }

            assertEquals(HttpStatusCode.OK, saved.status)
            assertEquals(UserSide.SALON, saved.me().side)
            assertEquals(UserSide.SALON, client.get(ApiRoutes.ME) { bearerAuth(token) }.me().side)
        }

    @Test
    fun `the choice is final - a different side is 409, the same side again is fine`() =
        withApp {
            val token = TestTokens.token(phone = "919000000003")
            assertEquals(HttpStatusCode.OK, putSide(token, "CUSTOMER").status)

            val again = putSide(token, "CUSTOMER")
            val switch = putSide(token, "SALON")

            assertEquals(HttpStatusCode.OK, again.status)
            assertEquals(HttpStatusCode.Conflict, switch.status)
            assertEquals(MeErrorCodes.SIDE_ALREADY_CHOSEN, switch.error().error.code)
            assertEquals(UserSide.CUSTOMER, client.get(ApiRoutes.ME) { bearerAuth(token) }.me().side)
        }

    @Test
    fun `an unknown side is a 400`() =
        withApp {
            val response =
                client.put(ApiRoutes.ME_SIDE) {
                    bearerAuth(TestTokens.token())
                    contentType(ContentType.Application.Json)
                    setBody("""{"side":"ADMIN"}""")
                }

            assertEquals(HttpStatusCode.BadRequest, response.status)
            assertEquals(ErrorCodes.BAD_REQUEST, response.error().error.code)
        }

    @Test
    fun `saving a side needs a token`() =
        withApp {
            val response =
                client.put(ApiRoutes.ME_SIDE) {
                    contentType(ContentType.Application.Json)
                    setBody("""{"side":"CUSTOMER"}""")
                }

            assertEquals(HttpStatusCode.Unauthorized, response.status)
        }

    @Test
    fun `a new user has no name yet, then saves a profile and gets it back`() =
        withApp {
            val token = TestTokens.token(phone = "919000000011")
            assertEquals(null, client.get(ApiRoutes.ME) { bearerAuth(token) }.me().name)

            val saved = putProfile(token, """{"name":"  Priya Sharma  ","email":" Priya@Example.COM "}""")

            assertEquals(HttpStatusCode.OK, saved.status)
            assertEquals("Priya Sharma", saved.me().name)
            assertEquals("priya@example.com", saved.me().email)
            val again = client.get(ApiRoutes.ME) { bearerAuth(token) }.me()
            assertEquals("Priya Sharma" to "priya@example.com", again.name to again.email)
        }

    @Test
    fun `a returning user can change their name and clear the email`() =
        withApp {
            val token = TestTokens.token(phone = "919000000012")
            putProfile(token, """{"name":"Rahul","email":"rahul@example.com"}""")

            val changed = putProfile(token, """{"name":"Rahul Verma","email":""}""").me()

            assertEquals("Rahul Verma", changed.name)
            assertEquals(null, changed.email)
        }

    @Test
    fun `email is optional`() =
        withApp {
            val response = putProfile(TestTokens.token(phone = "919000000013"), """{"name":"Asha"}""")

            assertEquals(HttpStatusCode.OK, response.status)
            assertEquals(null, response.me().email)
        }

    @Test
    fun `a name that is too short, too long or blank is refused`() =
        withApp {
            val token = TestTokens.token(phone = "919000000014")
            listOf("A", " ", "", "x".repeat(61), "Bad\u0007Name").forEach { name ->
                val response = putProfile(token, """{"name":"$name"}""")

                assertEquals(HttpStatusCode.BadRequest, response.status, "name '$name'")
                assertEquals(MeErrorCodes.INVALID_NAME, response.error().error.code)
            }
            assertEquals(null, client.get(ApiRoutes.ME) { bearerAuth(token) }.me().name)
        }

    @Test
    fun `a name of exactly 2 and 60 characters is fine`() =
        withApp {
            val token = TestTokens.token(phone = "919000000015")

            assertEquals(HttpStatusCode.OK, putProfile(token, """{"name":"Al"}""").status)
            assertEquals(HttpStatusCode.OK, putProfile(token, """{"name":"${"y".repeat(60)}"}""").status)
        }

    @Test
    fun `an invalid email is refused`() =
        withApp {
            val token = TestTokens.token(phone = "919000000016")
            listOf(
                "priya",
                "priya@",
                "@example.com",
                "pri ya@example.com",
                "priya@example",
                "a".repeat(250) + "@x.in",
            ).forEach { email ->
                val response = putProfile(token, """{"name":"Priya","email":"$email"}""")

                assertEquals(HttpStatusCode.BadRequest, response.status, "email '$email'")
                assertEquals(MeErrorCodes.INVALID_EMAIL, response.error().error.code)
            }
        }

    @Test
    fun `a missing name or a broken body is a 400`() =
        withApp {
            val token = TestTokens.token(phone = "919000000017")

            assertEquals(HttpStatusCode.BadRequest, putProfile(token, """{"email":"a@b.in"}""").status)
            assertEquals(HttpStatusCode.BadRequest, putProfile(token, """not json""").status)
        }

    @Test
    fun `saving a profile needs a login`() =
        withApp {
            val response =
                client.put(ApiRoutes.ME_PROFILE) {
                    contentType(ContentType.Application.Json)
                    setBody("""{"name":"Priya"}""")
                }

            assertEquals(HttpStatusCode.Unauthorized, response.status)
        }

    @Test
    fun `a person only ever changes their own profile`() =
        withApp {
            val priya = TestTokens.token(userId = UUID.randomUUID(), phone = "919000000018")
            val rahul = TestTokens.token(userId = UUID.randomUUID(), phone = "919000000019")
            putProfile(rahul, """{"name":"Rahul"}""")

            putProfile(priya, """{"name":"Priya"}""")

            assertEquals("Rahul", client.get(ApiRoutes.ME) { bearerAuth(rahul) }.me().name)
            assertEquals("Priya", client.get(ApiRoutes.ME) { bearerAuth(priya) }.me().name)
        }

    private suspend fun ApplicationTestBuilder.putProfile(
        token: String,
        body: String,
    ) = client.put(ApiRoutes.ME_PROFILE) {
        bearerAuth(token)
        contentType(ContentType.Application.Json)
        setBody(body)
    }

    private fun withApp(block: suspend ApplicationTestBuilder.() -> Unit) =
        testApplication {
            val deps = fakeDependencies().copy(users = ExposedUserRepository(TestDatabase.exposed))
            application { module(testConfig(database = TestDatabase.config), deps) }
            block()
        }

    private suspend fun ApplicationTestBuilder.putSide(
        token: String,
        side: String,
    ) = client.put(ApiRoutes.ME_SIDE) {
        bearerAuth(token)
        contentType(ContentType.Application.Json)
        setBody("""{"side":"$side"}""")
    }

    private suspend fun HttpResponse.me() = ApiJson.decodeFromString<MeResponse>(bodyAsText())

    private suspend fun HttpResponse.error() = ApiJson.decodeFromString<ErrorResponse>(bodyAsText())
}
