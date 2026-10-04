package com.glide.backend.db

import java.sql.Connection
import java.sql.SQLException
import java.util.UUID
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

/**
 * The database rules of V5 (BE-020) on real PostgreSQL. Each test runs in one transaction that is rolled back,
 * so it starts from an empty `admins` table and leaves nothing behind.
 */
class AdminTablesTest {
    @Test
    fun `the first admin and an invited admin can be stored`() =
        inRolledBackTransaction {
            val first = insertAdmin(email = "first@glide.test", invitedBy = null)
            insertAdmin(email = "second@glide.test", invitedBy = first)

            assertEquals(2, count("SELECT count(*) FROM admins"))
        }

    @Test
    fun `there can be only one admin without an inviter`() =
        inRolledBackTransaction {
            insertAdmin(email = "first@glide.test", invitedBy = null)

            assertViolates("unique_violation") { insertAdmin(email = "another@glide.test", invitedBy = null) }
        }

    @Test
    fun `an email is an admin at most once, and must be lower case and look like an email`() =
        inRolledBackTransaction {
            val first = insertAdmin(email = "first@glide.test", invitedBy = null)

            assertViolates("unique_violation") { insertAdmin(email = "first@glide.test", invitedBy = first) }
            assertViolates("check_violation") { insertAdmin(email = "Upper@glide.test", invitedBy = first) }
            assertViolates("check_violation") { insertAdmin(email = "not-an-email", invitedBy = first) }
            assertViolates("check_violation") { insertAdmin(email = "a".repeat(250) + "@x.in", invitedBy = first) }
        }

    @Test
    fun `the inviter must be an admin`() =
        inRolledBackTransaction {
            assertViolates("foreign_key_violation") {
                insertAdmin(email = "b@glide.test", invitedBy = UUID.randomUUID())
            }
        }

    @Test
    fun `status is INVITED or ACTIVE, and ACTIVE always has an activation time`() =
        inRolledBackTransaction {
            val first = insertAdmin(email = "first@glide.test", invitedBy = null)

            assertViolates("check_violation") { execute("UPDATE admins SET status = 'BOSS' WHERE user_id = '$first'") }
            assertViolates(
                "check_violation",
            ) { execute("UPDATE admins SET status = 'ACTIVE' WHERE user_id = '$first'") }
            execute("UPDATE admins SET status = 'ACTIVE', activated_at = now() WHERE user_id = '$first'")
        }

    @Test
    fun `audit rows can be added but never changed or deleted`() =
        inRolledBackTransaction {
            val id = UUID.randomUUID()
            execute(
                """
                INSERT INTO audit_log (id, actor_user_id, action, entity_type, entity_id, after)
                VALUES ('$id', NULL, 'ADMIN_INVITED', 'admin', 'x', '{"email":"b@glide.test"}')
                """,
            )

            assertViolates("insufficient_privilege") {
                execute("UPDATE audit_log SET action = 'OTHER' WHERE id = '$id'")
            }
            assertViolates("insufficient_privilege") { execute("DELETE FROM audit_log WHERE id = '$id'") }
            assertViolates("insufficient_privilege") { execute("TRUNCATE audit_log") }
        }

    @Test
    fun `audit actions and entity types follow the naming rules`() =
        inRolledBackTransaction {
            assertViolates("check_violation") {
                execute("INSERT INTO audit_log (action, entity_type, entity_id) VALUES ('invited', 'admin', 'x')")
            }
            assertViolates("check_violation") {
                execute("INSERT INTO audit_log (action, entity_type, entity_id) VALUES ('ADMIN_INVITED', 'Admin', 'x')")
            }
        }

    private class Tx(
        val connection: Connection,
    )

    private fun inRolledBackTransaction(block: Tx.() -> Unit) {
        TestDatabase.dataSource.connection.use { connection ->
            connection.autoCommit = false
            try {
                Tx(connection).apply {
                    execute("DELETE FROM admins")
                    block()
                }
            } finally {
                connection.rollback()
            }
        }
    }

    private fun Tx.execute(sql: String) {
        connection.createStatement().use { it.execute(sql.trimIndent()) }
    }

    private fun Tx.count(sql: String): Int =
        connection.createStatement().use { st ->
            st.executeQuery(sql).use {
                it.next()
                it.getInt(1)
            }
        }

    private fun Tx.insertAdmin(
        email: String,
        invitedBy: UUID?,
    ): UUID {
        val id = UUID.randomUUID()
        connection.prepareStatement("INSERT INTO admins (user_id, email, invited_by) VALUES (?, ?, ?)").use {
            it.setObject(1, id)
            it.setString(2, email)
            it.setObject(3, invitedBy)
            it.executeUpdate()
        }
        return id
    }

    /** Runs [block] inside a savepoint and checks it fails with the PostgreSQL error [condition]; the transaction goes on. */
    private fun Tx.assertViolates(
        condition: String,
        block: () -> Unit,
    ) {
        val savepoint = connection.setSavepoint()
        val error = assertFailsWith<SQLException> { block() }
        connection.rollback(savepoint)
        assertEquals(SQL_STATES.getValue(condition), error.sqlState, "expected $condition, got: ${error.message}")
    }

    private companion object {
        val SQL_STATES =
            mapOf(
                "unique_violation" to "23505",
                "check_violation" to "23514",
                "foreign_key_violation" to "23503",
                "insufficient_privilege" to "42501",
            )
    }
}
