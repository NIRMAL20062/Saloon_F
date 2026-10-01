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

            if (problems.isNotEmpty()) throw InvalidConfigException(problems)
            return AppConfig(appEnv, port, version, database, origins, rateLimit)
        }

        private const val DEFAULT_PORT = 8080
        private const val DEFAULT_RATE_LIMIT = 300
        private const val MAX_RATE_LIMIT = 100_000
        private val ORIGIN_REGEX = Regex("^https?://[A-Za-z0-9.-]+(:\\d{1,5})?$")
    }
}

class InvalidConfigException(
    val problems: List<String>,
) : IllegalStateException("Invalid configuration:\n" + problems.joinToString("\n") { " - $it" })
