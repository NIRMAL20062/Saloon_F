package com.glide.backend

import com.glide.backend.admins.Admin
import com.glide.backend.admins.AdminRepository
import com.glide.shared.admin.AdminStatus
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap

/** For route tests that don't touch the database. Real behaviour is tested against Postgres in AdminRepositoryTest. */
class InMemoryAdminRepository : AdminRepository {
    private val admins = ConcurrentHashMap<UUID, Admin>()

    override suspend fun find(userId: UUID): Admin? = admins[userId]

    override suspend fun findByEmail(email: String): Admin? = admins.values.firstOrNull { it.email == email }

    override suspend fun activate(
        userId: UUID,
        requestId: String?,
    ): Admin = admins.computeIfPresent(userId) { _, admin -> admin.copy(status = AdminStatus.ACTIVE) }!!

    override suspend fun addFirst(
        userId: UUID,
        email: String,
    ): Admin? = add(userId, email, null)

    override suspend fun addInvited(
        userId: UUID,
        email: String,
        invitedBy: UUID,
        requestId: String?,
    ): Admin? = add(userId, email, invitedBy)

    @Synchronized
    private fun add(
        userId: UUID,
        email: String,
        invitedBy: UUID?,
    ): Admin? {
        if (invitedBy == null && admins.values.any { it.invitedBy == null }) return null
        if (admins.containsKey(userId) || admins.values.any { it.email == email }) return null
        return Admin(userId, email, AdminStatus.INVITED, invitedBy).also { admins[userId] = it }
    }
}
