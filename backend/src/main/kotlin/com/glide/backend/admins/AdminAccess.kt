package com.glide.backend.admins

import com.glide.backend.auth.AuthenticatedUser
import com.glide.backend.auth.USER_AUTH
import com.glide.backend.plugins.respondError
import com.glide.shared.admin.AdminErrorCodes
import com.glide.shared.admin.AdminStatus
import io.ktor.http.HttpStatusCode
import io.ktor.server.application.ApplicationCall
import io.ktor.server.application.createRouteScopedPlugin
import io.ktor.server.auth.AuthenticationChecked
import io.ktor.server.auth.authenticate
import io.ktor.server.auth.principal
import io.ktor.server.plugins.callid.callId
import io.ktor.server.routing.Route
import io.ktor.util.AttributeKey

class AdminAccessConfig {
    lateinit var admins: AdminRepository
}

private val AdminKey = AttributeKey<Admin>("glide.admin")

/**
 * Lets a request through only for one of our admins (the `admins` table decides, matched on the login's user id) whose
 * login passed the authenticator-app step (Supabase `aal2`, DF-16). Otherwise 403 NOT_ADMIN or 403 MFA_REQUIRED.
 * An INVITED admin becomes ACTIVE on their first request that gets through.
 */
private val AdminAccess =
    createRouteScopedPlugin("AdminAccess", ::AdminAccessConfig) {
        val admins = pluginConfig.admins
        on(AuthenticationChecked) { call ->
            // No principal: the authentication plugin has already answered 401.
            val user = call.principal<AuthenticatedUser>() ?: return@on
            val admin = admins.find(user.id)
            when {
                admin == null -> {
                    call.respondError(
                        HttpStatusCode.Forbidden,
                        AdminErrorCodes.NOT_ADMIN,
                        "This account is not a Glide admin.",
                    )
                }

                !user.mfaVerified -> {
                    call.respondError(
                        HttpStatusCode.Forbidden,
                        AdminErrorCodes.MFA_REQUIRED,
                        "Log in with the code from your authenticator app to continue.",
                    )
                }

                else -> {
                    val current =
                        if (admin.status ==
                            AdminStatus.INVITED
                        ) {
                            admins.activate(admin.userId, call.callId)
                        } else {
                            admin
                        }
                    call.attributes.put(AdminKey, current)
                }
            }
        }
    }

/** Routes inside need a signed-in admin with MFA done: `adminOnly(admins) { get(...) { call.admin() } }`. */
fun Route.adminOnly(
    admins: AdminRepository,
    build: Route.() -> Unit,
) {
    authenticate(USER_AUTH) {
        install(AdminAccess) { this.admins = admins }
        build()
    }
}

/** The admin making this request. Only call inside [adminOnly]. */
fun ApplicationCall.admin(): Admin = attributes[AdminKey]
