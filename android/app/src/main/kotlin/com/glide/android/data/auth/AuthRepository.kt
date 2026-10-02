package com.glide.android.data.auth

import com.glide.shared.api.ApiJson
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import retrofit2.Response
import java.io.IOException
import javax.inject.Inject
import javax.inject.Singleton

sealed interface AuthResult<out T> {
    data class Success<T>(
        val value: T,
    ) : AuthResult<T>

    data class Failure(
        val error: AuthError,
    ) : AuthResult<Nothing>
}

/** Why a login step failed. The UI shows its own wording for each; Supabase's text is never shown. */
enum class AuthError { INVALID_PHONE, INVALID_CODE, RATE_LIMITED, NETWORK, UNEXPECTED }

/**
 * Phone login with Supabase Auth (D-016): send OTP → verify → session kept encrypted on the phone and refreshed
 * automatically. Everything else in the app asks [accessToken] for a valid token.
 */
@Singleton
class AuthRepository
    @Inject
    constructor(
        private val api: SupabaseAuthApi,
        private val store: SessionStore,
        private val clock: EpochClock,
    ) {
        private val _session = MutableStateFlow(store.load())

        /** Null = signed out. */
        val session: StateFlow<Session?> = _session.asStateFlow()

        private val refreshLock = Mutex()

        /** [phone] in E.164 digits without "+", e.g. `919000000001`. */
        suspend fun requestOtp(phone: String): AuthResult<Unit> =
            call(onClientError = AuthError.INVALID_PHONE) { api.sendOtp(OtpRequest(phone)) }.map { }

        suspend fun verifyOtp(
            phone: String,
            code: String,
        ): AuthResult<Session> =
            call(onClientError = AuthError.INVALID_CODE) { api.verifyOtp(VerifyRequest(phone, code)) }
                .map { response -> save(response) }

        /** A token that's valid for at least another minute, refreshing it first if needed; null when signed out. */
        suspend fun accessToken(): String? {
            val current = _session.value ?: return null
            if (current.expiresAtEpochSeconds - clock.nowEpochSeconds() >
                REFRESH_MARGIN_SECONDS
            ) {
                return current.accessToken
            }
            return refresh(staleToken = current.accessToken)
        }

        /**
         * Gets a new access token with the refresh token. [staleToken] is the token that just failed or expired: if another
         * request already refreshed it, the new one is returned without a second refresh. Returns null and signs out when
         * the refresh token is no longer accepted; keeps the session on network errors.
         */
        suspend fun refresh(staleToken: String?): String? =
            refreshLock.withLock {
                val current = _session.value ?: return null
                if (staleToken != null && current.accessToken != staleToken) return current.accessToken
                // A 4xx here means Supabase no longer accepts this refresh token (revoked, signed out elsewhere).
                val result =
                    call(onClientError = AuthError.INVALID_CODE) { api.refresh(RefreshRequest(current.refreshToken)) }
                when (result) {
                    is AuthResult.Success -> {
                        save(result.value).accessToken
                    }

                    is AuthResult.Failure -> {
                        if (result.error == AuthError.INVALID_CODE) signOutLocally()
                        null
                    }
                }
            }

        /** Ends the session on this phone; also tells Supabase, but signs out even if that fails (e.g. offline). */
        suspend fun logout() {
            val current = _session.value ?: return
            runCatching { api.logout("Bearer ${current.accessToken}") }
            signOutLocally()
        }

        private fun signOutLocally() {
            store.clear()
            _session.value = null
        }

        private fun save(response: SessionResponse): Session {
            val session =
                Session(
                    accessToken = response.accessToken,
                    refreshToken = response.refreshToken,
                    expiresAtEpochSeconds = clock.nowEpochSeconds() + response.expiresIn,
                    userId = response.user.id,
                    phone = response.user.phone,
                )
            store.save(session)
            _session.value = session
            return session
        }

        private suspend fun <T> call(
            onClientError: AuthError,
            block: suspend () -> Response<T>,
        ): AuthResult<T> =
            try {
                val response = block()
                val body = response.body()
                when {
                    response.isSuccessful -> {
                        @Suppress("UNCHECKED_CAST")
                        AuthResult.Success(body ?: Unit as T)
                    }

                    else -> {
                        AuthResult.Failure(classify(response, onClientError))
                    }
                }
            } catch (e: CancellationException) {
                throw e
            } catch (_: IOException) {
                AuthResult.Failure(AuthError.NETWORK)
            } catch (_: RuntimeException) {
                AuthResult.Failure(AuthError.UNEXPECTED)
            }

        private fun classify(
            response: Response<*>,
            onClientError: AuthError,
        ): AuthError {
            val code =
                runCatching {
                    response.errorBody()?.string()?.let { ApiJson.decodeFromString<SupabaseError>(it).errorCode }
                }.getOrNull()
            return when {
                response.code() == TOO_MANY_REQUESTS || code?.contains("rate_limit") == true -> AuthError.RATE_LIMITED
                response.code() in CLIENT_ERRORS -> onClientError
                else -> AuthError.UNEXPECTED
            }
        }

        private companion object {
            const val REFRESH_MARGIN_SECONDS = 60L
            const val TOO_MANY_REQUESTS = 429
            val CLIENT_ERRORS = 400..499
        }
    }

private inline fun <T, R> AuthResult<T>.map(transform: (T) -> R): AuthResult<R> =
    when (this) {
        is AuthResult.Success -> AuthResult.Success(transform(value))
        is AuthResult.Failure -> this
    }
