package com.glide.shared.me

import kotlinx.serialization.Serializable

/** Which interface the app shows this person (D-024). Chosen once at onboarding and final (D-030). */
@Serializable
enum class UserSide { CUSTOMER, SALON }

/** Body of `GET /v1/me` and `PUT /v1/me/side`. */
@Serializable
data class MeResponse(
    /** Supabase user id (UUID). */
    val id: String,
    /** Phone number the person logged in with, in E.164 without "+" (as Supabase returns it), e.g. `919000000001`. */
    val phone: String? = null,
    /** Null until the person answers "customer or salon?" at onboarding. */
    val side: UserSide? = null,
)

/** Error codes of the /v1/me endpoints (see ErrorCodes for the shared ones). */
object MeErrorCodes {
    /** `PUT /v1/me/side` with a different side than the one already chosen. The choice is final (D-030). */
    const val SIDE_ALREADY_CHOSEN = "SIDE_ALREADY_CHOSEN"
}

/** Body of `PUT /v1/me/side`. */
@Serializable
data class UpdateSideRequest(
    val side: UserSide,
)
