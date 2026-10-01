package com.glide.backend.health

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import org.slf4j.LoggerFactory
import javax.sql.DataSource
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds

fun interface DatabaseHealthCheck {
    suspend fun isHealthy(): Boolean
}

/** Healthy = a pooled connection can be borrowed and answers within [timeout]. */
class JdbcDatabaseHealthCheck(
    private val dataSource: DataSource,
    private val timeout: Duration = 3.seconds,
) : DatabaseHealthCheck {
    private val log = LoggerFactory.getLogger(JdbcDatabaseHealthCheck::class.java)

    override suspend fun isHealthy(): Boolean =
        withTimeoutOrNull(timeout) {
            withContext(Dispatchers.IO) {
                runCatching { dataSource.connection.use { it.isValid(timeout.inWholeSeconds.toInt()) } }
                    // Log the exception type only: driver messages can contain hostnames or user names.
                    .onFailure { log.warn("Database health check failed: {}", it.javaClass.name) }
                    .getOrDefault(false)
            }
        } ?: false
}
