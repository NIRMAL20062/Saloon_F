package com.glide.shared.me

import kotlinx.serialization.Serializable

/** Which interface the app shows this person (D-024). Chosen at onboarding; can be switched later. */
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

/** Body of `PUT /v1/me/side`. */
@Serializable
data class UpdateSideRequest(
    val side: UserSide,
)
