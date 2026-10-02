package com.glide.backend

import com.glide.backend.auth.AuthenticatedUser
import com.glide.backend.users.AppUser
import com.glide.backend.users.UserRepository
import com.glide.shared.me.UserSide
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap

/** For route tests that don't touch the database. Real behaviour is tested against Postgres in UserRepositoryTest. */
class InMemoryUserRepository : UserRepository {
    private val users = ConcurrentHashMap<UUID, AppUser>()

    override suspend fun ensure(user: AuthenticatedUser): AppUser =
        users.compute(user.id) { _, existing ->
            existing?.copy(phone = user.phone ?: existing.phone) ?: AppUser(user.id, user.phone, null)
        }!!

    override suspend fun setSide(
        id: UUID,
        side: UserSide,
    ): AppUser = users.computeIfPresent(id) { _, existing -> existing.copy(side = side) }!!
}
