package com.glide.backend.db

import com.zaxxer.hikari.HikariDataSource
import java.sql.Connection
import java.sql.DriverManager
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * Every pooled connection works in the `glide` schema (DF-26) for its whole life (BE-024): also when its first
 * transaction is rolled back, and after the pool replaces connections that were closed underneath it, as Supabase's
 * pooler does with idle ones. Each test has its own pool, so the shared test pool is never touched.
 */
class ConnectionSchemaTest {
    @Test
    fun `a fresh connection whose first transaction is rolled back still finds the glide tables`() =
        freshPool().use { pool ->
            pool.connection.use { connection ->
                connection.single("SELECT 1")
                connection.rollback()

                assertFindsGlideTables(connection)
            }
        }

    @Test
    fun `connections the pool replaces after they were closed underneath it still find the glide tables`() =
        freshPool().use { pool ->
            // Fill the pool, then close every one of its connections from the database side.
            val closed = holdAll(pool) { connections -> connections.map { it.pid() }.toSet() }
            assertEquals(pool.maximumPoolSize, closed.size)
            val db = TestDatabase.config
            DriverManager.getConnection(db.jdbcUrl, db.user, db.password).use { admin ->
                closed.forEach { pid -> admin.single("SELECT pg_terminate_backend($pid, 5000)") }
            }
            // The pool checks a connection when it hands it out only if it sat unused for 500 ms; idle ones always have.
            Thread.sleep(ALIVE_CHECK_AFTER_MS)

            holdAll(pool) { connections ->
                connections.forEach { connection ->
                    assertTrue(connection.pid() !in closed, "a closed connection came back")
                    connection.rollback()

                    assertFindsGlideTables(connection)
                }
            }
        }

    private fun freshPool(): HikariDataSource {
        TestDatabase.dataSource // the shared pool migrates the database once
        return DatabaseFactory.createDataSource(TestDatabase.config)
    }

    /** Takes every connection of [pool] at once, so [block] sees each of them; gives them back afterwards. */
    private fun <T> holdAll(
        pool: HikariDataSource,
        block: (List<Connection>) -> T,
    ): T {
        val connections = List(pool.maximumPoolSize) { pool.connection }
        try {
            return block(connections)
        } finally {
            connections.forEach { it.close() }
        }
    }

    private fun assertFindsGlideTables(connection: Connection) {
        assertEquals(DatabaseFactory.SCHEMA, connection.single("SELECT current_schema()"))
        // Without the schema this fails with: relation "admins" does not exist.
        connection.single("SELECT count(*) FROM admins")
    }

    private fun Connection.pid(): Int = single("SELECT pg_backend_pid()").toInt()

    private fun Connection.single(sql: String): String =
        createStatement().use { st ->
            st.executeQuery(sql).use {
                it.next()
                it.getString(1)
            }
        }

    private companion object {
        const val ALIVE_CHECK_AFTER_MS = 600L
    }
}
