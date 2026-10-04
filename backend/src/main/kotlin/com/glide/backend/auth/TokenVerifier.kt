package com.glide.backend.auth

import com.glide.backend.config.SupabaseConfig
import com.nimbusds.jose.JWSAlgorithm
import com.nimbusds.jose.jwk.source.JWKSource
import com.nimbusds.jose.jwk.source.JWKSourceBuilder
import com.nimbusds.jose.proc.JWSVerificationKeySelector
import com.nimbusds.jose.proc.SecurityContext
import com.nimbusds.jose.util.DefaultResourceRetriever
import com.nimbusds.jwt.JWTClaimsSet
import com.nimbusds.jwt.proc.DefaultJWTClaimsVerifier
import com.nimbusds.jwt.proc.DefaultJWTProcessor
import org.slf4j.LoggerFactory
import java.net.URI
import java.util.UUID

/** The person behind a valid access token. */
data class AuthenticatedUser(
    /** Supabase user id (`sub`). Also the primary key of `app_users`. */
    val id: UUID,
    /** Phone number from the token, if they logged in by phone. */
    val phone: String?,
    /** Email from the token, if the login has one (admins log in by email, D-013). */
    val email: String? = null,
    /** True when this login passed the authenticator-app step (Supabase `aal2`). Admin routes require it (DF-16). */
    val mfaVerified: Boolean = false,
)

fun interface TokenVerifier {
    /** Returns the user for a valid token, or null for anything else (bad signature, expired, wrong project…). */
    fun verify(token: String): AuthenticatedUser?
}

/**
 * Checks Supabase access tokens (D-016) using the project's **public** signing keys, so no secret is needed.
 * Accepts only: asymmetric signatures (ES256, RS256) from this project's keys, this project's issuer,
 * audience `authenticated`, not expired, a UUID subject, and role `authenticated` (anon/service tokens are refused).
 */
class SupabaseTokenVerifier(
    issuer: String,
    keySource: JWKSource<SecurityContext>,
) : TokenVerifier {
    private val log = LoggerFactory.getLogger(SupabaseTokenVerifier::class.java)

    private val processor =
        DefaultJWTProcessor<SecurityContext>().apply {
            // Only asymmetric algorithms: an HS256 token can never pass, so the "alg confusion" attack is impossible.
            jwsKeySelector = JWSVerificationKeySelector(setOf(JWSAlgorithm.ES256, JWSAlgorithm.RS256), keySource)
            jwtClaimsSetVerifier =
                DefaultJWTClaimsVerifier(
                    AUDIENCE,
                    JWTClaimsSet
                        .Builder()
                        .issuer(issuer)
                        .claim("role", ROLE)
                        .build(),
                    setOf("sub", "exp", "iat"),
                )
        }

    override fun verify(token: String): AuthenticatedUser? =
        runCatching {
            val claims = processor.process(token, null)
            AuthenticatedUser(
                id = UUID.fromString(claims.subject),
                phone =
                    claims.getStringClaim("phone")?.takeIf {
                        it.isNotBlank()
                    },
                email = claims.getStringClaim("email")?.takeIf { it.isNotBlank() }?.lowercase(),
                mfaVerified = claims.getStringClaim("aal") == MFA_LEVEL,
            )
        }.onFailure {
            // Type only: the message can contain token contents.
            log.info("Rejected access token: {}", it.javaClass.simpleName)
        }.getOrNull()

    companion object {
        private const val AUDIENCE = "authenticated"
        private const val ROLE = "authenticated"

        /** Supabase's "authenticator assurance level" after a login completed MFA. */
        private const val MFA_LEVEL = "aal2"

        /** Waiting for Supabase's public keys. Nimbus defaults to 500 ms, which real networks in India exceed (seen: 750 ms). */
        private const val KEYS_TIMEOUT_MS = 5_000

        /** After a failed key download, wait this long before trying again (stops floods; short so logins recover fast). */
        private const val KEYS_RETRY_PAUSE_MS = 5_000L

        private const val KEYS_MAX_BYTES = 50 * 1024

        /** Production verifier: fetches the project's public keys over HTTPS and caches them (refreshes on key rotation). */
        fun forProject(config: SupabaseConfig): SupabaseTokenVerifier =
            SupabaseTokenVerifier(
                issuer = config.issuer,
                keySource =
                    JWKSourceBuilder
                        .create<SecurityContext>(
                            URI(config.jwksUrl).toURL(),
                            DefaultResourceRetriever(KEYS_TIMEOUT_MS, KEYS_TIMEOUT_MS, KEYS_MAX_BYTES),
                        ).retrying(true)
                        .rateLimited(KEYS_RETRY_PAUSE_MS)
                        .build(),
            )
    }
}
