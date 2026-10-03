package com.glide.android.data.auth

import android.content.Context
import com.glide.android.BuildConfig
import com.glide.android.data.network.AccessTokens
import com.glide.android.data.network.createOkHttpClient
import com.glide.android.data.network.createRetrofit
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import okhttp3.Interceptor
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object AuthModule {
    @Provides
    @Singleton
    fun supabaseAuthApi(): SupabaseAuthApi =
        createSupabaseAuthApi(
            BuildConfig.SUPABASE_URL,
            BuildConfig.SUPABASE_PUBLISHABLE_KEY,
            debugLogging = BuildConfig.DEBUG,
        )

    @Provides
    fun accessTokens(repository: AuthRepository): AccessTokens = repository

    @Provides
    fun phoneLogin(repository: AuthRepository): PhoneLogin = repository

    @Provides
    @Singleton
    fun sessionStore(
        @ApplicationContext context: Context,
    ): SessionStore =
        EncryptedSessionStore(
            prefs = context.getSharedPreferences("glide_session", Context.MODE_PRIVATE),
            cipher = KeystoreTokenCipher(),
        )

    @Provides
    fun clock(): EpochClock = EpochClock { System.currentTimeMillis() / MILLIS_PER_SECOND }

    private const val MILLIS_PER_SECOND = 1000
}

/** Plain function so tests build the exact same client against a fake server. */
fun createSupabaseAuthApi(
    baseUrl: String,
    publishableKey: String,
    debugLogging: Boolean,
): SupabaseAuthApi {
    val apiKey =
        Interceptor { chain ->
            chain.proceed(
                chain
                    .request()
                    .newBuilder()
                    .header("apikey", publishableKey)
                    .build(),
            )
        }
    val client =
        createOkHttpClient(debugLogging)
            .newBuilder()
            .addInterceptor(apiKey)
            .build()
    return createRetrofit(baseUrl, client).create(SupabaseAuthApi::class.java)
}
