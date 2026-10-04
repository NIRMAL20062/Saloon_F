package com.glide.backend.admins

import com.glide.backend.config.SupabaseConfig
import kotlinx.coroutines.future.await
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import org.slf4j.LoggerFactory
import java.net.URI
import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpResponse
import java.time.Duration
import java.util.UUID

/** What Supabase said about an invite. */
sealed interface InviteResult {
    /** Supabase created the login and sent its invite email. */
    data class Invited(
        val userId: UUID,
    ) : InviteResult

    /** The email already has a Supabase login (so no invite email was sent). */
    data object AlreadyRegistered : InviteResult
}

/** Supabase refused or didn't answer. Carries no Supabase text: it can contain the email. */
class AuthAdminException(
    val status: Int?,
    val errorCode: String?,
    cause: Throwable? = null,
) : RuntimeException("Supabase admin API failed: status=$status error_code=$errorCode", cause)

/**
 * The parts of Supabase's admin API (needs the secret key) the backend uses. Behind an interface so tests use a fake.
 * Both calls are idempotent from our side: running them again for the same email finds the same login.
 */
interface AuthAdmin {
    /** Creates a login for [email] and has Supabase send its invite email. */
    suspend fun invite(email: String): InviteResult

    /** Finds the login for [email], creating it if there is none, **without sending any email**. Returns its user id. */
    suspend fun findOrCreateUser(email: String): UUID
}

/** Used when no secret key is configured: every call fails, and routes answer INVITES_UNAVAILABLE. */
object DisabledAuthAdmin : AuthAdmin {
    override suspend fun invite(email: String): InviteResult = throw AuthAdminUnavailableException()

    override suspend fun findOrCreateUser(email: String): UUID = throw AuthAdminUnavailableException()
}

class AuthAdminUnavailableException : IllegalStateException("SUPABASE_SECRET_KEY is not set")

/** Talks to `<project>/auth/v1/...` with the JDK's HTTP client (no extra dependency). */
class SupabaseAuthAdmin(
    projectUrl: String,
    private val secretKey: String,
    private val http: HttpClient = defaultClient(),
) : AuthAdmin {
    private val base = "$projectUrl/auth/v1"
    private val log = LoggerFactory.getLogger(SupabaseAuthAdmin::class.java)

    override suspend fun invite(email: String): InviteResult {
        val response = post("/invite", JSON.encodeToString(EmailBody.serializer(), EmailBody(email)))
        return when {
            response.statusCode() in 200..299 -> InviteResult.Invited(userId(response))
            response.errorCode() == EMAIL_EXISTS -> InviteResult.AlreadyRegistered
            else -> throw failure(response)
        }
    }

    override suspend fun findOrCreateUser(email: String): UUID {
        // A magic link that is generated but never sent: Supabase returns the login (creating it if needed) and no email
        // goes out. The link and one-time code in the answer are ignored and never logged.
        val body =
            JSON.encodeToString(
                GenerateLinkBody.serializer(),
                GenerateLinkBody(type = "magiclink", email = email),
            )
        val response = post("/admin/generate_link", body)
        if (response.statusCode() !in 200..299) throw failure(response)
        return userId(response)
    }

    private suspend fun post(
        path: String,
        json: String,
    ): HttpResponse<String> {
        val request =
            HttpRequest
                .newBuilder(URI("$base$path"))
                .timeout(REQUEST_TIMEOUT)
                .header("Content-Type", "application/json")
                .header("apikey", secretKey)
                .apply {
                    // The legacy service_role key is a JWT and must also be the bearer token; new sb_secret_ keys must not.
                    if (!secretKey.startsWith("sb_secret_")) header("Authorization", "Bearer $secretKey")
                }.POST(HttpRequest.BodyPublishers.ofString(json))
                .build()
        return try {
            http.sendAsync(request, HttpResponse.BodyHandlers.ofString()).await()
        } catch (e: java.io.IOException) {
            log.warn("Supabase admin API unreachable: {}", e.javaClass.simpleName)
            throw AuthAdminException(status = null, errorCode = null, cause = e)
        }
    }

    private fun userId(response: HttpResponse<String>): UUID =
        runCatching { UUID.fromString(JSON.decodeFromString(UserBody.serializer(), response.body()).id) }
            .getOrElse { throw AuthAdminException(response.statusCode(), "unreadable_user", it) }

    private fun HttpResponse<String>.errorCode(): String? =
        runCatching { JSON.decodeFromString(ErrorBody.serializer(), body()).errorCode }.getOrNull()

    private fun failure(response: HttpResponse<String>): AuthAdminException {
        val code = response.errorCode()
        // Status and Supabase's error code only: the message can contain the email.
        log.warn("Supabase admin API refused: status={} error_code={}", response.statusCode(), code)
        return AuthAdminException(response.statusCode(), code)
    }

    @Serializable
    private data class EmailBody(
        val email: String,
    )

    @Serializable
    private data class GenerateLinkBody(
        val type: String,
        val email: String,
    )

    @Serializable
    private data class UserBody(
        val id: String,
    )

    @Serializable
    private data class ErrorBody(
        @SerialName("error_code") val errorCode: String? = null,
    )

    companion object {
        private const val EMAIL_EXISTS = "email_exists"
        private val REQUEST_TIMEOUT: Duration = Duration.ofSeconds(10)
        private val JSON = Json { ignoreUnknownKeys = true }

        private fun defaultClient(): HttpClient =
            HttpClient
                .newBuilder()
                .connectTimeout(Duration.ofSeconds(5))
                .followRedirects(HttpClient.Redirect.NEVER)
                .build()

        /** The real client when the secret key is configured, otherwise [DisabledAuthAdmin]. */
        fun forProject(config: SupabaseConfig): AuthAdmin =
            config.secretKey?.let { SupabaseAuthAdmin(config.url, it) } ?: DisabledAuthAdmin
    }
}
