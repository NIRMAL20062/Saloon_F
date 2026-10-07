package com.glide.backend.salons

import com.glide.backend.db.TestDatabase
import java.sql.Connection
import java.sql.SQLException
import java.util.UUID
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

/**
 * The database rules of V9 (BE-032, DF-24, DF-33) on real PostgreSQL, as the backend's limited role. Each test runs in
 * one transaction that is rolled back.
 */
class SalonBankDetailsTablesTest {
    @Test
    fun `bank details are stored once per salon, in its own transaction`() =
        inRolledBackTransaction {
            val salon = newSalon()
            insertBank(salon)

            assertEquals("1", single("SELECT count(*) FROM salon_bank_details"))
            assertViolates(UNIQUE_VIOLATION) { insertBank(salon) }
        }

    @Test
    fun `a plain account number can't be stored, only the encrypted form`() =
        inRolledBackTransaction {
            val salon = newSalon()
            assertViolates(CHECK_VIOLATION) { insertBank(salon, encrypted = "123456789012") }
            assertViolates(CHECK_VIOLATION) { insertBank(salon, encrypted = "v1:not base64!") }
            assertViolates(CHECK_VIOLATION) { insertBank(salon, last4 = "12345") }
        }

    @Test
    fun `IFSC has India's shape, in capitals`() =
        inRolledBackTransaction {
            val salon = newSalon()
            listOf("hdfc0001234", "HDFC1001234", "HDFC000123", "HDFC00012345", "HDF00001234").forEach { bad ->
                assertViolates(CHECK_VIOLATION) { insertBank(salon, ifsc = bad) }
            }
            assertViolates(CHECK_VIOLATION) { insertBank(salon, holder = " Asha") }
            insertBank(salon, ifsc = "SBIN0A12B34")
        }

    @Test
    fun `salon A's transaction sees only its own bank details and can't write B's`() =
        inRolledBackTransaction {
            val a = newSalon()
            insertBank(a, holder = "Owner A")
            val b = newSalon()
            insertBank(b, holder = "Owner B")

            forSalon(a)
            assertEquals("Owner A", single("SELECT string_agg(account_holder_name, ',') FROM salon_bank_details"))
            assertViolates(INSUFFICIENT_PRIVILEGE) { insertBankRow(UUID.randomUUID()) }
            execute("UPDATE salon_bank_details SET ifsc = 'HDFC0009999' WHERE salon_id = '$b'") // touches nothing
            forSalon(b)
            assertEquals("HDFC0001234", single("SELECT ifsc FROM salon_bank_details"))

            execute("SELECT set_config('app.salon_id', '', true)")
            assertEquals("0", single("SELECT count(*) FROM salon_bank_details"))
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

    /** Bank details for [salon], in its own transaction. */
    private fun Tx.insertBank(
        salon: UUID,
        holder: String = "Asha Rao",
        encrypted: String = "v1:QUJDREVGR0hJSktMTU5PUA==",
        last4: String = "9012",
        ifsc: String = "HDFC0001234",
    ) {
        forSalon(salon)
        insertBankRow(salon, holder, encrypted, last4, ifsc)
    }

    private fun Tx.insertBankRow(
        salon: UUID,
        holder: String = "Asha Rao",
        encrypted: String = "v1:QUJDREVGR0hJSktMTU5PUA==",
        last4: String = "9012",
        ifsc: String = "HDFC0001234",
    ) {
        connection
            .prepareStatement(
                """
                INSERT INTO salon_bank_details (salon_id, account_holder_name, account_number_encrypted, account_number_last4, ifsc)
                VALUES (?, ?, ?, ?, ?)
                """.trimIndent(),
            ).use {
                it.setObject(1, salon)
                listOf(holder, encrypted, last4, ifsc).forEachIndexed { i, value -> it.setString(i + 2, value) }
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
