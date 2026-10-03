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
