package com.glide.backend.health

import com.glide.shared.api.ApiRoutes
import com.glide.shared.health.HealthResponse
import com.glide.shared.health.HealthStatus
import io.ktor.http.HttpStatusCode
import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import io.ktor.server.routing.get

fun Route.healthRoutes(
    version: String,
    databaseHealthCheck: DatabaseHealthCheck,
) {
    get(ApiRoutes.HEALTH_LIVE) {
        call.respond(HealthResponse(status = HealthStatus.UP, version = version))
    }

    get(ApiRoutes.HEALTH) {
        val database = if (databaseHealthCheck.isHealthy()) HealthStatus.UP else HealthStatus.DOWN
        // 503 tells load balancers and uptime monitors to stop sending traffic here.
        val httpStatus = if (database == HealthStatus.UP) HttpStatusCode.OK else HttpStatusCode.ServiceUnavailable
        call.respond(httpStatus, HealthResponse(status = database, version = version, database = database))
    }
}
