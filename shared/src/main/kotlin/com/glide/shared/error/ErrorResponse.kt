package com.glide.shared.error

import kotlinx.serialization.Serializable

/**
 * Body of every non-2xx response: `{"error":{"code":"...","message":"...","requestId":"..."}}`.
 * Clients branch on [ErrorBody.code], never on [ErrorBody.message] (messages may change).
 */
@Serializable
data class ErrorResponse(
    val error: ErrorBody,
)

@Serializable
data class ErrorBody(
    /** Stable machine-readable code, one of [ErrorCodes]. */
    val code: String,
    /** Human-readable explanation. Never contains stack traces, SQL or secrets. */
    val message: String,
    /** Same value as the `X-Request-Id` response header. Quote it when reporting a bug. */
    val requestId: String? = null,
)

/** Error codes shared by every endpoint. Feature-specific codes are added next to their feature. */
object ErrorCodes {
    const val BAD_REQUEST = "BAD_REQUEST"
    const val UNAUTHORIZED = "UNAUTHORIZED"
    const val FORBIDDEN = "FORBIDDEN"
    const val NOT_FOUND = "NOT_FOUND"
    const val METHOD_NOT_ALLOWED = "METHOD_NOT_ALLOWED"
    const val PAYLOAD_TOO_LARGE = "PAYLOAD_TOO_LARGE"
    const val UNSUPPORTED_MEDIA_TYPE = "UNSUPPORTED_MEDIA_TYPE"
    const val RATE_LIMITED = "RATE_LIMITED"
    const val INTERNAL = "INTERNAL"
}
