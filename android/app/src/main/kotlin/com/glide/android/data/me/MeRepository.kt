package com.glide.android.data.me

import com.glide.android.data.network.ApiResult
import com.glide.android.data.network.GlideApi
import com.glide.android.data.network.apiCall
import com.glide.shared.me.MeResponse
import com.glide.shared.me.UpdateProfileRequest
import com.glide.shared.me.UpdateSideRequest
import com.glide.shared.me.UserSide
import javax.inject.Inject

/** The signed-in person as the backend knows them (`GET /v1/me`). */
class MeRepository
    @Inject
    constructor(
        private val api: GlideApi,
    ) {
        suspend fun me(): ApiResult<MeResponse> = apiCall { api.me() }

        suspend fun chooseSide(side: UserSide): ApiResult<MeResponse> =
            apiCall { api.chooseSide(UpdateSideRequest(side)) }

        /** [email] may be null or blank: it's optional and clears any saved one. */
        suspend fun saveProfile(
            name: String,
            email: String?,
        ): ApiResult<MeResponse> = apiCall { api.updateProfile(UpdateProfileRequest(name, email?.ifBlank { null })) }
    }
