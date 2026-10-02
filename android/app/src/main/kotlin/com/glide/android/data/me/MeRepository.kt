package com.glide.android.data.me

import com.glide.android.data.network.ApiResult
import com.glide.android.data.network.GlideApi
import com.glide.android.data.network.apiCall
import com.glide.shared.me.MeResponse
import javax.inject.Inject

/** The signed-in person as the backend knows them (`GET /v1/me`). */
class MeRepository
    @Inject
    constructor(
        private val api: GlideApi,
    ) {
        suspend fun me(): ApiResult<MeResponse> = apiCall { api.me() }
    }
