package com.glide.android.data.network

import com.glide.shared.api.ApiRoutes
import com.glide.shared.health.HealthResponse
import retrofit2.Response
import retrofit2.http.GET

/** Backend endpoints. Paths and models come from `:shared`, so they can't drift from the server. */
interface GlideApi {
    /** Returns `Response` because 503 still carries a [HealthResponse] body (database DOWN). */
    @GET(ApiRoutes.HEALTH)
    suspend fun health(): Response<HealthResponse>
}
