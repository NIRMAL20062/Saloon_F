package com.glide.android.testing

import com.glide.android.data.network.GlideApi
import com.glide.shared.api.ApiJson
import com.glide.shared.error.ErrorBody
import com.glide.shared.error.ErrorResponse
import com.glide.shared.health.HealthResponse
import com.glide.shared.health.HealthStatus
import com.glide.shared.me.MeErrorCodes
import com.glide.shared.me.MeResponse
import com.glide.shared.me.UpdateProfileRequest
import com.glide.shared.me.UpdateSideRequest
import com.glide.shared.me.UserSide
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.ResponseBody.Companion.toResponseBody
import retrofit2.HttpException
import retrofit2.Response
import java.io.IOException

/**
 * A backend in memory that behaves like ours for `/health` and `/v1/me*`: the side is final (409), and the profile is
 * validated the way the server does. Set [offline] to fail every call like a lost connection.
 */
class FakeBackend(
    var me: MeResponse = MeResponse("user-1", "919000000001"),
) : GlideApi {
    var offline = false
    val calls = mutableListOf<String>()

    override suspend fun health(): Response<HealthResponse> {
        check()
        return Response.success(HealthResponse(HealthStatus.UP, "0.1.0", HealthStatus.UP))
    }

    override suspend fun me(): MeResponse {
        calls += "me"
        check()
        return me
    }

    override suspend fun chooseSide(body: UpdateSideRequest): MeResponse {
        calls += "side:${body.side}"
        check()
        if (me.side != null && me.side != body.side) throw httpError(409, MeErrorCodes.SIDE_ALREADY_CHOSEN)
        me = me.copy(side = body.side)
        return me
    }

    override suspend fun updateProfile(body: UpdateProfileRequest): MeResponse {
        calls += "profile:${body.name}:${body.email}"
        check()
        val name = body.name.trim()
        if (name.length !in 2..60) throw httpError(400, MeErrorCodes.INVALID_NAME)
        me = me.copy(name = name, email = body.email?.trim()?.lowercase())
        return me
    }

    private fun check() {
        if (offline) throw IOException("offline")
    }

    companion object {
        fun httpError(
            status: Int,
            code: String,
        ): HttpException {
            val body = ApiJson.encodeToString(ErrorResponse(ErrorBody(code, "message", "req-1")))
            return HttpException(Response.error<Any>(status, body.toResponseBody("application/json".toMediaType())))
        }

        /** A customer who finished onboarding. */
        val CUSTOMER = MeResponse("user-1", "919000000001", UserSide.CUSTOMER, "Test Customer")
    }
}
