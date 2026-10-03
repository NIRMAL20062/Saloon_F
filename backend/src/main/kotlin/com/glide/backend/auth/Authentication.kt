package com.glide.backend.auth

import io.ktor.server.application.Application
import io.ktor.server.application.ApplicationCall
import io.ktor.server.application.install
import io.ktor.server.auth.Authentication
import io.ktor.server.auth.bearer
import io.ktor.server.auth.principal

/** Name of the auth provider for routes that need a signed-in user: `authenticate(USER_AUTH) { ... }`. */
const val USER_AUTH = "user"

fun Application.configureAuthentication(verifier: TokenVerifier) {
    install(Authentication) {
        bearer(USER_AUTH) {
            realm = "glide"
            authenticate { credential -> verifier.verify(credential.token) }
        }
    }
}

/** The signed-in user. Only call inside `authenticate(USER_AUTH) { }`, where Ktor guarantees one exists. */
fun ApplicationCall.user(): AuthenticatedUser =
    checkNotNull(principal<AuthenticatedUser>()) {
        "route is missing authenticate(USER_AUTH)"
    }
