package com.glide.backend.plugins

import com.glide.shared.error.ErrorBody
import com.glide.shared.error.ErrorCodes
import com.glide.shared.error.ErrorResponse
import io.ktor.http.HttpStatusCode
import io.ktor.server.application.Application
import io.ktor.server.application.ApplicationCall
import io.ktor.server.application.install
import io.ktor.server.plugins.BadRequestException
import io.ktor.server.plugins.NotFoundException
import io.ktor.server.plugins.PayloadTooLargeException
import io.ktor.server.plugins.UnsupportedMediaTypeException
import io.ktor.server.plugins.callid.callId
import io.ktor.server.plugins.statuspages.StatusPages
import io.ktor.server.response.respond
import org.slf4j.LoggerFactory

private val log = LoggerFactory.getLogger("com.glide.backend.ErrorHandling")

/**
 * Turns every failure into the shared error envelope. Clients never see stack traces,
 * exception messages, SQL or class names: those go to the server log under the request ID.
 */
fun Application.configureErrorHandling() {
    install(StatusPages) {
        exception<BadRequestException> { call, _ ->
            call.respondError(HttpStatusCode.BadRequest, ErrorCodes.BAD_REQUEST, "The request is malformed.")
        }
        exception<NotFoundException> { call, _ ->
            call.respondError(HttpStatusCode.NotFound, ErrorCodes.NOT_FOUND, "Not found.")
        }
        exception<PayloadTooLargeException> { call, _ ->
            call.respondError(
                HttpStatusCode.PayloadTooLarge,
                ErrorCodes.PAYLOAD_TOO_LARGE,
                "Request body is too large.",
            )
        }
        exception<UnsupportedMediaTypeException> { call, _ ->
            call.respondError(
                HttpStatusCode.UnsupportedMediaType,
                ErrorCodes.UNSUPPORTED_MEDIA_TYPE,
                "Send the body as application/json.",
            )
        }
        exception<Throwable> { call, cause ->
            log.error("Unhandled error", cause)
            call.respondError(
                HttpStatusCode.InternalServerError,
                ErrorCodes.INTERNAL,
                "Something went wrong. Quote the request ID when reporting this.",
            )
        }

        status(HttpStatusCode.NotFound) { call, status ->
            call.respondError(status, ErrorCodes.NOT_FOUND, "No such endpoint.")
        }
        status(HttpStatusCode.MethodNotAllowed) { call, status ->
            call.respondError(status, ErrorCodes.METHOD_NOT_ALLOWED, "Method not allowed on this endpoint.")
        }
        status(HttpStatusCode.TooManyRequests) { call, status ->
            call.respondError(status, ErrorCodes.RATE_LIMITED, "Too many requests. Wait a moment and retry.")
        }
    }
}

suspend fun ApplicationCall.respondError(
    status: HttpStatusCode,
    code: String,
    message: String,
) = respond(status, ErrorResponse(ErrorBody(code = code, message = message, requestId = callId)))
