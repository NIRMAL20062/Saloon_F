package com.glide.backend.admins

import com.glide.backend.db.Transactor
import com.glide.backend.validation.Emails
import java.util.UUID

/**
 * Our admins (D-013, DF-16): the first one added by a server command, every later one invited by an admin. Each database
 * step is one transaction (BE-025); calls to Supabase happen between them, never while a transaction is open.
 */
class AdminService(
    private val transactor: Transactor,
    private val admins: AdminRepository,
    private val authAdmin: AuthAdmin,
) {
    sealed interface Outcome {
        data class Added(
            val admin: Admin,
        ) : Outcome

        data object InvalidEmail : Outcome

        /** Invite: the email is already an admin. First admin: there already is one. */
        data object AlreadyExists : Outcome

        /** No Supabase secret key configured. */
        data object Unavailable : Outcome

        /** Supabase refused or didn't answer. Nothing was saved. */
        data object SupabaseFailed : Outcome
    }

    suspend fun find(userId: UUID): Admin? = transactor.transaction { admins.find(userId) }

    /** INVITED → ACTIVE on the admin's first request with the authenticator app, audit row included. */
    suspend fun activate(
        userId: UUID,
        requestId: String?,
    ): Admin = transactor.transaction { admins.activate(userId, requestId) }

    /**
     * [inviter] invites [rawEmail]. Supabase creates the login and sends its invite email; if the email already has a
     * login, it is looked up instead (no email: they log in to the admin website with their email as usual). Then the
     * admin is saved as INVITED and the invite audit-logged.
     */
    suspend fun invite(
        inviter: Admin,
        rawEmail: String,
        requestId: String?,
    ): Outcome {
        val email = adminEmail(rawEmail) ?: return Outcome.InvalidEmail
        if (transactor.transaction { admins.findByEmail(email) } != null) return Outcome.AlreadyExists
        val userId =
            try {
                when (val result = authAdmin.invite(email)) {
                    is InviteResult.Invited -> result.userId
                    InviteResult.AlreadyRegistered -> authAdmin.findOrCreateUser(email)
                }
            } catch (_: AuthAdminUnavailableException) {
                return Outcome.Unavailable
            } catch (_: AuthAdminException) {
                return Outcome.SupabaseFailed
            }
        return transactor
            .transaction {
                admins.addInvited(
                    userId,
                    email,
                    inviter.userId,
                    requestId,
                )
            }?.let { Outcome.Added(it) }
            ?: Outcome.AlreadyExists
    }

    /** The one-off command (BE-020): makes [rawEmail] the first admin, without sending any email. */
    suspend fun addFirst(rawEmail: String): Outcome {
        val email = adminEmail(rawEmail) ?: return Outcome.InvalidEmail
        val userId =
            try {
                authAdmin.findOrCreateUser(email)
            } catch (_: AuthAdminUnavailableException) {
                return Outcome.Unavailable
            } catch (_: AuthAdminException) {
                return Outcome.SupabaseFailed
            }
        return transactor.transaction { admins.addFirst(userId, email) }?.let { Outcome.Added(it) }
            ?: Outcome.AlreadyExists
    }

    /**
     * Admin emails are plain ASCII: Kotlin and PostgreSQL lower-case some other letters differently, which would trip
     * the database's lower-case check (a 500 whose log line would contain the email).
     */
    private fun adminEmail(raw: String): String? =
        Emails.normalize(raw)?.takeIf { email ->
            email.all {
                it.code in
                    0x21..0x7E
            }
        }
}
