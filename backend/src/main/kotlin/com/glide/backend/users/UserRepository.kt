package com.glide.backend.users

import com.glide.backend.auth.AuthenticatedUser
import com.glide.shared.me.UserSide
import org.jetbrains.exposed.v1.core.ResultRow
import org.jetbrains.exposed.v1.core.Table
import org.jetbrains.exposed.v1.core.and
import org.jetbrains.exposed.v1.core.eq
import org.jetbrains.exposed.v1.core.isNull
import org.jetbrains.exposed.v1.core.or
import org.jetbrains.exposed.v1.jdbc.insertIgnore
import org.jetbrains.exposed.v1.jdbc.selectAll
import org.jetbrains.exposed.v1.jdbc.update
import java.util.UUID
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid
import kotlin.uuid.toJavaUuid
import kotlin.uuid.toKotlinUuid

/** Mirrors V2__app_users.sql + V4__app_users_profile.sql. created_at/updated_at are filled by the database. */
@OptIn(ExperimentalUuidApi::class)
object AppUsers : Table("app_users") {
    val id = uuid("id")
    val phone = text("phone").nullable()
    val side = enumerationByName<UserSide>("side", SIDE_MAX_LENGTH).nullable()
    val name = text("name").nullable()
    val email = text("email").nullable()

    override val primaryKey = PrimaryKey(id)
}

private const val SIDE_MAX_LENGTH = 16

data class AppUser(
    val id: UUID,
    val phone: String?,
    val side: UserSide?,
    val name: String? = null,
    val email: String? = null,
)

/** Runs inside the caller's transaction ([com.glide.backend.db.Transactor], BE-025); never opens its own. */
interface UserRepository {
    /** Returns the user's row, creating it on their first request. Safe when two first requests race. */
    fun ensure(user: AuthenticatedUser): AppUser

    /**
     * Saves the onboarding choice. Returns the user, or null when a *different* side was already chosen (the choice is
     * final, D-030). Choosing the same side again is accepted, so app retries are safe.
     */
    fun chooseSide(
        id: UUID,
        side: UserSide,
    ): AppUser?

    /** Saves this person's own name and email (already validated, see [ProfileInput]). A null [email] clears it. */
    fun updateProfile(
        id: UUID,
        profile: ProfileInput.Valid,
    ): AppUser
}

@OptIn(ExperimentalUuidApi::class)
class ExposedUserRepository : UserRepository {
    override fun ensure(user: AuthenticatedUser): AppUser {
        val id = user.id.toKotlinUuid()
        // INSERT ... ON CONFLICT DO NOTHING: parallel first requests can't create two rows.
        AppUsers.insertIgnore {
            it[AppUsers.id] = id
            it[phone] = user.phone
        }
        val row = find(id)
        // Keep the phone in sync if the person changed it in Supabase.
        return if (user.phone != null && row[AppUsers.phone] != user.phone) {
            AppUsers.update({ AppUsers.id eq id }) { it[phone] = user.phone }
            find(id).toAppUser()
        } else {
            row.toAppUser()
        }
    }

    override fun chooseSide(
        id: UUID,
        side: UserSide,
    ): AppUser? {
        val kid = id.toKotlinUuid()
        // Only rows with no side yet (or the same side) match, so a switch updates nothing and never hits the trigger.
        val updated =
            AppUsers.update({ (AppUsers.id eq kid) and (AppUsers.side.isNull() or (AppUsers.side eq side)) }) {
                it[AppUsers.side] = side
            }
        return if (updated == 1) find(kid).toAppUser() else null
    }

    override fun updateProfile(
        id: UUID,
        profile: ProfileInput.Valid,
    ): AppUser {
        val kid = id.toKotlinUuid()
        AppUsers.update({ AppUsers.id eq kid }) {
            it[name] = profile.name
            it[email] = profile.email
        }
        return find(kid).toAppUser()
    }

    private fun find(id: Uuid): ResultRow = AppUsers.selectAll().where { AppUsers.id eq id }.single()

    private fun ResultRow.toAppUser() =
        AppUser(
            this[AppUsers.id].toJavaUuid(),
            this[AppUsers.phone],
            this[AppUsers.side],
            this[AppUsers.name],
            this[AppUsers.email],
        )
}
