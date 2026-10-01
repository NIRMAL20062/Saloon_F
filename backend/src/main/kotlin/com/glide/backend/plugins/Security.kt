package com.glide.backend.plugins

import com.glide.backend.config.AppConfig
import com.glide.backend.config.AppEnv
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpMethod
import io.ktor.server.application.Application
import io.ktor.server.application.createApplicationPlugin
import io.ktor.server.application.install
import io.ktor.server.plugins.bodylimit.RequestBodyLimit
import io.ktor.server.plugins.cors.routing.CORS
import io.ktor.server.plugins.origin
import io.ktor.server.plugins.ratelimit.RateLimit
import kotlin.time.Duration.Companion.minutes

/** Largest request body accepted. Raise per-route (e.g. for uploads) rather than globally. */
const val MAX_REQUEST_BODY_BYTES: Long = 1024 * 1024

fun Application.configureSecurity(config: AppConfig) {
    install(SecurityHeaders) {
        hsts = config.env == AppEnv.STAGING || config.env == AppEnv.PRODUCTION
    }

    // No allowed origins = no CORS headers at all = browsers block every cross-origin call.
    if (config.corsAllowedOrigins.isNotEmpty()) {
        install(CORS) {
            config.corsAllowedOrigins.forEach { origin ->
                allowHost(origin.substringAfter("://"), schemes = listOf(origin.substringBefore("://")))
            }
            listOf(HttpMethod.Get, HttpMethod.Post, HttpMethod.Put, HttpMethod.Patch, HttpMethod.Delete)
                .forEach(::allowMethod)
            allowHeader(HttpHeaders.ContentType)
            allowHeader(HttpHeaders.Authorization)
            allowHeader(HttpHeaders.XRequestId)
            exposeHeader(HttpHeaders.XRequestId)
            // Auth uses a bearer token in the Authorization header, never cookies.
            allowCredentials = false
            maxAgeInSeconds = 3600
        }
    }

    install(RateLimit) {
        global {
            rateLimiter(limit = config.rateLimitPerMinute, refillPeriod = 1.minutes)
            // TODO(Q-004): once hosting is chosen, install ForwardedHeaders for that proxy only, so the
            // real client IP is used. Trusting X-Forwarded-For from anyone would let clients dodge limits.
            requestKey { call -> call.request.origin.remoteHost }
        }
    }

    install(RequestBodyLimit) {
        bodyLimit { MAX_REQUEST_BODY_BYTES }
    }
}

class SecurityHeadersConfig {
    var hsts: Boolean = false
}

/** Headers for a JSON-only API: nothing may be framed, sniffed, cached or referred. */
val SecurityHeaders =
    createApplicationPlugin("SecurityHeaders", ::SecurityHeadersConfig) {
        val hsts = pluginConfig.hsts
        onCall { call ->
            with(call.response.headers) {
                append("X-Content-Type-Options", "nosniff")
                append("X-Frame-Options", "DENY")
                append("Referrer-Policy", "no-referrer")
                append("Content-Security-Policy", "default-src 'none'; frame-ancestors 'none'")
                append(HttpHeaders.CacheControl, "no-store")
                if (hsts) append(HttpHeaders.StrictTransportSecurity, "max-age=31536000; includeSubDomains")
            }
        }
    }
