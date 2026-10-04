package com.glide.backend.admins

import com.glide.backend.FakeAuthAdmin
import com.glide.backend.TestTokens
import com.glide.backend.db.TestDatabase
import com.glide.backend.fakeDependencies
import com.glide.backend.module
import com.glide.backend.testConfig
import com.glide.backend.users.ExposedUserRepository
import com.glide.shared.admin.AdminErrorCodes
import com.glide.shared.admin.AdminResponse
import com.glide.shared.admin.AdminStatus
import com.glide.shared.api.ApiJson
import com.glide.shared.api.ApiRoutes
import com.glide.shared.error.ErrorCodes
import com.glide.shared.error.ErrorResponse
import io.ktor.client.request.bearerAuth
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.statement.HttpResponse
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.HttpStatusCode
import io.ktor.http.contentType
import io.ktor.server.testing.ApplicationTestBuilder
import io.ktor.server.testing.testApplication
import kotlinx.coroutines.runBlocking
import java.util.UUID
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

/** /v1/admin end to end: real auth plugin, real repository, real PostgreSQL; Supabase's admin API is a fake. */
class AdminRoutesTest {
    private val admins = ExposedAdminRepository(TestDatabase.exposed)
    private val supabase = FakeAuthAdmin()
    private val firstId = UUID.randomUUID()

    @BeforeTest
    fun firstAdmin() {
        resetAdmins()
        runBlocking { admins.addFirst(firstId, "first@glide.test") }
    }

    private val firstAdmin get() = TestTokens.adminToken(firstId, "first@glide.test")

    // ---- Who gets in ----

    @Test
    fun `no token is 401`() =
        withApp {
            assertEquals(HttpStatusCode.Unauthorized, client.get(ApiRoutes.ADMIN_ME).status)
            assertEquals(HttpStatusCode.Unauthorized, invite(token = null, """{"email":"b@glide.test"}""").status)
        }

    @Test
    fun `a non-admin is refused with NOT_ADMIN, even with MFA done`() =
        withApp {
            val phoneUser = TestTokens.token(phone = "919000000001")
            val strangerWithMfa = TestTokens.adminToken(UUID.randomUUID(), "stranger@glide.test", mfa = true)

            listOf(phoneUser, strangerWithMfa).forEach { token ->
                val me = client.get(ApiRoutes.ADMIN_ME) { bearerAuth(token) }
                assertEquals(HttpStatusCode.Forbidden, me.status)
                assertEquals(AdminErrorCodes.NOT_ADMIN, me.error().error.code)

                val invited = invite(token, """{"email":"b@glide.test"}""")
                assertEquals(HttpStatusCode.Forbidden, invited.status)
                assertEquals(AdminErrorCodes.NOT_ADMIN, invited.error().error.code)
            }
            assertEquals(emptyList(), supabase.invitesSent)
        }

    @Test
    fun `an admin is matched on their login's id, never on the email in the token`() =
        withApp {
            val sameEmailOtherLogin = TestTokens.adminToken(UUID.randomUUID(), "first@glide.test")

            val response = client.get(ApiRoutes.ADMIN_ME) { bearerAuth(sameEmailOtherLogin) }

            assertEquals(HttpStatusCode.Forbidden, response.status)
            assertEquals(AdminErrorCodes.NOT_ADMIN, response.error().error.code)
        }

    @Test
    fun `an admin without the authenticator-app step is refused with MFA_REQUIRED and stays INVITED`() =
        withApp {
            val withoutMfa = TestTokens.adminToken(firstId, "first@glide.test", mfa = false)

            val me = client.get(ApiRoutes.ADMIN_ME) { bearerAuth(withoutMfa) }
            val invited = invite(withoutMfa, """{"email":"b@glide.test"}""")

            assertEquals(HttpStatusCode.Forbidden, me.status)
            assertEquals(AdminErrorCodes.MFA_REQUIRED, me.error().error.code)
            assertEquals(HttpStatusCode.Forbidden, invited.status)
            assertEquals(AdminErrorCodes.MFA_REQUIRED, invited.error().error.code)
            assertEquals(AdminStatus.INVITED, admins.find(firstId)!!.status)
            assertEquals(emptyList(), supabase.invitesSent)
        }

    @Test
    fun `an admin with MFA gets their details, and their first such login makes them ACTIVE`() =
        withApp {
            val response = client.get(ApiRoutes.ADMIN_ME) { bearerAuth(firstAdmin) }

            assertEquals(HttpStatusCode.OK, response.status)
            assertEquals(AdminResponse(firstId.toString(), "first@glide.test", AdminStatus.ACTIVE), response.admin())
            assertEquals(AdminStatus.ACTIVE, admins.find(firstId)!!.status)
        }

    // ---- Invites ----

    @Test
    fun `an admin invites someone, Supabase sends the invite and a pending admin is saved`() =
        withApp {
            val response = invite(firstAdmin, """{"email":"  New.Admin@Glide.TEST "}""")

            assertEquals(HttpStatusCode.Created, response.status)
            val newId = supabase.loginOf("new.admin@glide.test")!!
            assertEquals(AdminResponse(newId.toString(), "new.admin@glide.test", AdminStatus.INVITED), response.admin())
            assertEquals(listOf("new.admin@glide.test"), supabase.invitesSent)
            assertEquals(
                Admin(newId, "new.admin@glide.test", AdminStatus.INVITED, invitedBy = firstId),
                admins.find(newId),
            )
        }

    @Test
    fun `the invite is audit-logged with the inviter and the request ID`() =
        withApp {
            val response =
                client.post(ApiRoutes.ADMIN_INVITES) {
                    bearerAuth(firstAdmin)
                    header("X-Request-Id", "invite-req-1")
                    contentType(ContentType.Application.Json)
                    setBody("""{"email":"audited@glide.test"}""")
                }
            val newId = supabase.loginOf("audited@glide.test")!!

            assertEquals(HttpStatusCode.Created, response.status)
            assertEquals(listOf(Triple(firstId, "ADMIN_INVITED", "invite-req-1")), audit(newId))
        }

    @Test
    fun `the invited admin can then use the admin website with their own login`() =
        withApp {
            invite(firstAdmin, """{"email":"b@glide.test"}""")
            val b = supabase.loginOf("b@glide.test")!!

            val response = client.get(ApiRoutes.ADMIN_ME) { bearerAuth(TestTokens.adminToken(b, "b@glide.test")) }

            assertEquals(HttpStatusCode.OK, response.status)
            assertEquals(AdminStatus.ACTIVE, response.admin().status)
        }

    @Test
    fun `inviting an email that is already an admin is 409 and sends nothing`() =
        withApp {
            invite(firstAdmin, """{"email":"b@glide.test"}""")

            listOf("b@glide.test", "B@GLIDE.test", "first@glide.test").forEach { email ->
                val response = invite(firstAdmin, """{"email":"$email"}""")

                assertEquals(HttpStatusCode.Conflict, response.status, email)
                assertEquals(AdminErrorCodes.ADMIN_ALREADY_EXISTS, response.error().error.code)
            }
            assertEquals(listOf("b@glide.test"), supabase.invitesSent)
        }

    @Test
    fun `an email that already has a Supabase login becomes a pending admin without an invite email`() =
        withApp {
            val existing = supabase.existingLogin("signed.up@glide.test")

            val response = invite(firstAdmin, """{"email":"signed.up@glide.test"}""")

            assertEquals(HttpStatusCode.Created, response.status)
            assertEquals(existing.toString(), response.admin().id)
            assertEquals(emptyList(), supabase.invitesSent)
        }

    @Test
    fun `an invalid email is 400 and nothing is sent or saved`() =
        withApp {
            val invalid = listOf("", "b", "b@", "@glide.test", "b c@glide.test", "b@glide", "a".repeat(250) + "@x.in")
            invalid.forEach { email ->
                val response = invite(firstAdmin, """{"email":"$email"}""")

                assertEquals(HttpStatusCode.BadRequest, response.status, "email '$email'")
                assertEquals(AdminErrorCodes.INVALID_EMAIL, response.error().error.code)
            }
            assertEquals(emptyList(), supabase.invitesSent)
        }

    @Test
    fun `a missing email or a broken body is 400`() =
        withApp {
            listOf("""{}""", """not json""", """{"email":5}""").forEach { body ->
                val response = invite(firstAdmin, body)

                assertEquals(HttpStatusCode.BadRequest, response.status, body)
                assertEquals(ErrorCodes.BAD_REQUEST, response.error().error.code)
            }
        }

    @Test
    fun `Supabase failing is 502 and nothing is saved`() =
        withApp {
            supabase.failWith = AuthAdminException(status = 500, errorCode = "unexpected_failure")

            val response = invite(firstAdmin, """{"email":"b@glide.test"}""")

            assertEquals(HttpStatusCode.BadGateway, response.status)
            assertEquals(AdminErrorCodes.INVITE_FAILED, response.error().error.code)
            assertNull(admins.findByEmail("b@glide.test"))
        }

    @Test
    fun `without a Supabase secret key invites are 503 and nothing is saved`() =
        withApp(authAdmin = DisabledAuthAdmin) {
            val response = invite(firstAdmin, """{"email":"b@glide.test"}""")

            assertEquals(HttpStatusCode.ServiceUnavailable, response.status)
            assertEquals(AdminErrorCodes.INVITES_UNAVAILABLE, response.error().error.code)
            assertNull(admins.findByEmail("b@glide.test"))
        }

    @Test
    fun `error answers never echo the email`() =
        withApp {
            invite(firstAdmin, """{"email":"b@glide.test"}""")

            val conflict = invite(firstAdmin, """{"email":"b@glide.test"}""").bodyAsText()

            assertTrue("b@glide.test" !in conflict, conflict)
        }

    private suspend fun ApplicationTestBuilder.invite(
        token: String?,
        body: String,
    ) = client.post(ApiRoutes.ADMIN_INVITES) {
        if (token != null) bearerAuth(token)
        contentType(ContentType.Application.Json)
        setBody(body)
    }

    private fun withApp(
        authAdmin: AuthAdmin = supabase,
        block: suspend ApplicationTestBuilder.() -> Unit,
    ) = testApplication {
        val deps =
            fakeDependencies().copy(
                users = ExposedUserRepository(TestDatabase.exposed),
                admins = admins,
                authAdmin = authAdmin,
            )
        application { module(testConfig(database = TestDatabase.config), deps) }
        block()
    }

    private fun audit(adminId: UUID): List<Triple<UUID?, String, String?>> =
        TestDatabase.dataSource.connection.use { conn ->
            conn
                .prepareStatement(
                    "SELECT actor_user_id, action, request_id FROM audit_log WHERE entity_type = 'admin' AND entity_id = ?",
                ).use { st ->
                    st.setString(1, adminId.toString())
                    st.executeQuery().use { rs ->
                        buildList {
                            while (rs.next()) {
                                add(
                                    Triple(rs.getObject(1) as UUID?, rs.getString(2), rs.getString(3)),
                                )
                            }
                        }
                    }
                }
        }

    private suspend fun HttpResponse.admin() = ApiJson.decodeFromString<AdminResponse>(bodyAsText())

    private suspend fun HttpResponse.error() = ApiJson.decodeFromString<ErrorResponse>(bodyAsText())
}
