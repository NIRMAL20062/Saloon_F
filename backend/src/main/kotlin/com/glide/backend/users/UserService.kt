package com.glide.backend.users

import com.glide.backend.auth.AuthenticatedUser
import com.glide.backend.db.Transactor
import com.glide.shared.me.UserSide

/** The signed-in person's own account (BE-016, BE-018). Each method is one transaction (BE-025). */
class UserService(
    private val transactor: Transactor,
    private val users: UserRepository,
) {
    /** Who is signed in; their row is created on their first request. */
    suspend fun me(user: AuthenticatedUser): AppUser = transactor.transaction { users.ensure(user) }

    /** Saves the onboarding choice. Null when a different side was already chosen (final, D-030). */
    suspend fun chooseSide(
        user: AuthenticatedUser,
        side: UserSide,
    ): AppUser? = transactor.transaction { users.chooseSide(users.ensure(user).id, side) }

    /** Saves this person's own name and email (already validated). */
    suspend fun updateProfile(
        user: AuthenticatedUser,
        profile: ProfileInput.Valid,
    ): AppUser = transactor.transaction { users.updateProfile(users.ensure(user).id, profile) }
}
