package com.glide.backend.config

enum class AppEnv { LOCAL, TEST, STAGING, PRODUCTION }

data class DatabaseConfig(
    val jdbcUrl: String,
    val user: String,
    val password: String,
) {
    // Never print the password, even in debug logs or crash reports.
    override fun toString() = "DatabaseConfig(jdbcUrl=$jdbcUrl, user=$user, password=***)"
}

/** Where logins come from (D-016). Tokens are checked with Supabase's public signing keys. */
data class SupabaseConfig(
    /** Project URL, e.g. `https://abcd.supabase.co`. */
    val url: String,
    /**
     * Secret key (`sb_secret_...`, or the legacy service_role key) for Supabase's admin API: inviting admins (BE-020).
     * Bypasses every Supabase rule, so it lives only in the environment. Null on a laptop without it: invites are off.
     */
    val secretKey: String? = null,
) {
    // Never print the secret key, even in debug logs or crash reports.
    override fun toString() = "SupabaseConfig(url=$url, secretKey=${if (secretKey == null) "none" else "***"})"

    /** Value of the `iss` claim in every access token from this project. */
    val issuer: String get() = "$url/auth/v1"

    /** Public signing keys (ES256) used to check token signatures. */
    val jwksUrl: String get() = "$url/auth/v1/.well-known/jwks.json"
}

/**
 * Everything the backend needs from its environment. Loaded once at startup.
 * Secrets come **only** from environment variables (see .env.example), never from files in git.
 */
data class AppConfig(
    val env: AppEnv,
    val port: Int,
    val version: String,
    val database: DatabaseConfig,
    /** Browser origins allowed to call the API, e.g. `https://admin.example.com`. Empty = none. */
    val corsAllowedOrigins: List<String>,
    /** Max requests per minute from one client IP, across all endpoints. */
    val rateLimitPerMinute: Int,
    val supabase: SupabaseConfig,
) {
    companion object {
        /**
         * Builds the config from environment variables. Collects **every** problem before failing,
         * so a misconfigured deploy shows the whole list at once instead of one error per restart.
         */
        fun fromEnv(
            env: Map<String, String>,
            version: String,
        ): AppConfig {
            val problems = mutableListOf<String>()

            fun required(name: String): String =
                env[name]?.takeIf { it.isNotBlank() } ?: "".also { problems += "$name is required" }

            val appEnv =
                env["APP_ENV"]?.let { raw ->
                    AppEnv.entries.firstOrNull { it.name.equals(raw, ignoreCase = true) }
                        ?: AppEnv.LOCAL.also { problems += "APP_ENV must be one of ${AppEnv.entries}, was '$raw'" }
                } ?: AppEnv.LOCAL

            val port =
                env["PORT"]?.let { raw ->
                    raw.toIntOrNull()?.takeIf { it in 1..65535 }
                        ?: 0.also { problems += "PORT must be a number between 1 and 65535, was '$raw'" }
                } ?: DEFAULT_PORT

            val database =
                DatabaseConfig(
                    jdbcUrl = required("DATABASE_URL"),
                    user = required("DATABASE_USER"),
                    password = required("DATABASE_PASSWORD"),
                )
            if (database.jdbcUrl.isNotEmpty() && !database.jdbcUrl.startsWith("jdbc:postgresql://")) {
                problems += "DATABASE_URL must start with jdbc:postgresql://"
            }
            if (database.jdbcUrl.isSupabase() && !SSL_REQUIRED.containsMatchIn(database.jdbcUrl)) {
                problems +=
                    "DATABASE_URL to Supabase must end with ?sslmode=require (otherwise the password travels unencrypted)"
            }

            val origins =
                env["CORS_ALLOWED_ORIGINS"]
                    .orEmpty()
                    .split(',')
                    .map { it.trim() }
                    .filter { it.isNotEmpty() }
            origins.filterNot { it.matches(ORIGIN_REGEX) }.forEach {
                problems += "CORS_ALLOWED_ORIGINS entry '$it' must look like https://host[:port] with no path"
            }
            if (appEnv == AppEnv.PRODUCTION) {
                origins.filter { it.startsWith("http://") }.forEach {
                    problems += "CORS_ALLOWED_ORIGINS entry '$it' must use https in production"
                }
            }

            val rateLimit =
                env["RATE_LIMIT_PER_MINUTE"]?.let { raw ->
                    raw.toIntOrNull()?.takeIf { it in 1..MAX_RATE_LIMIT }
                        ?: 0.also { problems += "RATE_LIMIT_PER_MINUTE must be a number between 1 and $MAX_RATE_LIMIT" }
                } ?: DEFAULT_RATE_LIMIT

            val supabaseUrl = required("SUPABASE_URL").trimEnd('/')
            if (supabaseUrl.isNotEmpty()) {
                val local = appEnv == AppEnv.LOCAL || appEnv == AppEnv.TEST
                // http only for a Supabase running on this machine (supabase CLI); everything else must be https.
                val ok = supabaseUrl.matches(SUPABASE_URL_REGEX) && (supabaseUrl.startsWith("https://") || local)
                if (!ok) problems += "SUPABASE_URL must look like https://<project>.supabase.co"
            }

            val secretKey = env["SUPABASE_SECRET_KEY"]?.trim()?.ifEmpty { null }
            when {
                secretKey == null && (appEnv == AppEnv.STAGING || appEnv == AppEnv.PRODUCTION) -> {
                    problems += "SUPABASE_SECRET_KEY is required in $appEnv (admin invites need it)"
                }

                secretKey != null && secretKey.startsWith("sb_publishable_") -> {
                    problems += "SUPABASE_SECRET_KEY is the publishable key; use the secret key (sb_secret_...)"
                }

                secretKey != null && !secretKey.matches(SECRET_KEY_REGEX) && !secretKey.matches(LEGACY_KEY_REGEX) -> {
                    problems +=
                        "SUPABASE_SECRET_KEY must be a Supabase secret key (sb_secret_...) or the legacy service_role key"
                }
            }

            if (problems.isNotEmpty()) throw InvalidConfigException(problems)
            return AppConfig(
                appEnv,
                port,
                version,
                database,
                origins,
                rateLimit,
                SupabaseConfig(supabaseUrl, secretKey),
            )
        }

        private const val DEFAULT_PORT = 8080
        private const val DEFAULT_RATE_LIMIT = 300
        private val SUPABASE_URL_REGEX = Regex("^https?://[A-Za-z0-9.-]+(:\\d{1,5})?$")
        private const val MAX_RATE_LIMIT = 100_000
        private val SECRET_KEY_REGEX = Regex("^sb_secret_[A-Za-z0-9_-]{16,}$")

        /** The legacy service_role key is a JWT: three base64url parts. */
        private val LEGACY_KEY_REGEX = Regex("^[A-Za-z0-9_-]+\\.[A-Za-z0-9_-]+\\.[A-Za-z0-9_-]+$")
        private val ORIGIN_REGEX = Regex("^https?://[A-Za-z0-9.-]+(:\\d{1,5})?$")
    }
}

class InvalidConfigException(
    val problems: List<String>,
) : IllegalStateException("Invalid configuration:\n" + problems.joinToString("\n") { " - $it" })

/** Postgres hosted by Supabase (direct or through its connection pooler). */
private fun String.isSupabase(): Boolean {
    val host = substringAfter("://").substringBefore('/').substringBefore('?').substringBefore(':')
    return host.endsWith(".supabase.co") || host.endsWith(".supabase.com")
}

private val SSL_REQUIRED = Regex("[?&]sslmode=(require|verify-ca|verify-full)(&|$)")
