package com.glide.android.domain.status

import com.glide.android.data.health.HealthRepository
import com.glide.android.data.network.ApiResult
import com.glide.shared.health.HealthStatus
import javax.inject.Inject

/** What the app knows about the backend's state. */
data class SystemStatus(
    val server: HealthStatus,
    val database: HealthStatus,
    val version: String,
)

class GetSystemStatusUseCase
    @Inject
    constructor(
        private val repository: HealthRepository,
    ) {
        suspend operator fun invoke(): ApiResult<SystemStatus> =
            when (val result = repository.health()) {
                is ApiResult.Failure -> {
                    result
                }

                is ApiResult.Success -> {
                    ApiResult.Success(
                        SystemStatus(
                            // We got an answer, so the server process itself is up, even if the database isn't.
                            server = HealthStatus.UP,
                            database = result.data.database ?: HealthStatus.DOWN,
                            version = result.data.version,
                        ),
                    )
                }
            }
    }
