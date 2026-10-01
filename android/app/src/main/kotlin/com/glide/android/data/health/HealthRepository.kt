package com.glide.android.data.health

import com.glide.android.data.network.ApiResult
import com.glide.android.data.network.GlideApi
import com.glide.android.data.network.apiCall
import com.glide.shared.api.ApiJson
import com.glide.shared.health.HealthResponse
import retrofit2.HttpException
import javax.inject.Inject

interface HealthRepository {
    suspend fun health(): ApiResult<HealthResponse>
}

class NetworkHealthRepository
    @Inject
    constructor(
        private val api: GlideApi,
    ) : HealthRepository {
        override suspend fun health(): ApiResult<HealthResponse> =
            apiCall {
                val response = api.health()
                val body = response.body()
                when {
                    response.isSuccessful && body != null -> {
                        body
                    }

                    // 503 from our backend still carries a HealthResponse (database DOWN). A 503 from anything
                    // else (proxy, load balancer) won't parse, and is reported as a plain HTTP error.
                    response.code() == SERVICE_UNAVAILABLE -> {
                        response.errorBody()?.string()?.let {
                            runCatching {
                                ApiJson.decodeFromString<HealthResponse>(
                                    it,
                                )
                            }.getOrNull()
                        }
                            ?: throw HttpException(response)
                    }

                    else -> {
                        throw HttpException(response)
                    }
                }
            }

        private companion object {
            const val SERVICE_UNAVAILABLE = 503
        }
    }
