package com.glide.android.data.network

import com.glide.shared.api.ApiJson
import okhttp3.Interceptor
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Response
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.kotlinx.serialization.asConverterFactory
import java.util.UUID
import java.util.concurrent.TimeUnit

const val REQUEST_ID_HEADER = "X-Request-Id"

/**
 * Builds the one OkHttp client the app uses. Plain functions (not Hilt) so tests exercise the exact same setup.
 * Body logging is on only in debug builds; the Authorization header is always redacted.
 */
fun createOkHttpClient(debugLogging: Boolean): OkHttpClient =
    OkHttpClient
        .Builder()
        .connectTimeout(10, TimeUnit.SECONDS)
        .readTimeout(20, TimeUnit.SECONDS)
        .addInterceptor(RequestIdInterceptor())
        .apply {
            if (debugLogging) {
                addInterceptor(
                    HttpLoggingInterceptor().apply {
                        level = HttpLoggingInterceptor.Level.BODY
                        redactHeader("Authorization")
                    },
                )
            }
        }.build()

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
