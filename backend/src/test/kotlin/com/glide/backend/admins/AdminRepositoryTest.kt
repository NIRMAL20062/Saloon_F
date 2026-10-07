package com.glide.backend.admins

import com.glide.backend.db.TestDatabase
import com.glide.shared.admin.AdminStatus
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.runBlocking
import org.jetbrains.exposed.v1.jdbc.transactions.transaction
import java.util.UUID
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNotNull
import kotlin.test.assertNull

/** Real PostgreSQL (Testcontainers) with the real migrations. */
class AdminRepositoryTest {
    private val repository = ExposedAdminRepository()

    private fun <T> tx(block: () -> T): T = transaction(TestDatabase.exposed) { block() }

    @BeforeTest
    fun emptyAdmins() = resetAdmins()

    @Test
    fun `the first admin is added once, as INVITED, and audit-logged as done by a server command`() =
        runBlocking {
            val id = UUID.randomUUID()

            val admin = tx { repository.addFirst(id, "first@glide.test") }

            assertEquals(Admin(id, "first@glide.test", AdminStatus.INVITED, invitedBy = null), admin)
            assertEquals(admin, tx { repository.find(id) })
            assertEquals(admin, tx { repository.findByEmail("first@glide.test") })
            assertEquals(
                listOf(
                    AuditRow(
                        actor = null,
                        action = "ADMIN_ADDED",
                        after = """{"email": "first@glide.test", "status": "INVITED"}""",
                    ),
                ),
                auditRows(id),
            )
        }

    @Test
    fun `there is only ever one first admin, even when the command races itself`() =
        runBlocking {
            val results =
                (1..5)
                    .map { n ->
                        async(Dispatchers.IO) { tx { repository.addFirst(UUID.randomUUID(), "first$n@glide.test") } }
                    }.awaitAll()

            assertEquals(1, results.count { it != null })
            assertNull(tx { repository.addFirst(UUID.randomUUID(), "later@glide.test") })
        }

    @Test
    fun `an invited admin records who invited them and the request, in the audit log too`() =
        runBlocking {
            val first = tx { repository.addFirst(UUID.randomUUID(), "first@glide.test") }!!
            val id = UUID.randomUUID()

            val invited = tx { repository.addInvited(id, "b@glide.test", first.userId, "req-42") }

            assertEquals(Admin(id, "b@glide.test", AdminStatus.INVITED, first.userId), invited)
            assertEquals(
                listOf(
                    AuditRow(
                        actor = first.userId,
                        action = "ADMIN_INVITED",
                        after = """{"email": "b@glide.test", "status": "INVITED"}""",
                        requestId = "req-42",
                    ),
                ),
                auditRows(id),
            )
        }

    @Test
    fun `the same email or login can't be added twice, and nothing is audit-logged for the refusal`() =
        runBlocking {
            val first = tx { repository.addFirst(UUID.randomUUID(), "first@glide.test") }!!
            val b = UUID.randomUUID()
            tx { repository.addInvited(b, "b@glide.test", first.userId, null) }
            val other = UUID.randomUUID()

            assertNull(tx { repository.addInvited(other, "b@glide.test", first.userId, null) })
            assertNull(tx { repository.addInvited(b, "c@glide.test", first.userId, null) })
            assertEquals(emptyList(), auditRows(other))
            assertEquals(1, auditRows(b).size)
        }

    @Test
    fun `activation turns INVITED into ACTIVE once, stamps the time, and is audit-logged once`() =
        runBlocking {
            val id = UUID.randomUUID()
            tx { repository.addFirst(id, "first@glide.test") }

            val results = (1..5).map { async(Dispatchers.IO) { tx { repository.activate(id, "req-1") } } }.awaitAll()

            results.forEach { assertEquals(AdminStatus.ACTIVE, it.status) }
            assertNotNull(activatedAt(id))
            assertEquals(1, auditRows(id).count { it.action == "ADMIN_ACTIVATED" })
            assertEquals(AdminStatus.ACTIVE, tx { repository.activate(id, "req-2") }.status)
            assertEquals(1, auditRows(id).count { it.action == "ADMIN_ACTIVATED" })
        }

    @Test
    fun `unknown logins and emails are not admins`() =
        runBlocking {
            assertNull(tx { repository.find(UUID.randomUUID()) })
            assertNull(tx { repository.findByEmail("nobody@glide.test") })
        }

    @Test
    fun `an invite and its audit row are kept or dropped together, in the caller's transaction (BE-025)`() {
        val first = tx { repository.addFirst(UUID.randomUUID(), "first@glide.test") }!!
        val id = UUID.randomUUID()

        assertFailsWith<IllegalStateException> {
            tx {
                repository.addInvited(id, "b@glide.test", first.userId, "req-7")
                error("a later step of the same piece of work failed")
            }
        }

        assertNull(tx { repository.find(id) })
        assertEquals(emptyList(), auditRows(id))
    }

    private data class AuditRow(
        val actor: UUID?,
        val action: String,
        val after: String?,
        val requestId: String? = null,
    )

    private fun auditRows(adminId: UUID): List<AuditRow> =
        TestDatabase.dataSource.connection.use { conn ->
            conn
                .prepareStatement(
                    """
                    SELECT actor_user_id, action, after::text, request_id FROM audit_log
                    WHERE entity_type = 'admin' AND entity_id = ? AND salon_id IS NULL ORDER BY created_at
                    """.trimIndent(),
                ).use { st ->
                    st.setString(1, adminId.toString())
                    st.executeQuery().use { rs ->
                        buildList {
                            while (rs.next()) {
                                add(
                                    AuditRow(
                                        rs.getObject(1) as UUID?,
                                        rs.getString(2),
                                        rs.getString(3),
                                        rs.getString(4),
                                    ),
                                )
                            }
                        }
                    }
                }
        }

    private fun activatedAt(id: UUID): Any? =
        TestDatabase.dataSource.connection.use { conn ->
            conn.prepareStatement("SELECT activated_at FROM admins WHERE user_id = ?").use { st ->
                st.setObject(1, id)
                st.executeQuery().use { rs ->
                    rs.next()
                    rs.getObject(1)
                }
            }
        }
}

/** Empties `admins` (the audit log can't be emptied: tests look up their own rows by id). */
fun resetAdmins() {
    TestDatabase.dataSource.connection.use { conn ->
        conn.createStatement().use { it.execute("DELETE FROM admins") }
        conn.commit()
    }
}
