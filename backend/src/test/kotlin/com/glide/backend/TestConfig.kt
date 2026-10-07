package com.glide.backend

import com.glide.backend.config.AppConfig
import com.glide.backend.config.AppEnv
import com.glide.backend.config.DatabaseConfig
import com.glide.backend.config.SecretBytes
import com.glide.backend.config.SupabaseConfig
import com.glide.backend.health.DatabaseHealthCheck
import com.glide.backend.salons.ExposedSalonRepository

/** Config for tests that don't touch a real database. */
fun testConfig(
    database: DatabaseConfig = DatabaseConfig("jdbc:postgresql://unused:5432/unused", "unused", "unused"),
    corsAllowedOrigins: List<String> = listOf("http://localhost:3000"),
    env: AppEnv = AppEnv.TEST,
    rateLimitPerMinute: Int = 10_000,
    bankDetailsKey: SecretBytes? = TEST_BANK_KEY,
) = AppConfig(
    env = env,
    port = 0,
    version = "test",
    database = database,
    corsAllowedOrigins = corsAllowedOrigins,
    rateLimitPerMinute = rateLimitPerMinute,
    supabase = SupabaseConfig(TestTokens.PROJECT_URL),
    bankDetailsKey = bankDetailsKey,
)

/** A fixed key for tests only (never used outside them). */
val TEST_BANK_KEY = SecretBytes(ByteArray(32) { (it * 7).toByte() })

/** Dependencies with fakes, for route tests that don't need a real database. */
fun fakeDependencies(databaseHealthy: Boolean = true) =
    AppDependencies(
        databaseHealthCheck = DatabaseHealthCheck { databaseHealthy },
        tokenVerifier = TestTokens.verifier,
        transactor = DirectTransactor,
        users = InMemoryUserRepository(),
        admins = InMemoryAdminRepository(),
        // Stateless; salon route tests pair it with the test database's transactor (SalonRoutesTest).
        salons = ExposedSalonRepository(),
        authAdmin = FakeAuthAdmin(),
    )
