package com.glide.backend

import com.glide.backend.auth.SupabaseTokenVerifier
import com.nimbusds.jose.JOSEObjectType
import com.nimbusds.jose.JWSAlgorithm
import com.nimbusds.jose.JWSHeader
import com.nimbusds.jose.crypto.ECDSASigner
import com.nimbusds.jose.crypto.MACSigner
import com.nimbusds.jose.jwk.Curve
import com.nimbusds.jose.jwk.ECKey
import com.nimbusds.jose.jwk.JWKSet
import com.nimbusds.jose.jwk.gen.ECKeyGenerator
import com.nimbusds.jose.jwk.source.ImmutableJWKSet
import com.nimbusds.jose.proc.SecurityContext
import com.nimbusds.jwt.JWTClaimsSet
import com.nimbusds.jwt.SignedJWT
import java.util.Date
import java.util.UUID
import kotlin.time.Duration
import kotlin.time.Duration.Companion.hours

/**
 * Tokens shaped exactly like Supabase access tokens (ES256, iss/aud/role/phone claims), signed with a test key.
 * The verifier under test gets the test key's public half, just like production gets Supabase's JWKS.
 */
object TestTokens {
    const val PROJECT_URL = "https://test-project.supabase.co"
    const val ISSUER = "$PROJECT_URL/auth/v1"

    private val key: ECKey = ECKeyGenerator(Curve.P_256).keyID("test-key").generate()
    private val otherKey: ECKey = ECKeyGenerator(Curve.P_256).keyID("test-key").generate()

    val verifier = SupabaseTokenVerifier(ISSUER, ImmutableJWKSet<SecurityContext>(JWKSet(key.toPublicJWK())))

    /** The public half as a JWKS document, exactly what Supabase serves at /auth/v1/.well-known/jwks.json. */
    val publicJwksJson: String = JWKSet(key.toPublicJWK()).toString()

    fun token(
        userId: UUID = UUID.randomUUID(),
        phone: String? = "919000000001",
        issuer: String = ISSUER,
        audience: String = "authenticated",
        role: String = "authenticated",
        expiresIn: Duration = 1.hours,
        signingKey: ECKey = key,
    ): String {
        val now = System.currentTimeMillis()
        val claims =
            JWTClaimsSet
                .Builder()
                .issuer(issuer)
                .audience(audience)
                .subject(userId.toString())
                .issueTime(Date(now))
                .expirationTime(Date(now + expiresIn.inWholeMilliseconds))
                .claim("role", role)
                .apply { if (phone != null) claim("phone", phone) }
                .build()
        val header =
            JWSHeader
                .Builder(JWSAlgorithm.ES256)
                .keyID(signingKey.keyID)
                .type(JOSEObjectType.JWT)
                .build()
        return SignedJWT(header, claims).apply { sign(ECDSASigner(signingKey)) }.serialize()
    }

    /** Valid in every way except the signature comes from a different key (same key id, so only the math catches it). */
    fun tokenSignedByOtherKey(): String = token(signingKey = otherKey)

    /** An HS256 token, as an attacker would forge with a guessed secret ("alg confusion"). */
    fun hs256Token(userId: UUID = UUID.randomUUID()): String {
        val claims =
            JWTClaimsSet
                .Builder()
                .issuer(ISSUER)
                .audience("authenticated")
                .subject(userId.toString())
                .issueTime(Date())
                .expirationTime(Date(System.currentTimeMillis() + 3_600_000))
                .claim("role", "authenticated")
                .build()
        return SignedJWT(JWSHeader(JWSAlgorithm.HS256), claims)
            .apply { sign(MACSigner(ByteArray(32) { 7 })) }
            .serialize()
    }
}
