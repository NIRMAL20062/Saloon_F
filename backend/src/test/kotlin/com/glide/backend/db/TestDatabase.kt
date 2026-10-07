package com.glide.backend.db

import com.glide.backend.config.DatabaseConfig
import com.zaxxer.hikari.HikariDataSource
import org.jetbrains.exposed.v1.jdbc.Database
import org.testcontainers.postgresql.PostgreSQLContainer

/**
 * One real PostgreSQL (same major version as production) shared by every test in the JVM.
 * Needs Docker. Started on first use and migrated once; tests isolate their data themselves.
 */
object TestDatabase {
    const val IMAGE = "postgres:17-alpine"

    private val container: PostgreSQLContainer by lazy {
        PostgreSQLContainer(IMAGE).apply { start() }
    }

    val config: DatabaseConfig by lazy {
        DatabaseConfig(container.jdbcUrl, container.username, container.password)
    }

    /**
     * A migrated data source, the backend's own pool: its connections work as the limited role (BE-031), so tests see what
     * the backend sees, row-level security included. Do not close it; it lives for the whole test run.
     */
    val dataSource: HikariDataSource by lazy {
        DatabaseFactory.migrate(config)
        DatabaseFactory.createDataSource(config)
    }

    /** Exposed connection to the same migrated database, for repository tests. */
    val exposed: Database by lazy { DatabaseFactory.connectExposed(dataSource) }
}
