package com.glide.backend

import com.glide.backend.config.AppConfig
import com.glide.backend.config.AppEnv
import com.glide.backend.config.DatabaseConfig

/** Config for tests that don't touch a real database. */
fun testConfig(
    database: DatabaseConfig = DatabaseConfig("jdbc:postgresql://unused:5432/unused", "unused", "unused"),
    corsAllowedOrigins: List<String> = listOf("http://localhost:3000"),
) = AppConfig(
    env = AppEnv.TEST,
    port = 0,
    version = "test",
    database = database,
    corsAllowedOrigins = corsAllowedOrigins,
)
