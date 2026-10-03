package com.glide.android.data.auth

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.Header
import retrofit2.http.POST
import retrofit2.http.Query

/**
 * Supabase Auth (GoTrue) endpoints the app uses for phone login (D-016). These models are Supabase's, not our backend's,
 * so they live here and not in `:shared`. Every call carries the public `apikey` header (added by an interceptor).
 */
interface SupabaseAuthApi {
    /** Sends an OTP by SMS (test numbers: nothing is sent, the fixed code works). */
    @POST("auth/v1/otp")
    suspend fun sendOtp(
        @Body body: OtpRequest,
    ): Response<Unit>

    @POST("auth/v1/verify")
    suspend fun verifyOtp(
        @Body body: VerifyRequest,
    ): Response<SessionResponse>

    @POST("auth/v1/token")
    suspend fun refresh(
        @Body body: RefreshRequest,
        @Query("grant_type") grantType: String = "refresh_token",
    ): Response<SessionResponse>

    @POST("auth/v1/logout")
    suspend fun logout(
        @Header("Authorization") bearer: String,
    ): Response<Unit>
}

@Serializable
data class OtpRequest(
    val phone: String,
)

@Serializable
data class VerifyRequest(
    val phone: String,
    val token: String,
    val type: String = "sms",
)

@Serializable
data class RefreshRequest(
    @SerialName("refresh_token") val refreshToken: String,
)

@Serializable
data class SessionResponse(
    @SerialName("access_token") val accessToken: String,
    @SerialName("refresh_token") val refreshToken: String,
    @SerialName("expires_in") val expiresIn: Long,
    val user: SupabaseUser,
)

@Serializable
data class SupabaseUser(
    val id: String,
    val phone: String? = null,
)

/** Error body Supabase returns, e.g. `{"code":403,"error_code":"otp_expired","msg":"Token has expired or is invalid"}`. */
@Serializable
data class SupabaseError(
    @SerialName("error_code") val errorCode: String? = null,
)
