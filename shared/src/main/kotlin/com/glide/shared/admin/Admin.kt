package com.glide.shared.admin

import kotlinx.serialization.Serializable

/** Where an admin is in joining (BE-020). */
@Serializable
enum class AdminStatus {
    /** Added or invited; hasn't finished an admin login (email code + authenticator app) yet. */
    INVITED,

    /** Has logged in to the admin website with the authenticator app at least once. */
    ACTIVE,
}

/** One admin of our team (D-013). Body of `GET /v1/admin/me` and `POST /v1/admin/admins/invites`. */
@Serializable
data class AdminResponse(
    /** Supabase user id (UUID). */
    val id: String,
    /** Login email, in lower case. */
    val email: String,
    val status: AdminStatus,
)

/** Body of `POST /v1/admin/admins/invites`. */
@Serializable
data class InviteAdminRequest(
    val email: String,
)

/** Error codes of the /v1/admin endpoints (see ErrorCodes for the shared ones). */
object AdminErrorCodes {
    /** 403: signed in, but this person is not one of our admins. */
    const val NOT_ADMIN = "NOT_ADMIN"

    /** 403: an admin, but this login hasn't passed the authenticator-app step (MFA). Log in again with the app's code. */
    const val MFA_REQUIRED = "MFA_REQUIRED"

    /** 400: the email doesn't look like an email address or is longer than 254 characters. */
    const val INVALID_EMAIL = "INVALID_EMAIL"

    /** 409: this email is already an admin (invited or active). */
    const val ADMIN_ALREADY_EXISTS = "ADMIN_ALREADY_EXISTS"

    /** 502: Supabase refused or failed to send the invite (e.g. the email service is down). Nothing was saved. */
    const val INVITE_FAILED = "INVITE_FAILED"

    /** 503: the backend has no Supabase secret key configured, so it can't invite anyone. */
    const val INVITES_UNAVAILABLE = "INVITES_UNAVAILABLE"
}
