package com.glide.backend

import com.glide.backend.admins.AuthAdmin
import com.glide.backend.admins.AuthAdminException
import com.glide.backend.admins.InviteResult
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.CopyOnWriteArrayList

/**
 * Stands in for Supabase's admin API: keeps logins by email and records which invite emails "went out".
 * Set [failWith] to make every call fail like Supabase being down.
 */
class FakeAuthAdmin : AuthAdmin {
    private val logins = ConcurrentHashMap<String, UUID>()

    /** Emails Supabase would have sent an invite to. */
    val invitesSent = CopyOnWriteArrayList<String>()

    var failWith: AuthAdminException? = null

    /** An email that already has a Supabase login (e.g. someone who signed up before), so invite says email_exists. */
    fun existingLogin(email: String): UUID = UUID.randomUUID().also { logins[email] = it }

    fun loginOf(email: String): UUID? = logins[email]

    override suspend fun invite(email: String): InviteResult {
        failWith?.let { throw it }
        if (logins.containsKey(email)) return InviteResult.AlreadyRegistered
        invitesSent += email
        return InviteResult.Invited(logins.computeIfAbsent(email) { UUID.randomUUID() })
    }

    override suspend fun findOrCreateUser(email: String): UUID {
        failWith?.let { throw it }
        return logins.computeIfAbsent(email) { UUID.randomUUID() }
    }
}
