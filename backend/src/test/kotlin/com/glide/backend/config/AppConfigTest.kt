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
            "SUPABASE_SECRET_KEY" to SECRET_KEY,
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

    @Test
    fun `reads the Supabase secret key and never prints it`() {
        val config = AppConfig.fromEnv(valid, "1")

        assertEquals(SECRET_KEY, config.supabase.secretKey)
        assertFalse(config.toString().contains(SECRET_KEY))
        assertTrue(config.toString().contains("secretKey=***"))
    }

    @Test
    fun `the secret key is optional on a laptop and in tests`() {
        listOf("local", "test").forEach { env ->
            val config = AppConfig.fromEnv(valid - "SUPABASE_SECRET_KEY" + ("APP_ENV" to env), "1")

            assertEquals(null, config.supabase.secretKey, env)
        }
        assertEquals(
            null,
            AppConfig.fromEnv(valid + ("APP_ENV" to "local") + ("SUPABASE_SECRET_KEY" to " "), "1").supabase.secretKey,
        )
    }

    @Test
    fun `the secret key is required on staging and production`() {
        listOf("staging", "production").forEach { env ->
            val error =
                assertFailsWith<InvalidConfigException> {
                    AppConfig.fromEnv(
                        valid - "SUPABASE_SECRET_KEY" + ("APP_ENV" to env) +
                            ("CORS_ALLOWED_ORIGINS" to "https://a.example.com"),
                        "1",
                    )
                }

            assertEquals(
                listOf("SUPABASE_SECRET_KEY is required in ${env.uppercase()} (admin invites need it)"),
                error.problems,
            )
        }
    }

    @Test
    fun `the publishable key or anything else in the secret key's place is refused without echoing it`() {
        val publishable =
            assertFailsWith<InvalidConfigException> {
                AppConfig.fromEnv(valid + ("SUPABASE_SECRET_KEY" to "sb_publishable_abcdefghijklmnop"), "1")
            }
        assertEquals(
            listOf("SUPABASE_SECRET_KEY is the publishable key; use the secret key (sb_secret_...)"),
            publishable.problems,
        )

        val garbage =
            assertFailsWith<InvalidConfigException> {
                AppConfig.fromEnv(
                    valid + ("SUPABASE_SECRET_KEY" to "hunter2"),
                    "1",
                )
            }
        assertFalse(garbage.message.orEmpty().contains("hunter2"))
    }

    @Test
    fun `the legacy service_role key is accepted`() {
        val legacy = "eyJhbGciOiJIUzI1NiJ9.eyJyb2xlIjoic2VydmljZV9yb2xlIn0.c2lnbmF0dXJl"

        assertEquals(legacy, AppConfig.fromEnv(valid + ("SUPABASE_SECRET_KEY" to legacy), "1").supabase.secretKey)
    }

    private companion object {
        const val SECRET_KEY = "sb_secret_test_only_0123456789abcdef"
    }
}
