package com.glide.android.testing

import com.glide.android.data.health.HealthRepository
import com.glide.android.data.network.ApiResult
import com.glide.shared.health.HealthResponse
import com.glide.shared.health.HealthStatus

/** Returns whatever [next] is set to and counts calls. */
class FakeHealthRepository(
    var next: ApiResult<HealthResponse> = ApiResult.Success(HealthResponse(HealthStatus.UP, "0.1.0", HealthStatus.UP)),
) : HealthRepository {
    var calls = 0
        private set

    override suspend fun health(): ApiResult<HealthResponse> {
        calls++
        return next
    }
}
