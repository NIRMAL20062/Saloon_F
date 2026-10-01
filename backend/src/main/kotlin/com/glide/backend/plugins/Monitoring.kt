package com.glide.backend.plugins

import io.ktor.http.HttpHeaders
import io.ktor.server.application.Application
import io.ktor.server.application.install
import io.ktor.server.plugins.callid.CallId
import io.ktor.server.plugins.callid.callIdMdc
import io.ktor.server.plugins.calllogging.CallLogging
import io.ktor.server.plugins.calllogging.processingTimeMillis
import io.ktor.server.request.header
import io.ktor.server.request.httpMethod
import io.ktor.server.request.path
import org.slf4j.event.Level
import java.util.UUID

fun Application.configureMonitoring() {
    install(CallId) {
        // Reuse the caller's X-Request-Id (so app logs and server logs line up) only if it is safe
        // to put in logs; otherwise generate a fresh one. Never reject a call over a bad request ID.
        retrieve { call -> call.request.header(HttpHeaders.XRequestId)?.takeIf(::isSafeRequestId) }
        generate { UUID.randomUUID().toString() }
        verify(::isSafeRequestId)
        replyToHeader(HttpHeaders.XRequestId)
    }

    install(CallLogging) {
        level = Level.INFO
        callIdMdc("requestId")
        filter { !it.request.path().startsWith("/health") }
        // Path only, never the query string or body: those can carry phone numbers, OTPs or tokens.
        format { call ->
            "${call.response.status()?.value} ${call.request.httpMethod.value} ${call.request.path()} " +
                "${call.processingTimeMillis()}ms"
        }
    }
}

private const val MAX_REQUEST_ID_LENGTH = 64

private fun isSafeRequestId(id: String): Boolean =
    id.length in 1..MAX_REQUEST_ID_LENGTH && id.all { it.isLetterOrDigit() || it == '-' || it == '_' }
