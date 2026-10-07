package com.glide.backend.db

import kotlinx.coroutines.runBlocking
import org.jetbrains.exposed.v1.jdbc.transactions.TransactionManager
import java.sql.Connection
import java.sql.DriverManager
import java.sql.SQLException
import java.util.UUID
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

/**
 * The backend works as the limited role `glide_app` (BE-031, D-027), and a transaction for one salon sees and writes only
 * that salon's rows. Real PostgreSQL. The probe table stands in for the salon tables that come with BE-017.
 */
class AppRoleTest {
    private val transactor = ExposedTransactor(TestDatabase.exposed)
    private val salonA = UUID.randomUUID()
    private val salonB = UUID.randomUUID()

    @BeforeTest
    fun createProbeTable() {
        TestDatabase.dataSource // migrated, so the role and its default rights exist
        owner { conn ->
            conn.createStatement().use {
                it.execute(
                    """
                    CREATE TABLE glide.rls_probe (salon_id uuid NOT NULL, note text NOT NULL);
                    ALTER TABLE glide.rls_probe ENABLE ROW LEVEL SECURITY;
                    ALTER TABLE glide.rls_probe FORCE ROW LEVEL SECURITY;
                    CREATE POLICY rls_probe_salon ON glide.rls_probe
                        USING (salon_id = nullif(current_setting('app.salon_id', true), '')::uuid);
                    INSERT INTO glide.rls_probe VALUES ('$salonA', 'a1'), ('$salonA', 'a2'), ('$salonB', 'b1');
                    """.trimIndent(),
                )
            }
        }
    }

    @AfterTest
    fun dropProbeTable() {
        owner { conn -> conn.createStatement().use { it.execute("DROP TABLE glide.rls_probe") } }
    }

    @Test
    fun `pooled connections work as glide_app, which is not superuser and can't bypass row-level security`() {
        TestDatabase.dataSource.connection.use { conn ->
            conn.createStatement().use { st ->
                st
                    .executeQuery(
                        "SELECT current_user, rolsuper, rolbypassrls FROM pg_roles WHERE rolname = current_user",
                    ).use {
                        it.next()
                        assertEquals("glide_app", it.getString(1))
                        assertEquals(false, it.getBoolean(2))
                        assertEquals(false, it.getBoolean(3))
                    }
            }
        }
    }

    @Test
    fun `a transaction for salon A sees only salon A's rows`() {
        assertEquals(listOf("a1", "a2"), runBlocking { transactor.transaction(salonA) { notes() } })
        assertEquals(listOf("b1"), runBlocking { transactor.transaction(salonB) { notes() } })
    }

    @Test
    fun `with no salon set, no salon-owned row is visible`() {
        assertEquals(emptyList(), runBlocking { transactor.transaction { notes() } })
    }

    @Test
    fun `salon A can't write a row for salon B`() {
        val error =
            assertFailsWith<SQLException> {
                runBlocking {
                    transactor.transaction(
                        salonA,
                    ) { exec("INSERT INTO rls_probe VALUES ('$salonB', 'sneaky')") }
                }
            }
        assertEquals(INSUFFICIENT_PRIVILEGE, error.sqlState)
    }

    @Test
    fun `the salon setting ends with its transaction`() {
        TestDatabase.dataSource.connection.use { conn ->
            conn.createStatement().use { it.execute("SELECT set_config('app.salon_id', '$salonA', true)") }
            conn.commit()
            conn.createStatement().use { st ->
                st.executeQuery("SELECT coalesce(current_setting('app.salon_id', true), '')").use {
                    it.next()
                    assertEquals("", it.getString(1))
                }
            }
            conn.rollback()
        }
    }

    @Test
    fun `glide_app can't change the audit log or the migration history, nor create tables`() {
        listOf(
            "UPDATE audit_log SET action = 'X' WHERE false",
            "DELETE FROM audit_log WHERE false",
            "TRUNCATE audit_log",
            "DELETE FROM flyway_schema_history WHERE false",
            "CREATE TABLE glide.not_allowed (id int)",
        ).forEach { sql ->
            TestDatabase.dataSource.connection.use { conn ->
                val error = assertFailsWith<SQLException>(sql) { conn.createStatement().use { it.execute(sql) } }
                assertEquals(INSUFFICIENT_PRIVILEGE, error.sqlState, sql)
                conn.rollback()
            }
        }
    }

    private fun notes(): List<String> {
        val notes = mutableListOf<String>()
        TransactionManager.current().exec("SELECT note FROM rls_probe ORDER BY note") { rs ->
            while (rs.next()) notes += rs.getString(1)
        }
        return notes
    }

    private fun exec(sql: String) {
        TransactionManager.current().exec(sql)
    }

    /** The owner login, as Flyway uses it: creates and drops the probe table. */
    private fun owner(block: (Connection) -> Unit) {
        val db = TestDatabase.config
        DriverManager.getConnection(db.jdbcUrl, db.user, db.password).use { conn ->
            block(conn)
            assertTrue(conn.autoCommit)
        }
    }

    private companion object {
        const val INSUFFICIENT_PRIVILEGE = "42501"
    }
}
