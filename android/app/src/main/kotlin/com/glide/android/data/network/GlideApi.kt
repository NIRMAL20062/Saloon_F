package com.glide.android.data.network

import com.glide.shared.api.ApiRoutes
import com.glide.shared.health.HealthResponse
import com.glide.shared.me.MeResponse
import com.glide.shared.me.UpdateProfileRequest
import com.glide.shared.me.UpdateSideRequest
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.PUT

/** Backend endpoints. Paths and models come from `:shared`, so they can't drift from the server. */
interface GlideApi {
    /** Returns `Response` because 503 still carries a [HealthResponse] body (database DOWN). */
    @GET(ApiRoutes.HEALTH)
    suspend fun health(): Response<HealthResponse>

    /** The signed-in person. Needs a login token (added by BearerTokenInterceptor). */
    @GET(ApiRoutes.ME)
    suspend fun me(): MeResponse

    /** The onboarding answer, customer or salon. Final once saved: a different side → 409 SIDE_ALREADY_CHOSEN (D-030). */
    @PUT(ApiRoutes.ME_SIDE)
    suspend fun chooseSide(
        @Body body: UpdateSideRequest,
    ): MeResponse

    /** The person's own name and optional email (BE-018). 400 INVALID_NAME / INVALID_EMAIL. */
    @PUT(ApiRoutes.ME_PROFILE)
    suspend fun updateProfile(
        @Body body: UpdateProfileRequest,
    ): MeResponse
}
