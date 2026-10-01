package com.glide.shared.health

import kotlinx.serialization.Serializable

@Serializable
enum class HealthStatus { UP, DOWN }

/** Body of `GET /health` and `GET /health/live`. */
@Serializable
data class HealthResponse(
    /** Overall status. `UP` only if every checked dependency is `UP`. */
    val status: HealthStatus,
    /** Backend build version, e.g. `0.1.0`. */
    val version: String,
    /** Database status. Absent on `/health/live`, which doesn't touch the database. */
    val database: HealthStatus? = null,
)
