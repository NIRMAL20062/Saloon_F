package com.glide.shared.api

/** URL paths of the backend API. Backend routes and Android Retrofit interfaces both reference these. */
object ApiRoutes {
    /** Liveness: the process is up. No dependencies checked. */
    const val HEALTH_LIVE = "/health/live"

    /** Readiness: the process is up **and** its dependencies (database) respond. */
    const val HEALTH = "/health"

    /** Prefix for every business endpoint, e.g. `/v1/salons`. Bump only for breaking changes. */
    const val V1 = "/v1"
}
