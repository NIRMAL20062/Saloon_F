package com.glide.android.data.network

import com.glide.shared.api.ApiJson
import com.glide.shared.error.ErrorResponse
import kotlinx.coroutines.CancellationException
import kotlinx.serialization.SerializationException
import retrofit2.HttpException
import java.io.IOException

/** Result of a backend call. Repositories return this; they never throw network errors to the UI. */
sealed interface ApiResult<out T> {
    data class Success<T>(
        val data: T,
    ) : ApiResult<T>

    data class Failure(
        val error: ApiError,
    ) : ApiResult<Nothing>
}

/** Why a call failed. The UI maps these to user-friendly text; raw server text is never shown. */
sealed interface ApiError {
    /** No connection, DNS failure, timeout. Usually worth a Retry button. */
    data object Network : ApiError

    /**
     * The backend answered with an error status.
     * [code] is the stable `ErrorCodes` value from the error envelope (null if the body wasn't one).
     * [requestId] identifies the call in server logs.
     */
    data class Http(
        val status: Int,
        val code: String?,
        val requestId: String?,
    ) : ApiError

    /** The response didn't match the contract (should never happen; report it). */
    data object Unexpected : ApiError
}

/** Runs a Retrofit call and turns every failure into an [ApiError]. */
suspend fun <T> apiCall(block: suspend () -> T): ApiResult<T> =
    try {
        ApiResult.Success(block())
    } catch (e: CancellationException) {
        throw e
    } catch (e: HttpException) {
        ApiResult.Failure(e.toApiError())
    } catch (_: IOException) {
        ApiResult.Failure(ApiError.Network)
    } catch (_: SerializationException) {
        ApiResult.Failure(ApiError.Unexpected)
    }

internal fun HttpException.toApiError(): ApiError.Http {
    val envelope =
        runCatching { response()?.errorBody()?.string()?.let { ApiJson.decodeFromString<ErrorResponse>(it) } }
            .getOrNull()
    return ApiError.Http(
        status = code(),
        code = envelope?.error?.code,
        requestId = envelope?.error?.requestId ?: response()?.headers()?.get(REQUEST_ID_HEADER),
    )
}
