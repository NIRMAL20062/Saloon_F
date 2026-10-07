package com.glide.backend.salons

import com.glide.backend.db.TestDatabase
import com.glide.shared.salon.IndianState
import java.sql.Connection
import java.sql.SQLException
import java.util.UUID
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

/**
 * The database rules of V7 (BE-017, D-046, DF-32) on real PostgreSQL, as the backend's limited role. Each test runs in
 * one transaction that is rolled back.
 */
class SalonTablesTest {
    @Test
    fun `a salon is stored as DRAFT, in the transaction for that salon`() =
        inRolledBackTransaction {
            val id = UUID.randomUUID()
            forSalon(id)
            insertSalon(id)

            assertEquals("DRAFT", single("SELECT status FROM salons WHERE id = '$id'"))
        }

    @Test
    fun `every state and union territory of the list is accepted`() =
        inRolledBackTransaction {
            IndianState.entries.forEach { state ->
                val id = UUID.randomUUID()
                forSalon(id)
                insertSalon(id, state = state.name)
            }
        }

    @Test
    fun `the name is 1 to 30 characters, not blank, without spaces around it`() =
        inRolledBackTransaction {
            insertSalonFor(name = "😀".repeat(30)) // characters, not bytes: 30 emoji fit
            assertViolates { insertSalonFor(name = "") }
            assertViolates { insertSalonFor(name = " ") }
            assertViolates { insertSalonFor(name = " Glow") }
            assertViolates { insertSalonFor(name = "a".repeat(31)) }
        }

    @Test
    fun `phone, PIN code, state, type and status follow their formats`() =
        inRolledBackTransaction {
            assertViolates { insertSalonFor(phone = "9876543210") } // without 91
            assertViolates { insertSalonFor(phone = "910123456789") } // can't start with 0 or 1 after 91
            assertViolates { insertSalonFor(pincode = "012345") }
            assertViolates { insertSalonFor(pincode = "56001") }
            assertViolates { insertSalonFor(state = "NARNIA") }
            assertViolates { insertSalonFor(type = "KIDS") }
            assertViolates { insertSalonFor(status = "OPEN") }
            assertViolates { insertSalonFor(line1 = " ") }
            assertViolates { insertSalonFor(city = "x".repeat(51)) }
        }

    @Test
    fun `a rejected salon always has a reason`() =
        inRolledBackTransaction {
            assertViolates { insertSalonFor(status = "REJECTED") }
            insertSalonFor(status = "REJECTED", reason = "The IFSC doesn't match the bank")
        }

    @Test
    fun `a transaction for salon A sees only salon A, and with no salon set sees none and writes none`() =
        inRolledBackTransaction {
            val a = UUID.randomUUID()
            val b = UUID.randomUUID()
            forSalon(a)
            insertSalon(a)
            forSalon(b)
            insertSalon(b)

            forSalon(a)
            assertEquals("$a", single("SELECT string_agg(id::text, ',') FROM salons"))
            assertViolates("42501") { insertSalon(b) } // a row for B written in A's transaction

            execute("SELECT set_config('app.salon_id', '', true)")
            assertEquals("0", single("SELECT count(*) FROM salons"))
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

    /** A new salon in its own salon context. */
    private fun Tx.insertSalonFor(
        name: String = "Glow Studio",
        phone: String = "919000000001",
        line1: String = "12, MG Road",
        city: String = "Bengaluru",
        state: String = "KARNATAKA",
        pincode: String = "560001",
        type: String = "UNISEX",
        status: String = "DRAFT",
        reason: String? = null,
    ) {
        val id = UUID.randomUUID()
        forSalon(id)
        insertSalon(id, name, phone, line1, city, state, pincode, type, status, reason)
    }

    private fun Tx.insertSalon(
        id: UUID,
        name: String = "Glow Studio",
        phone: String = "919000000001",
        line1: String = "12, MG Road",
        city: String = "Bengaluru",
        state: String = "KARNATAKA",
        pincode: String = "560001",
        type: String = "UNISEX",
        status: String = "DRAFT",
        reason: String? = null,
    ) {
        connection
            .prepareStatement(
                """
                INSERT INTO salons (id, name, phone, address_line1, address_area, city, state, pincode, type, status,
                                    rejection_reason)
                VALUES (?, ?, ?, ?, 'Indiranagar', ?, ?, ?, ?, ?, ?)
                """.trimIndent(),
            ).use {
                it.setObject(1, id)
                listOf(name, phone, line1, city, state, pincode, type, status, reason)
                    .forEachIndexed { i, value -> it.setString(i + 2, value) }
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

    /** Runs [block] in a savepoint and checks PostgreSQL refuses it ([sqlState]: check_violation by default). */
    private fun Tx.assertViolates(
        sqlState: String = CHECK_VIOLATION,
        block: () -> Unit,
    ) {
        val savepoint = connection.setSavepoint()
        val error = assertFailsWith<SQLException> { block() }
        connection.rollback(savepoint)
        assertEquals(sqlState, error.sqlState, error.message)
    }

    private companion object {
        const val CHECK_VIOLATION = "23514"
    }
}
