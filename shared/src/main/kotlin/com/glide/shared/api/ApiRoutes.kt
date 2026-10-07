package com.glide.shared.api

/** URL paths of the backend API. Backend routes and Android Retrofit interfaces both reference these. */
object ApiRoutes {
    /** Liveness: the process is up. No dependencies checked. */
    const val HEALTH_LIVE = "/health/live"

    /** Readiness: the process is up **and** its dependencies (database) respond. */
    const val HEALTH = "/health"

    /** Prefix for every business endpoint, e.g. `/v1/salons`. Bump only for breaking changes. */
    const val V1 = "/v1"

    /** The signed-in user (any side). Requires `Authorization: Bearer <Supabase access token>`. */
    const val ME = "$V1/me"

    /** Save the onboarding choice: customer side or salon side (D-024). */
    const val ME_SIDE = "$V1/me/side"

    /** Save the person's name and optional email (BE-018). */
    const val ME_PROFILE = "$V1/me/profile"

    /** The signed-in admin of our team (BE-020). Needs an admin login with the authenticator-app step done. */
    const val ADMIN_ME = "$V1/admin/me"

    /** Invite another admin by email (BE-020). Admins only. */
    const val ADMIN_INVITES = "$V1/admin/admins/invites"

    /** Create the signed-in person's salon; they become its owner (BE-017). Salon side only. */
    const val SALON_SALONS = "$V1/salon/salons"

    /** The signed-in person's one salon and their role in it (BE-017, D-036). */
    const val SALON_ME = "$V1/salon/me"

    /** The owner edits the salon's profile while it isn't live (BE-017). */
    const val SALON_PROFILE = "$V1/salon/salon"

    /** The owner saves (PUT) or reads (GET) the salon's bank details, always masked (BE-032, DF-24). */
    const val SALON_BANK_DETAILS = "$V1/salon/bank-details"

    /** The owner sends the salon to our team for verification (BE-032, D-033). */
    const val SALON_SUBMIT = "$V1/salon/submit-for-verification"
}
