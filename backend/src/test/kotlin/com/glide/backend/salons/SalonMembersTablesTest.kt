package com.glide.backend.salons

import com.glide.backend.db.TestDatabase
import java.sql.Connection
import java.sql.SQLException
import java.util.UUID
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

/**
 * The database rules of V8 (BE-017, D-035, D-039) on real PostgreSQL, as the backend's limited role. Each test runs in
 * one transaction that is rolled back.
 */
class SalonMembersTablesTest {
    @Test
    fun `an owner is stored in their salon's transaction`() =
        inRolledBackTransaction {
            val salon = newSalon()
            addMember(salon, newUser(), "919000000101", "OWNER")

            assertEquals("1", single("SELECT count(*) FROM salon_members"))
        }

    @Test
    fun `one salon per person and per phone, even across salons the transaction can't see`() =
        inRolledBackTransaction {
            val user = newUser()
            addMember(newSalon(), user, "919000000102", "OWNER")

            val other = newSalon()
            assertViolates(UNIQUE_VIOLATION) { addMember(other, user, "919000000103", "OWNER") }
            assertViolates(UNIQUE_VIOLATION) { addMember(other, null, "919000000102", "STAFF") }
        }

    @Test
    fun `a removed membership doesn't count, so the person can join again`() =
        inRolledBackTransaction {
            val user = newUser()
            val first = newSalon()
            addMember(first, user, "919000000104", "OWNER")
            execute("UPDATE salon_members SET status = 'REMOVED' WHERE salon_id = '$first'")

            addMember(newSalon(), user, "919000000104", "OWNER")
        }

    @Test
    fun `one owner per salon, and an owner always has a login`() =
        inRolledBackTransaction {
            val salon = newSalon()
            addMember(salon, newUser(), "919000000105", "OWNER")

            assertViolates(UNIQUE_VIOLATION) { addMember(salon, newUser(), "919000000106", "OWNER") }
            assertViolates(CHECK_VIOLATION) { addMember(newSalon(), null, "919000000107", "OWNER") }
            assertViolates(CHECK_VIOLATION) { addMember(salon, null, "919000000108", "MANAGER") }
            assertViolates(CHECK_VIOLATION) { addMember(salon, null, "+919000000109", "STAFF") }
        }

    @Test
    fun `a person sees their own membership without a salon set, and nobody else's`() =
        inRolledBackTransaction {
            val me = newUser()
            val someoneElse = newUser()
            addMember(newSalon(), me, "919000000110", "OWNER")
            addMember(newSalon(), someoneElse, "919000000111", "OWNER")

            execute("SELECT set_config('app.salon_id', '', true)")
            execute("SELECT set_config('app.user_id', '$me', true)")

            assertEquals("919000000110", single("SELECT string_agg(phone, ',') FROM salon_members"))
        }

    @Test
    fun `salon A's transaction sees only A's members and can't add one to B`() =
        inRolledBackTransaction {
            val a = newSalon()
            addMember(a, newUser(), "919000000112", "OWNER")
            val b = newSalon()
            addMember(b, newUser(), "919000000113", "OWNER")

            forSalon(a)
            assertEquals("919000000112", single("SELECT string_agg(phone, ',') FROM salon_members"))
            assertViolates(INSUFFICIENT_PRIVILEGE) { insertMember(b, null, "919000000114", "STAFF") }
        }

    private class Tx(
        val connection: Connection,
    )

    private fun inRolledBackTransaction(block: Tx.() -> Unit) {
        TestDatabase.dataSource.connection.use { connection ->
            try {
                Tx(connection).block()
            } finally {
                connection.rollback()
            }
        }
    }

    private fun Tx.forSalon(id: UUID) = execute("SELECT set_config('app.salon_id', '$id', true)")

    private fun Tx.newUser(): UUID =
        UUID.randomUUID().also { id ->
            execute("INSERT INTO app_users (id) VALUES ('$id')")
        }

    /** A new DRAFT salon; the transaction is then for it. */
    private fun Tx.newSalon(): UUID =
        UUID.randomUUID().also { id ->
            forSalon(id)
            execute(
                """
                INSERT INTO salons (id, name, phone, address_line1, address_area, city, state, pincode, type)
                VALUES ('$id', 'Glow', '919000000001', '12, MG Road', 'Indiranagar', 'Bengaluru', 'KARNATAKA', '560001', 'UNISEX')
                """.trimIndent(),
            )
        }

    /** Adds a member in its own salon's transaction. */
    private fun Tx.addMember(
        salon: UUID,
        user: UUID?,
        phone: String,
        role: String,
    ) {
        forSalon(salon)
        insertMember(salon, user, phone, role)
    }

    private fun Tx.insertMember(
        salon: UUID,
        user: UUID?,
        phone: String,
        role: String,
    ) {
        connection
            .prepareStatement(
                "INSERT INTO salon_members (salon_id, user_id, phone, role) VALUES (?, ?, ?, ?)",
            ).use {
                it.setObject(1, salon)
                it.setObject(2, user)
                it.setString(3, phone)
                it.setString(4, role)
                it.executeUpdate()
            }
    }

    private fun Tx.execute(sql: String) {
        connection.createStatement().use { it.execute(sql) }
    }

    private fun Tx.single(sql: String): String =
        connection.createStatement().use { st ->
            st.executeQuery(sql).use {
                it.next()
                it.getString(1)
            }
        }

    private fun Tx.assertViolates(
        sqlState: String,
        block: () -> Unit,
    ) {
        val savepoint = connection.setSavepoint()
        val error = assertFailsWith<SQLException> { block() }
        connection.rollback(savepoint)
        assertEquals(sqlState, error.sqlState, error.message)
    }

    private companion object {
        const val CHECK_VIOLATION = "23514"
        const val UNIQUE_VIOLATION = "23505"
        const val INSUFFICIENT_PRIVILEGE = "42501"
    }
}
