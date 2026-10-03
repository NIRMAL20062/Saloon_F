package com.glide.backend.users

import com.glide.backend.auth.USER_AUTH
import com.glide.backend.auth.user
import com.glide.backend.plugins.respondError
import com.glide.shared.api.ApiRoutes
import com.glide.shared.me.MeErrorCodes
import com.glide.shared.me.MeResponse
import com.glide.shared.me.UpdateProfileRequest
import com.glide.shared.me.UpdateSideRequest
import io.ktor.http.HttpStatusCode
import io.ktor.server.auth.authenticate
import io.ktor.server.request.receive
import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import io.ktor.server.routing.get
import io.ktor.server.routing.put

fun Route.meRoutes(users: UserRepository) {
    authenticate(USER_AUTH) {
        // Who is signed in, and which side of the app to show them (D-024).
        get(ApiRoutes.ME) {
            call.respond(users.ensure(call.user()).toResponse())
        }

        // The person's own name and optional email (BE-018). Only ever their own row: the id comes from the token.
        put(ApiRoutes.ME_PROFILE) {
            val request = call.receive<UpdateProfileRequest>()
            when (val input = ProfileInput.from(request)) {
                is ProfileInput.Invalid -> {
                    call.respondError(HttpStatusCode.BadRequest, input.code, input.message)
                }

                is ProfileInput.Valid -> {
                    val user = users.ensure(call.user())
                    call.respond(users.updateProfile(user.id, input).toResponse())
                }
            }
        }

        // Onboarding answer: "customer or salon?". Final once chosen (D-030); the same answer again is fine.
        put(ApiRoutes.ME_SIDE) {
            val request = call.receive<UpdateSideRequest>()
            val user = users.ensure(call.user())
            val saved = users.chooseSide(user.id, request.side)
            if (saved == null) {
                call.respondError(
                    HttpStatusCode.Conflict,
                    MeErrorCodes.SIDE_ALREADY_CHOSEN,
                    "Your choice is already saved and can't be changed.",
                )
            } else {
                call.respond(saved.toResponse())
            }
        }
    }
}

private fun AppUser.toResponse() =
    MeResponse(id = id.toString(), phone = phone, side = side, name = name, email = email)
