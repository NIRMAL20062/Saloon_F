package com.glide.backend.admins

import com.glide.backend.plugins.respondError
import com.glide.shared.admin.AdminErrorCodes
import com.glide.shared.admin.AdminResponse
import com.glide.shared.admin.InviteAdminRequest
import com.glide.shared.api.ApiRoutes
import io.ktor.http.HttpStatusCode
import io.ktor.server.plugins.callid.callId
import io.ktor.server.request.receive
import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import io.ktor.server.routing.get
import io.ktor.server.routing.post

fun Route.adminRoutes(
    admins: AdminRepository,
    service: AdminService,
) {
    adminOnly(admins) {
        // Who is signed in to the admin website. Also how the website checks "admin + MFA done" after login (WEB-005).
        get(ApiRoutes.ADMIN_ME) {
            call.respond(call.admin().toResponse())
        }

        // Invite another admin by email (D-013): Supabase sends the invite; they log in with email code + authenticator app.
        post(ApiRoutes.ADMIN_INVITES) {
            val request = call.receive<InviteAdminRequest>()
            when (val outcome = service.invite(call.admin(), request.email, call.callId)) {
                is AdminService.Outcome.Added -> {
                    call.respond(HttpStatusCode.Created, outcome.admin.toResponse())
                }

                AdminService.Outcome.InvalidEmail -> {
                    call.respondError(
                        HttpStatusCode.BadRequest,
                        AdminErrorCodes.INVALID_EMAIL,
                        "Enter a valid email address.",
                    )
                }

                AdminService.Outcome.AlreadyExists -> {
                    call.respondError(
                        HttpStatusCode.Conflict,
                        AdminErrorCodes.ADMIN_ALREADY_EXISTS,
                        "This email is already an admin.",
                    )
                }

                AdminService.Outcome.SupabaseFailed -> {
                    call.respondError(
                        HttpStatusCode.BadGateway,
                        AdminErrorCodes.INVITE_FAILED,
                        "The invite couldn't be sent. Nothing was saved; try again in a minute.",
                    )
                }

                AdminService.Outcome.Unavailable -> {
                    call.respondError(
                        HttpStatusCode.ServiceUnavailable,
                        AdminErrorCodes.INVITES_UNAVAILABLE,
                        "Invites are switched off on this server.",
                    )
                }
            }
        }
    }
}

private fun Admin.toResponse() = AdminResponse(id = userId.toString(), email = email, status = status)
