package com.glide.shared.me

import kotlinx.serialization.Serializable

/** Which interface the app shows this person (D-024). Chosen once at onboarding and final (D-030). */
@Serializable
enum class UserSide { CUSTOMER, SALON }

/** Body of `GET /v1/me`, `PUT /v1/me/side` and `PUT /v1/me/profile`. */
@Serializable
data class MeResponse(
    /** Supabase user id (UUID). */
    val id: String,
    /** Phone number the person logged in with, in E.164 without "+" (as Supabase returns it), e.g. `919000000001`. */
    val phone: String? = null,
    /** Null until the person answers "customer or salon?" at onboarding. */
    val side: UserSide? = null,
    /** The person's name (2–60 characters); null until they give it at onboarding (BE-018, DF-18). */
    val name: String? = null,
    /** Optional email, stored in lower case; null when not given. */
    val email: String? = null,
)

/** Error codes of the /v1/me endpoints (see ErrorCodes for the shared ones). */
object MeErrorCodes {
    /** `PUT /v1/me/side` with a different side than the one already chosen. The choice is final (D-030). */
    const val SIDE_ALREADY_CHOSEN = "SIDE_ALREADY_CHOSEN"

    /** `PUT /v1/me/profile`: the name is missing, shorter than 2 or longer than 60 characters, or has control characters. */
    const val INVALID_NAME = "INVALID_NAME"

    /** `PUT /v1/me/profile`: the email doesn't look like an email address or is longer than 254 characters. */
    const val INVALID_EMAIL = "INVALID_EMAIL"
}

/** Limits of `PUT /v1/me/profile`, shared so the app can check them before sending. */
object ProfileRules {
    const val NAME_MIN = 2
    const val NAME_MAX = 60
    const val EMAIL_MAX = 254
}

/**
 * Body of `PUT /v1/me/profile`. The name is trimmed; an empty or missing [email] clears it (it's optional).
 */
@Serializable
data class UpdateProfileRequest(
    val name: String,
    val email: String? = null,
)

/** Body of `PUT /v1/me/side`. */
@Serializable
data class UpdateSideRequest(
    val side: UserSide,
)
