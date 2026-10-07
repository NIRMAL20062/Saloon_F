package com.glide.backend.salons

import com.glide.backend.TEST_BANK_KEY
import com.glide.backend.TestTokens
import com.glide.backend.crypto.FieldCipher
import com.glide.backend.db.ExposedTransactor
import com.glide.backend.db.TestDatabase
import com.glide.backend.fakeDependencies
import com.glide.backend.module
import com.glide.backend.testConfig
import com.glide.backend.users.ExposedUserRepository
import com.glide.shared.api.ApiJson
import com.glide.shared.api.ApiRoutes
import com.glide.shared.error.ErrorResponse
import com.glide.shared.salon.BankDetailsResponse
import com.glide.shared.salon.MySalonResponse
import com.glide.shared.salon.SalonErrorCodes
import com.glide.shared.salon.SalonStatus
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
import java.sql.DriverManager
import java.util.UUID
import kotlin.random.Random
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull

/** BE-032 on real PostgreSQL, as the backend's limited role with row-level security (BE-031). */
class SalonBankRoutesTest {
    private val bank = """{"accountHolderName":" Asha Rao ","accountNumber":"5010 0123 4567 89","ifsc":"hdfc0001234"}"""

    @Test
    fun `the owner saves bank details, gets them back masked, and only the encrypted number is stored`() =
        withApp {
            val owner = owner()

            val response = putBank(owner, bank)

            assertEquals(HttpStatusCode.OK, response.status)
            assertEquals(
                BankDetailsResponse("Asha Rao", "6789", "HDFC0001234"),
                ApiJson.decodeFromString(response.bodyAsText()),
            )
            assertFalse(response.bodyAsText().contains("50100123456789"))
            assertEquals(
                BankDetailsResponse("Asha Rao", "6789", "HDFC0001234"),
                ApiJson.decodeFromString(getBank(owner).bodyAsText()),
            )
            // In the database: never the number in clear; it decrypts with the key and this salon only.
            val stored =
                db(
                    "SELECT account_number_encrypted FROM glide.salon_bank_details WHERE salon_id = '${owner.salonId}'",
                )!!
            assertFalse(stored.contains("0123456789"))
            assertEquals("50100123456789", FieldCipher(TEST_BANK_KEY).decrypt(stored, owner.salonId))
        }

    @Test
    fun `saving again replaces the details`() =
        withApp {
            val owner = owner()
            putBank(owner, bank)

            putBank(owner, """{"accountHolderName":"Asha R","accountNumber":"123456789","ifsc":"SBIN0001111"}""")

            assertEquals(
                BankDetailsResponse("Asha R", "6789", "SBIN0001111"),
                ApiJson.decodeFromString(getBank(owner).bodyAsText()),
            )
            assertEquals("1", db("SELECT count(*) FROM glide.salon_bank_details WHERE salon_id = '${owner.salonId}'"))
        }

    @Test
    fun `each field is checked, and an error never repeats the number`() =
        withApp {
            val owner = owner()
            listOf(
                """{"accountHolderName":"  ","accountNumber":"50100123456789","ifsc":"HDFC0001234"}""" to
                    SalonErrorCodes.INVALID_ACCOUNT_HOLDER,
                """{"accountHolderName":"${"a".repeat(
                    101,
                )}","accountNumber":"50100123456789","ifsc":"HDFC0001234"}""" to
                    SalonErrorCodes.INVALID_ACCOUNT_HOLDER,
                """{"accountHolderName":"Asha","accountNumber":"12345678","ifsc":"HDFC0001234"}""" to
                    SalonErrorCodes.INVALID_ACCOUNT_NUMBER,
                """{"accountHolderName":"Asha","accountNumber":"1234567890123456789","ifsc":"HDFC0001234"}""" to
                    SalonErrorCodes.INVALID_ACCOUNT_NUMBER,
                """{"accountHolderName":"Asha","accountNumber":"50100-123456","ifsc":"HDFC0001234"}""" to
                    SalonErrorCodes.INVALID_ACCOUNT_NUMBER,
                """{"accountHolderName":"Asha","accountNumber":"50100123456789","ifsc":"HDFC1001234"}""" to
                    SalonErrorCodes.INVALID_IFSC,
                """{"accountHolderName":"Asha","accountNumber":"50100123456789","ifsc":"HDFC000123"}""" to
                    SalonErrorCodes.INVALID_IFSC,
            ).forEach { (body, code) ->
                val response = putBank(owner, body)
                assertEquals(HttpStatusCode.BadRequest, response.status, body)
                assertEquals(code, error(response), body)
                assertFalse(response.bodyAsText().contains("50100"), body)
            }
            assertEquals(SalonErrorCodes.NO_BANK_DETAILS, error(getBank(owner))) // nothing was saved
        }

    @Test
    fun `submitting needs bank details, then the salon is under verification, and retrying is safe`() =
        withApp {
            val owner = owner()

            val early = submit(owner)
            assertEquals(HttpStatusCode.Conflict, early.status)
            assertEquals(SalonErrorCodes.NO_BANK_DETAILS, error(early))

            putBank(owner, bank)
            val submitted = submit(owner)
            assertEquals(HttpStatusCode.OK, submitted.status)
            assertEquals(
                SalonStatus.UNDER_VERIFICATION,
                ApiJson.decodeFromString<MySalonResponse>(submitted.bodyAsText()).salon.status,
            )
            assertEquals("UNDER_VERIFICATION", db("SELECT status FROM glide.salons WHERE id = '${owner.salonId}'"))
            assertEquals(HttpStatusCode.OK, submit(owner).status) // again: same answer
            // Details can still be changed while under verification (PRODUCT §6.1).
            assertEquals(HttpStatusCode.OK, putBank(owner, bank).status)
        }

    @Test
    fun `a rejected salon is fixed and submitted again, and the old reason goes`() =
        withApp {
            val owner = owner()
            putBank(owner, bank)
            db(
                "UPDATE glide.salons SET status = 'REJECTED', rejection_reason = 'IFSC doesn''t match' WHERE id = '${owner.salonId}' RETURNING 1",
            )

            val resubmitted = ApiJson.decodeFromString<MySalonResponse>(submit(owner).bodyAsText()).salon

            assertEquals(SalonStatus.UNDER_VERIFICATION, resubmitted.status)
            assertNull(resubmitted.rejectionReason)
        }

    @Test
    fun `a live salon's bank details can't be changed here, nor can it be submitted`() =
        withApp {
            val owner = owner()
            putBank(owner, bank)
            db("UPDATE glide.salons SET status = 'LIVE' WHERE id = '${owner.salonId}' RETURNING 1")

            assertEquals(SalonErrorCodes.SALON_NOT_EDITABLE, error(putBank(owner, bank)))
            assertEquals(SalonErrorCodes.SALON_NOT_EDITABLE, error(submit(owner)))
            assertEquals(HttpStatusCode.OK, getBank(owner).status) // still readable, masked
        }

    @Test
    fun `staff can't see, change or submit the bank details`() =
        withApp {
            val owner = owner()
            putBank(owner, bank)
            val staff = person()
            db(
                """
                INSERT INTO glide.app_users (id, phone, side) VALUES ('${staff.id}', '${staff.phone}', 'SALON');
                INSERT INTO glide.salon_members (salon_id, user_id, phone, role)
                VALUES ('${owner.salonId}', '${staff.id}', '${staff.phone}', 'STAFF') RETURNING 1
                """.trimIndent(),
            )

            listOf(getBank(staff), putBank(staff, bank), submit(staff)).forEach {
                assertEquals(HttpStatusCode.Forbidden, it.status)
                assertEquals(SalonErrorCodes.NOT_OWNER, error(it))
            }
            assertEquals("DRAFT", db("SELECT status FROM glide.salons WHERE id = '${owner.salonId}'"))
        }

    @Test
    fun `two salons never see or change each other's bank details`() =
        withApp {
            val a = owner()
            val b = owner()
            putBank(a, bank)
            putBank(b, """{"accountHolderName":"Owner B","accountNumber":"999988887777","ifsc":"ICIC0002222"}""")

            assertEquals(
                "Asha Rao",
                ApiJson.decodeFromString<BankDetailsResponse>(getBank(a).bodyAsText()).accountHolderName,
            )
            assertEquals(
                "Owner B",
                ApiJson.decodeFromString<BankDetailsResponse>(getBank(b).bodyAsText()).accountHolderName,
            )
            assertEquals("HDFC0001234", db("SELECT ifsc FROM glide.salon_bank_details WHERE salon_id = '${a.salonId}'"))
        }

    @Test
    fun `without the encryption key nothing is saved, and reading still works`() =
        withApp(bankKeyed = false) {
            val owner = owner()

            val response = putBank(owner, bank)

            assertEquals(HttpStatusCode.ServiceUnavailable, response.status)
            assertEquals(SalonErrorCodes.BANK_DETAILS_UNAVAILABLE, error(response))
            assertEquals(SalonErrorCodes.NO_BANK_DETAILS, error(getBank(owner)))
        }

    @Test
    fun `without a salon it's 404, and without a login 401`() =
        withApp {
            val person = person()
            assertEquals(SalonErrorCodes.NO_SALON, error(getBank(person)))
            assertEquals(SalonErrorCodes.NO_SALON, error(putBank(person, bank)))
            assertEquals(SalonErrorCodes.NO_SALON, error(submit(person)))
            assertEquals(HttpStatusCode.Unauthorized, client.get(ApiRoutes.SALON_BANK_DETAILS).status)
            assertEquals(HttpStatusCode.Unauthorized, client.put(ApiRoutes.SALON_BANK_DETAILS).status)
            assertEquals(HttpStatusCode.Unauthorized, client.post(ApiRoutes.SALON_SUBMIT).status)
        }

    // --- helpers

    private data class Person(
        val id: UUID,
        val phone: String,
        val salonId: String = "",
    ) {
        val token = TestTokens.token(userId = id, phone = phone)
    }

    private fun person() = Person(UUID.randomUUID(), "919" + Random.nextLong(100_000_000, 1_000_000_000))

    /** A salon-side person who has created a DRAFT salon. */
    private suspend fun ApplicationTestBuilder.owner(): Person {
        val p = person()
        client.put(ApiRoutes.ME_SIDE) {
            bearerAuth(p.token)
            contentType(ContentType.Application.Json)
            setBody("""{"side":"SALON"}""")
        }
        val created =
            client.post(ApiRoutes.SALON_SALONS) {
                bearerAuth(p.token)
                contentType(ContentType.Application.Json)
                setBody(
                    """
                    {"name":"Glow","address":{"line1":"1 Road","area":"Area","city":"Pune","state":"MAHARASHTRA","pincode":"411001"},
                     "type":"UNISEX"}
                    """.trimIndent(),
                )
            }
        assertEquals(HttpStatusCode.Created, created.status, created.bodyAsText())
        return p.copy(salonId = ApiJson.decodeFromString<MySalonResponse>(created.bodyAsText()).salon.id)
    }

    private suspend fun ApplicationTestBuilder.putBank(
        person: Person,
        body: String,
    ): HttpResponse =
        client.put(ApiRoutes.SALON_BANK_DETAILS) {
            bearerAuth(person.token)
            contentType(ContentType.Application.Json)
            setBody(body)
        }

    private suspend fun ApplicationTestBuilder.getBank(person: Person): HttpResponse =
        client.get(ApiRoutes.SALON_BANK_DETAILS) { bearerAuth(person.token) }

    private suspend fun ApplicationTestBuilder.submit(person: Person): HttpResponse =
        client.post(ApiRoutes.SALON_SUBMIT) { bearerAuth(person.token) }

    private suspend fun error(response: HttpResponse): String =
        ApiJson.decodeFromString<ErrorResponse>(response.bodyAsText()).error.code

    /** One value through the owner login (bypasses row-level security, like a look in the dashboard). */
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

    private fun withApp(
        bankKeyed: Boolean = true,
        block: suspend ApplicationTestBuilder.() -> Unit,
    ) = testApplication {
        TestDatabase.dataSource // migrated
        val deps =
            fakeDependencies().copy(
                transactor = ExposedTransactor(TestDatabase.exposed),
                users = ExposedUserRepository(),
                salons = ExposedSalonRepository(),
            )
        val config = testConfig(database = TestDatabase.config, bankDetailsKey = if (bankKeyed) TEST_BANK_KEY else null)
        application { module(config, deps) }
        block()
    }
}
