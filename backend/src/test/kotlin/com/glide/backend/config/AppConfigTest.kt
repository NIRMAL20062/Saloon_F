package com.glide.backend.config

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class AppConfigTest {
    private val valid =
        mapOf(
            "APP_ENV" to "staging",
            "PORT" to "9000",
            "DATABASE_URL" to "jdbc:postgresql://db:5432/glide",
            "DATABASE_USER" to "glide",
            "DATABASE_PASSWORD" to "s3cret-value",
            "CORS_ALLOWED_ORIGINS" to "https://admin.example.com, http://localhost:3000",
            "SUPABASE_URL" to "https://abcd.supabase.co/",
        )

    @Test
    fun `parses a valid environment`() {
        val config = AppConfig.fromEnv(valid, "1.2.3")

        assertEquals(AppEnv.STAGING, config.env)
        assertEquals(9000, config.port)
        assertEquals("1.2.3", config.version)
        assertEquals(listOf("https://admin.example.com", "http://localhost:3000"), config.corsAllowedOrigins)
        assertEquals(300, config.rateLimitPerMinute)
        assertEquals("https://abcd.supabase.co/auth/v1", config.supabase.issuer)
        assertEquals("https://abcd.supabase.co/auth/v1/.well-known/jwks.json", config.supabase.jwksUrl)
    }

    @Test
    fun `supabase url must be https outside local development`() {
        val error =
            assertFailsWith<InvalidConfigException> {
                AppConfig.fromEnv(valid + ("SUPABASE_URL" to "http://abcd.supabase.co"), "1")
            }

        assertEquals(listOf("SUPABASE_URL must look like https://<project>.supabase.co"), error.problems)
        // A Supabase running on this machine (supabase CLI) is fine for local development.
        AppConfig.fromEnv(valid + ("APP_ENV" to "local") + ("SUPABASE_URL" to "http://127.0.0.1:54321"), "1")
    }

    @Test
    fun `rejects an out-of-range rate limit`() {
        val error =
            assertFailsWith<InvalidConfigException> {
                AppConfig.fromEnv(valid + ("RATE_LIMIT_PER_MINUTE" to "0"), "1")
            }

        assertEquals(listOf("RATE_LIMIT_PER_MINUTE must be a number between 1 and 100000"), error.problems)
    }

    @Test
    fun `defaults to local env and port 8080`() {
        val config = AppConfig.fromEnv(valid - "APP_ENV" - "PORT", "1")

        assertEquals(AppEnv.LOCAL, config.env)
        assertEquals(8080, config.port)
    }

    @Test
    fun `reports every problem at once`() {
        val error =
            assertFailsWith<InvalidConfigException> {
                AppConfig.fromEnv(mapOf("APP_ENV" to "prod-ish", "PORT" to "99999"), "1")
            }

        assertEquals(
            listOf(
                "APP_ENV must be one of [LOCAL, TEST, STAGING, PRODUCTION], was 'prod-ish'",
                "PORT must be a number between 1 and 65535, was '99999'",
                "DATABASE_URL is required",
                "DATABASE_USER is required",
                "DATABASE_PASSWORD is required",
                "SUPABASE_URL is required",
            ),
            error.problems,
        )
    }

    @Test
    fun `rejects non-postgres database urls`() {
        val error =
            assertFailsWith<InvalidConfigException> {
                AppConfig.fromEnv(valid + ("DATABASE_URL" to "jdbc:mysql://db/glide"), "1")
            }

        assertEquals(listOf("DATABASE_URL must start with jdbc:postgresql://"), error.problems)
    }

    @Test
    fun `a supabase database needs an encrypted connection`() {
        val pooler = "jdbc:postgresql://aws-0-ap-south-1.pooler.supabase.com:5432/postgres"
        val error =
            assertFailsWith<InvalidConfigException> { AppConfig.fromEnv(valid + ("DATABASE_URL" to pooler), "1") }

        assertEquals(
            listOf(
                "DATABASE_URL to Supabase must end with ?sslmode=require (otherwise the password travels unencrypted)",
            ),
            error.problems,
        )
        assertEquals(
            "$pooler?sslmode=require",
            AppConfig.fromEnv(valid + ("DATABASE_URL" to "$pooler?sslmode=require"), "1").database.jdbcUrl,
        )
        assertFailsWith<InvalidConfigException> {
            AppConfig.fromEnv(
                valid + ("DATABASE_URL" to "jdbc:postgresql://db.abc.supabase.co:5432/postgres?sslmode=disable"),
                "1",
            )
        }
    }

    @Test
    fun `rejects malformed cors origins`() {
        val error =
            assertFailsWith<InvalidConfigException> {
                AppConfig.fromEnv(valid + ("CORS_ALLOWED_ORIGINS" to "*,https://ok.example.com/path"), "1")
            }

        assertEquals(2, error.problems.size)
    }

    @Test
    fun `production requires https origins`() {
        val error =
            assertFailsWith<InvalidConfigException> {
                AppConfig.fromEnv(valid + ("APP_ENV" to "production"), "1")
            }

        assertEquals(
            listOf("CORS_ALLOWED_ORIGINS entry 'http://localhost:3000' must use https in production"),
            error.problems,
        )
    }

    @Test
    fun `never prints the database password`() {
        val printed = AppConfig.fromEnv(valid, "1").toString()

        assertFalse(printed.contains("s3cret-value"))
        assertTrue(printed.contains("password=***"))
    }
}
