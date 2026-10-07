package com.glide.backend.salons

import com.glide.backend.TestTokens
import com.glide.backend.db.ExposedTransactor
import com.glide.backend.db.TestDatabase
import com.glide.backend.fakeDependencies
import com.glide.backend.module
import com.glide.backend.testConfig
import com.glide.backend.users.ExposedUserRepository
import com.glide.shared.api.ApiJson
import com.glide.shared.api.ApiRoutes
import com.glide.shared.error.ErrorCodes
import com.glide.shared.error.ErrorResponse
import com.glide.shared.salon.IndianState
import com.glide.shared.salon.MySalonResponse
import com.glide.shared.salon.SalonAddress
import com.glide.shared.salon.SalonErrorCodes
import com.glide.shared.salon.SalonResponse
import com.glide.shared.salon.SalonRole
import com.glide.shared.salon.SalonStatus
import com.glide.shared.salon.SalonType
import io.ktor.client.request.bearerAuth
import io.ktor.client.request.get
import io.ktor.client.request.post
import io.ktor.client.request.put
import io.ktor.client.request.setBody
import io.ktor.client.statement.HttpResponse
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.HttpStatusCode
import io.ktor.http.contentType
import io.ktor.server.testing.ApplicationTestBuilder
import io.ktor.server.testing.testApplication
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import java.sql.DriverManager
import java.util.UUID
import kotlin.random.Random
import kotlin.test.Test
import kotlin.test.assertEquals

/** BE-017 on real PostgreSQL, as the backend's limited role with row-level security (BE-031). */
class SalonRoutesTest {
    private val profile =
        """
        {"name":"  Glow Studio ","address":{"line1":"12, MG Road","area":"Indiranagar","landmark":"Near the metro",
         "city":"Bengaluru","state":"KARNATAKA","pincode":"560038"},"type":"UNISEX"}
        """.trimIndent()

    @Test
    fun `a salon-side person creates their salon and becomes its owner`() =
        withApp {
            val owner = salonPerson()

            val response = post(owner, profile)

            assertEquals(HttpStatusCode.Created, response.status)
            val body = ApiJson.decodeFromString<MySalonResponse>(response.bodyAsText())
            val expected =
                MySalonResponse(
                    SalonResponse(
                        id = body.salon.id,
                        name = "Glow Studio",
                        phone = owner.phone, // the owner's login number by default (D-046)
                        address =
                            SalonAddress(
                                "12, MG Road",
                                "Indiranagar",
                                "Near the metro",
                                "Bengaluru",
                                IndianState.KARNATAKA,
                                "560038",
                            ),
                        type = SalonType.UNISEX,
                        status = SalonStatus.DRAFT,
                    ),
                    SalonRole.OWNER,
                )
            assertEquals(expected, body)
            // What the database holds, read as the owner login (no row-level security).
            assertEquals(
                "Glow Studio|DRAFT",
                db("SELECT name || '|' || status FROM glide.salons WHERE id = '${body.salon.id}'"),
            )
            assertEquals(
                "${owner.id}|${owner.phone}|OWNER|ACTIVE",
                db(
                    "SELECT user_id || '|' || phone || '|' || role || '|' || status FROM glide.salon_members WHERE salon_id = '${body.salon.id}'",
                ),
            )
            assertEquals(body, mine(owner))
        }

    @Test
    fun `the salon's phone can be given in the usual Indian ways`() =
        withApp {
            listOf(
                "+91 98765 43210" to "919876543210",
                "080-2345 6789" to "918023456789",
                "09876543210" to "919876543210",
            ).forEach { (typed, stored) ->
                val response = post(salonPerson(), profile.replace("\"type\"", "\"phone\":\"$typed\",\"type\""))
                assertEquals(
                    stored,
                    ApiJson.decodeFromString<MySalonResponse>(response.bodyAsText()).salon.phone,
                    typed,
                )
            }
        }

    @Test
    fun `each field is checked`() =
        withApp {
            val person = salonPerson()
            listOf(
                profile.replace("  Glow Studio ", "   ") to SalonErrorCodes.INVALID_SALON_NAME,
                profile.replace("  Glow Studio ", "a".repeat(31)) to SalonErrorCodes.INVALID_SALON_NAME,
                profile.replace("\"type\"", "\"phone\":\"12345\",\"type\"") to SalonErrorCodes.INVALID_SALON_PHONE,
                profile.replace("\"type\"", "\"phone\":\"+1 415 555 0100\",\"type\"") to
                    SalonErrorCodes.INVALID_SALON_PHONE,
                profile.replace("12, MG Road", " ") to SalonErrorCodes.INVALID_ADDRESS,
                profile.replace("Indiranagar", "") to SalonErrorCodes.INVALID_ADDRESS,
                profile.replace("Bengaluru", "B".repeat(51)) to SalonErrorCodes.INVALID_ADDRESS,
                profile.replace("560038", "012345") to SalonErrorCodes.INVALID_ADDRESS,
                profile.replace("KARNATAKA", "NARNIA") to ErrorCodes.BAD_REQUEST,
                profile.replace("UNISEX", "KIDS") to ErrorCodes.BAD_REQUEST,
            ).forEach { (body, code) ->
                val response = post(person, body)
                assertEquals(HttpStatusCode.BadRequest, response.status, body)
                assertEquals(code, error(response), body)
            }
            assertEquals(HttpStatusCode.NotFound, get(person).status) // nothing was saved
        }

    @Test
    fun `30 emoji fit in a name, counted the way the database counts characters`() =
        withApp {
            val response = post(salonPerson(), profile.replace("  Glow Studio ", "💇".repeat(30)))

            assertEquals(HttpStatusCode.Created, response.status)
        }

    @Test
    fun `only the salon side can create a salon`() =
        withApp {
            val customer = person()
            setSide(customer, "CUSTOMER")
            val undecided = person()

            listOf(customer, undecided).forEach {
                val response = post(it, profile)
                assertEquals(HttpStatusCode.Forbidden, response.status)
                assertEquals(SalonErrorCodes.NOT_SALON_SIDE, error(response))
            }
        }

    @Test
    fun `one salon per person, even when two requests race`() =
        withApp {
            val owner = salonPerson()

            val statuses = coroutineScope { (1..4).map { async { post(owner, profile).status } }.awaitAll() }

            assertEquals(1, statuses.count { it == HttpStatusCode.Created }, statuses.toString())
            assertEquals(3, statuses.count { it == HttpStatusCode.Conflict }, statuses.toString())
            assertEquals("1", db("SELECT count(*) FROM glide.salon_members WHERE user_id = '${owner.id}'"))
        }

    @Test
    fun `without a salon, reading or editing it is 404`() =
        withApp {
            val person = salonPerson()

            assertEquals(SalonErrorCodes.NO_SALON, error(get(person)))
            assertEquals(SalonErrorCodes.NO_SALON, error(put(person, profile)))
        }

    @Test
    fun `the owner edits the profile until the salon is live`() =
        withApp {
            val owner = salonPerson()
            val id = created(owner).salon.id

            val response =
                put(
                    owner,
                    profile
                        .replace(
                            "Glow Studio",
                            "Glow Salon",
                        ).replace("\"type\"", "\"phone\":\"9123456780\",\"type\""),
                )

            assertEquals(HttpStatusCode.OK, response.status)
            val saved = ApiJson.decodeFromString<MySalonResponse>(response.bodyAsText()).salon
            assertEquals("Glow Salon" to "919123456780", saved.name to saved.phone)
            assertEquals(
                "Glow Salon|919123456780",
                db("SELECT name || '|' || phone FROM glide.salons WHERE id = '$id'"),
            )

            db("UPDATE glide.salons SET status = 'LIVE' WHERE id = '$id' RETURNING status")
            val live = put(owner, profile)
            assertEquals(HttpStatusCode.Conflict, live.status)
            assertEquals(SalonErrorCodes.SALON_NOT_EDITABLE, error(live))
            assertEquals("Glow Salon", db("SELECT name FROM glide.salons WHERE id = '$id'"))
        }

    @Test
    fun `staff read their salon but can't edit it`() =
        withApp {
            val owner = salonPerson()
            val salon = created(owner).salon
            val staff = person()
            db(
                """
                INSERT INTO glide.app_users (id, phone, side) VALUES ('${staff.id}', '${staff.phone}', 'SALON');
                INSERT INTO glide.salon_members (salon_id, user_id, phone, role)
                VALUES ('${salon.id}', '${staff.id}', '${staff.phone}', 'STAFF') RETURNING role
                """.trimIndent(),
            )

            assertEquals(MySalonResponse(salon, SalonRole.STAFF), mine(staff))
            val edit = put(staff, profile.replace("Glow Studio", "Taken Over"))
            assertEquals(HttpStatusCode.Forbidden, edit.status)
            assertEquals(SalonErrorCodes.NOT_OWNER, error(edit))
            assertEquals("Glow Studio", db("SELECT name FROM glide.salons WHERE id = '${salon.id}'"))
        }

    @Test
    fun `two salons never see or change each other`() =
        withApp {
            val ownerA = salonPerson()
            val ownerB = salonPerson()
            val a = created(ownerA).salon
            val b = created(ownerB, profile.replace("Glow Studio", "Salon B")).salon

            assertEquals(a, mine(ownerA).salon)
            assertEquals(b, mine(ownerB).salon)
            put(ownerB, profile.replace("Glow Studio", "B edited"))
            assertEquals("Glow Studio", db("SELECT name FROM glide.salons WHERE id = '${a.id}'"))
            assertEquals("B edited", db("SELECT name FROM glide.salons WHERE id = '${b.id}'"))
        }

    @Test
    fun `no login is 401 on every salon route`() =
        withApp {
            assertEquals(HttpStatusCode.Unauthorized, client.post(ApiRoutes.SALON_SALONS).status)
            assertEquals(HttpStatusCode.Unauthorized, client.get(ApiRoutes.SALON_ME).status)
            assertEquals(HttpStatusCode.Unauthorized, client.put(ApiRoutes.SALON_PROFILE).status)
        }

    // --- helpers

    private data class Person(
        val id: UUID,
        val phone: String,
    ) {
        val token = TestTokens.token(userId = id, phone = phone)
    }

    private fun person() = Person(UUID.randomUUID(), "919" + Random.nextLong(100_000_000, 1_000_000_000))

    private suspend fun ApplicationTestBuilder.salonPerson(): Person = person().also { setSide(it, "SALON") }

    private suspend fun ApplicationTestBuilder.setSide(
        person: Person,
        side: String,
    ) {
        val response =
            client.put(ApiRoutes.ME_SIDE) {
                bearerAuth(person.token)
                contentType(ContentType.Application.Json)
                setBody("""{"side":"$side"}""")
            }
        assertEquals(HttpStatusCode.OK, response.status)
    }

    private suspend fun ApplicationTestBuilder.post(
        person: Person,
        body: String,
    ): HttpResponse =
        client.post(ApiRoutes.SALON_SALONS) {
            bearerAuth(person.token)
            contentType(ContentType.Application.Json)
            setBody(body)
        }

    private suspend fun ApplicationTestBuilder.put(
        person: Person,
        body: String,
    ): HttpResponse =
        client.put(ApiRoutes.SALON_PROFILE) {
            bearerAuth(person.token)
            contentType(ContentType.Application.Json)
            setBody(body)
        }

    private suspend fun ApplicationTestBuilder.get(person: Person): HttpResponse =
        client.get(ApiRoutes.SALON_ME) {
            bearerAuth(person.token)
        }

    private suspend fun ApplicationTestBuilder.mine(person: Person): MySalonResponse {
        val response = get(person)
        assertEquals(HttpStatusCode.OK, response.status)
        return ApiJson.decodeFromString(response.bodyAsText())
    }

    private suspend fun ApplicationTestBuilder.created(
        owner: Person,
        body: String = profile,
    ): MySalonResponse {
        val response = post(owner, body)
        assertEquals(HttpStatusCode.Created, response.status, response.bodyAsText())
        return ApiJson.decodeFromString(response.bodyAsText())
    }

    private suspend fun error(response: HttpResponse): String =
        ApiJson.decodeFromString<ErrorResponse>(response.bodyAsText()).error.code

    /** One value from the database through the owner login (bypasses row-level security, like a look in the dashboard). */
    private fun db(sql: String): String? {
        val config = TestDatabase.config
        return DriverManager.getConnection(config.jdbcUrl, config.user, config.password).use { conn ->
            conn.createStatement().use { st ->
                if (!st.execute(sql)) {
                    return@use null
                }
                st.resultSet.use { if (it.next()) it.getString(1) else null }
            }
        }
    }

    private fun withApp(block: suspend ApplicationTestBuilder.() -> Unit) =
        testApplication {
            TestDatabase.dataSource // migrated
            val deps =
                fakeDependencies().copy(
                    transactor = ExposedTransactor(TestDatabase.exposed),
                    users = ExposedUserRepository(),
                    salons = ExposedSalonRepository(),
                )
            application { module(testConfig(database = TestDatabase.config), deps) }
            block()
        }
}
