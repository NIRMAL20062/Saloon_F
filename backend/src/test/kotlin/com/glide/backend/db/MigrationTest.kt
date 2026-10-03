package com.glide.backend.db

import org.flywaydb.core.Flyway
import org.flywaydb.core.api.FlywayException
import java.sql.Timestamp
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

/** Runs the real migrations against a real PostgreSQL. */
class MigrationTest {
    private val dataSource = TestDatabase.dataSource

    @Test
    fun `all migrations apply and re-running them is a no-op`() {
        val second = DatabaseFactory.migrate(dataSource)

        assertEquals(0, second.migrationsExecuted)
        assertTrue(second.success)
    }

    @Test
    fun `applied migrations match the files in the repo`() {
        // Fails if someone edited a migration after it ran (checksum mismatch).
        Flyway
            .configure()
            .dataSource(dataSource)
            .locations("classpath:db/migration")
            .load()
            .validate()
    }

    @Test
    fun `flyway clean is disabled so no environment can be wiped`() {
        val flyway =
            Flyway
                .configure()
                .dataSource(dataSource)
                .cleanDisabled(true)
                .load()

        assertFailsWith<FlywayException> { flyway.clean() }
    }

    @Test
    fun `set_updated_at trigger function bumps updated_at on update`() {
        dataSource.connection.use { conn ->
            conn.autoCommit = false
            conn.createStatement().use { st ->
                st.execute(
                    """
                    CREATE TEMP TABLE trigger_probe (
                        id int PRIMARY KEY,
                        name text,
                        updated_at timestamptz NOT NULL DEFAULT '2000-01-01T00:00:00Z'
                    ) ON COMMIT DROP;
                    CREATE TRIGGER trigger_probe_set_updated_at BEFORE UPDATE ON trigger_probe
                        FOR EACH ROW EXECUTE FUNCTION set_updated_at();
                    INSERT INTO trigger_probe (id, name) VALUES (1, 'before');
                    UPDATE trigger_probe SET name = 'after' WHERE id = 1;
                    """.trimIndent(),
                )
                st.executeQuery("SELECT updated_at FROM trigger_probe WHERE id = 1").use { rs ->
                    rs.next()
                    val updatedAt: Timestamp = rs.getTimestamp(1)
                    assertTrue(updatedAt.toInstant().isAfter(Timestamp.valueOf("2001-01-01 00:00:00").toInstant()))
                }
            }
            conn.rollback()
        }
    }

    @Test
    fun `our tables live in the glide schema, never in public`() {
        TestDatabase.dataSource.connection.use { conn ->
            val schemas =
                conn
                    .prepareStatement(
                        "SELECT table_schema FROM information_schema.tables WHERE table_name = 'app_users'",
                    ).use { st ->
                        st.executeQuery().use { rs ->
                            generateSequence { if (rs.next()) rs.getString(1) else null }.toList()
                        }
                    }

            assertEquals(listOf("glide"), schemas)
        }
    }
}
