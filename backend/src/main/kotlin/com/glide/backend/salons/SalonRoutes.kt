package com.glide.backend.salons

import com.glide.backend.auth.USER_AUTH
import com.glide.backend.auth.user
import com.glide.backend.plugins.respondError
import com.glide.shared.api.ApiRoutes
import com.glide.shared.salon.MySalonResponse
import com.glide.shared.salon.SalonAddress
import com.glide.shared.salon.SalonErrorCodes
import com.glide.shared.salon.SalonProfileRequest
import com.glide.shared.salon.SalonResponse
import io.ktor.http.HttpStatusCode
import io.ktor.server.application.ApplicationCall
import io.ktor.server.auth.authenticate
import io.ktor.server.request.receive
import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import io.ktor.server.routing.get
import io.ktor.server.routing.post
import io.ktor.server.routing.put

/** Salon side (D-017): the salon is the signed-in person's one salon (D-036); no route takes a salon id. */
fun Route.salonRoutes(salons: SalonService) {
    authenticate(USER_AUTH) {
        // Create my salon; I become its owner (BE-017). 201 with the salon, DRAFT.
        post(ApiRoutes.SALON_SALONS) {
            val profile = call.validProfile() ?: return@post
            call.respondOutcome(salons.create(call.user(), profile), HttpStatusCode.Created)
        }

        // My salon and my role in it.
        get(ApiRoutes.SALON_ME) {
            call.respondOutcome(salons.mine(call.user()), HttpStatusCode.OK)
        }

        // The owner edits the profile while the salon isn't live.
        put(ApiRoutes.SALON_PROFILE) {
            val profile = call.validProfile() ?: return@put
            call.respondOutcome(salons.updateProfile(call.user(), profile), HttpStatusCode.OK)
        }
    }
}

/** The checked profile, or null after answering 400. */
private suspend fun ApplicationCall.validProfile(): SalonInput.Valid? =
    when (val input = SalonInput.from(receive<SalonProfileRequest>())) {
        is SalonInput.Valid -> {
            input
        }

        is SalonInput.Invalid -> {
            respondError(HttpStatusCode.BadRequest, input.code, input.message)
            null
        }
    }

private suspend fun ApplicationCall.respondOutcome(
    outcome: SalonService.Outcome,
    success: HttpStatusCode,
) = when (outcome) {
    is SalonService.Outcome.Saved -> {
        respond(success, MySalonResponse(outcome.salon.toResponse(), outcome.role))
    }

    SalonService.Outcome.NotSalonSide -> {
        respondError(
            HttpStatusCode.Forbidden,
            SalonErrorCodes.NOT_SALON_SIDE,
            "Only the salon side of the app can create a salon.",
        )
    }

    SalonService.Outcome.AlreadyInSalon -> {
        respondError(HttpStatusCode.Conflict, SalonErrorCodes.ALREADY_IN_SALON, "You already belong to a salon.")
    }

    SalonService.Outcome.NoSalon -> {
        respondError(HttpStatusCode.NotFound, SalonErrorCodes.NO_SALON, "You don't belong to a salon yet.")
    }

    SalonService.Outcome.NotOwner -> {
        respondError(HttpStatusCode.Forbidden, SalonErrorCodes.NOT_OWNER, "Only the salon's owner can do this.")
    }

    SalonService.Outcome.NotEditable -> {
        respondError(
            HttpStatusCode.Conflict,
            SalonErrorCodes.SALON_NOT_EDITABLE,
            "A live salon's profile can't be changed here.",
        )
    }

    SalonService.Outcome.PhoneNeeded -> {
        respondError(HttpStatusCode.BadRequest, SalonErrorCodes.INVALID_SALON_PHONE, "Give the salon's phone number.")
    }
}

private fun Salon.toResponse() =
    SalonResponse(
        id = id.toString(),
        name = name,
        phone = phone,
        address = SalonAddress(line1, area, landmark, city, state, pincode),
        type = type,
        status = status,
        rejectionReason = rejectionReason,
    )
