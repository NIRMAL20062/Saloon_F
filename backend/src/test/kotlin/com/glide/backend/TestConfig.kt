package com.glide.backend

import com.glide.backend.config.AppConfig
import com.glide.backend.config.AppEnv
import com.glide.backend.config.DatabaseConfig
import com.glide.backend.health.DatabaseHealthCheck

/** Config for tests that don't touch a real database. */
fun testConfig(
    database: DatabaseConfig = DatabaseConfig("jdbc:postgresql://unused:5432/unused", "unused", "unused"),
    corsAllowedOrigins: List<String> = listOf("http://localhost:3000"),
    env: AppEnv = AppEnv.TEST,
    rateLimitPerMinute: Int = 10_000,
) = AppConfig(
    env = env,
    port = 0,
    version = "test",
    database = database,
    corsAllowedOrigins = corsAllowedOrigins,
    rateLimitPerMinute = rateLimitPerMinute,
)

/** Dependencies with fakes, for route tests that don't need a real database. */
fun fakeDependencies(databaseHealthy: Boolean = true) =
    AppDependencies(
        databaseHealthCheck = DatabaseHealthCheck { databaseHealthy },
    )
