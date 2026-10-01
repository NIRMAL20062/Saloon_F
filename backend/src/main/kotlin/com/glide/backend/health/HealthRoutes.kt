package com.glide.backend.health

import com.glide.shared.api.ApiRoutes
import com.glide.shared.health.HealthResponse
import com.glide.shared.health.HealthStatus
import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import io.ktor.server.routing.get

fun Route.healthRoutes(version: String) {
    get(ApiRoutes.HEALTH_LIVE) {
        call.respond(HealthResponse(status = HealthStatus.UP, version = version))
    }
}
