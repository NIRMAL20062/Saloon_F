package com.glide.backend.db

import com.glide.backend.config.DatabaseConfig
import com.zaxxer.hikari.HikariConfig
import com.zaxxer.hikari.HikariDataSource
import org.flywaydb.core.Flyway
import org.flywaydb.core.api.output.MigrateResult
import org.jetbrains.exposed.v1.jdbc.Database
import javax.sql.DataSource

object DatabaseFactory {
    /**
     * All our tables live in this schema, not in `public` (D-042, DF-26). On Supabase, `public` is published through its
     * Data API to anyone with the app's publishable key; `glide` is not, so only our backend reaches the data.
     */
    const val SCHEMA = "glide"

    fun createDataSource(config: DatabaseConfig): HikariDataSource =
        HikariDataSource(
            HikariConfig().apply {
                jdbcUrl = config.jdbcUrl
                username = config.user
                password = config.password
                poolName = "glide-db"
                // Every connection works in our schema, so SQL and Exposed use unqualified names. The setting is
                // committed when the connection opens: with auto-commit off it would otherwise sit in the connection's
                // first transaction, and a rollback of that transaction would undo it for good (BE-024).
                connectionInitSql = "SET search_path TO $SCHEMA"
                isIsolateInternalQueries = true
                maximumPoolSize = MAX_POOL_SIZE
                connectionTimeout = CONNECTION_TIMEOUT_MS
                // Exposed manages transactions itself.
                isAutoCommit = false
            },
        )

    /**
     * Applies pending migrations from `src/main/resources/db/migration`.
     * `clean` is disabled so no environment can ever wipe its database through Flyway.
     */
    fun migrate(dataSource: DataSource): MigrateResult =
        Flyway
            .configure()
            .dataSource(dataSource)
            .schemas(SCHEMA)
            .defaultSchema(SCHEMA)
            .createSchemas(true)
            .locations("classpath:db/migration")
            .cleanDisabled(true)
            .validateMigrationNaming(true)
            .load()
            .migrate()

    fun connectExposed(dataSource: DataSource): Database = Database.connect(dataSource)

    private const val MAX_POOL_SIZE = 10
    private const val CONNECTION_TIMEOUT_MS = 5_000L
}
