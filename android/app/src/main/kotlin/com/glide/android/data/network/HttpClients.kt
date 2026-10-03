package com.glide.android.data.network

import com.glide.shared.api.ApiJson
import kotlinx.coroutines.runBlocking
import okhttp3.Authenticator
import okhttp3.Interceptor
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import okhttp3.Route
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.kotlinx.serialization.asConverterFactory
import java.util.UUID
import java.util.concurrent.TimeUnit

const val REQUEST_ID_HEADER = "X-Request-Id"

/** Where backend calls get the signed-in user's token. Implemented by AuthRepository. */
interface AccessTokens {
    /** A valid token, or null when signed out. */
    suspend fun current(): String?

    /** The backend rejected [rejected]: get a fresh token (null = can't, the user is signed out or offline). */
    suspend fun refreshAfterRejection(rejected: String?): String?
}

/**
 * Builds the OkHttp clients the app uses. Plain functions (not Hilt) so tests exercise the exact same setup.
 * Debug builds log only method, URL, status and timing: bodies would contain OTPs, tokens and phone numbers.
 * With [tokens], every call carries `Authorization: Bearer <token>` and a 401 triggers one refresh + retry.
 */
fun createOkHttpClient(
    debugLogging: Boolean,
    tokens: AccessTokens? = null,
): OkHttpClient =
    OkHttpClient
        .Builder()
        .connectTimeout(10, TimeUnit.SECONDS)
        .readTimeout(20, TimeUnit.SECONDS)
        .addInterceptor(RequestIdInterceptor())
        .apply {
            if (tokens != null) {
                addInterceptor(BearerTokenInterceptor(tokens))
                authenticator(RefreshOnUnauthorized(tokens))
            }
            if (debugLogging) {
                addInterceptor(
                    HttpLoggingInterceptor().apply {
                        level = HttpLoggingInterceptor.Level.BASIC
                        redactHeader("Authorization")
                    },
                )
            }
        }.build()

/** Adds the signed-in user's token. OkHttp runs interceptors on a background thread, so blocking here is fine. */
class BearerTokenInterceptor(
    private val tokens: AccessTokens,
) : Interceptor {
    override fun intercept(chain: Interceptor.Chain): Response {
        val token = runBlocking { tokens.current() } ?: return chain.proceed(chain.request())
        return chain.proceed(
            chain
                .request()
                .newBuilder()
                .header(AUTHORIZATION, "Bearer $token")
                .build(),
        )
    }
}

/** On 401, refresh the token once and retry; give up if that doesn't help (never loops). */
class RefreshOnUnauthorized(
    private val tokens: AccessTokens,
) : Authenticator {
    override fun authenticate(
        route: Route?,
        response: Response,
    ): Request? {
        if (response.priorResponse != null) return null
        val rejected = response.request.header(AUTHORIZATION)?.removePrefix("Bearer ")
        val fresh = runBlocking { tokens.refreshAfterRejection(rejected) } ?: return null
        return response.request
            .newBuilder()
            .header(AUTHORIZATION, "Bearer $fresh")
            .build()
    }
}

private const val AUTHORIZATION = "Authorization"

fun createRetrofit(
    baseUrl: String,
    client: OkHttpClient,
): Retrofit =
    Retrofit
        .Builder()
        .baseUrl(baseUrl)
        .client(client)
        .addConverterFactory(ApiJson.asConverterFactory("application/json".toMediaType()))
        .build()

/** Tags every call so the app's logs and the backend's logs can be matched by request ID. */
class RequestIdInterceptor : Interceptor {
    override fun intercept(chain: Interceptor.Chain): Response =
        chain.proceed(
            chain
                .request()
                .newBuilder()
                .header(REQUEST_ID_HEADER, "android-${UUID.randomUUID()}")
                .build(),
        )
}
