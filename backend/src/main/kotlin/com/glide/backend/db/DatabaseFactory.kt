package com.glide.backend.db

import com.glide.backend.config.DatabaseConfig
import com.zaxxer.hikari.HikariConfig
import com.zaxxer.hikari.HikariDataSource
import org.flywaydb.core.Flyway
import org.flywaydb.core.api.output.MigrateResult
import org.jetbrains.exposed.v1.jdbc.Database
import javax.sql.DataSource

object DatabaseFactory {
    fun createDataSource(config: DatabaseConfig): HikariDataSource =
        HikariDataSource(
            HikariConfig().apply {
                jdbcUrl = config.jdbcUrl
                username = config.user
                password = config.password
                poolName = "glide-db"
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
            .locations("classpath:db/migration")
            .cleanDisabled(true)
            .validateMigrationNaming(true)
            .load()
            .migrate()

    fun connectExposed(dataSource: DataSource): Database = Database.connect(dataSource)

    private const val MAX_POOL_SIZE = 10
    private const val CONNECTION_TIMEOUT_MS = 5_000L
}
