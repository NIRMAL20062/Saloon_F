package com.glide.android.data.network

import com.glide.android.BuildConfig
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object NetworkModule {
    @Provides
    @Singleton
    fun okHttpClient(tokens: AccessTokens): OkHttpClient =
        createOkHttpClient(debugLogging = BuildConfig.DEBUG, tokens = tokens)

    @Provides
    @Singleton
    fun retrofit(client: OkHttpClient): Retrofit = createRetrofit(BuildConfig.API_BASE_URL, client)

    @Provides
    @Singleton
    fun glideApi(retrofit: Retrofit): GlideApi = retrofit.create(GlideApi::class.java)
}
