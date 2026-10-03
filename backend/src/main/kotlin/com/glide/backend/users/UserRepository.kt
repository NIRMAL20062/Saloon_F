package com.glide.backend.users

import com.glide.backend.auth.AuthenticatedUser
import com.glide.shared.me.UserSide
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.jetbrains.exposed.v1.core.ResultRow
import org.jetbrains.exposed.v1.core.Table
import org.jetbrains.exposed.v1.core.and
import org.jetbrains.exposed.v1.core.eq
import org.jetbrains.exposed.v1.core.isNull
import org.jetbrains.exposed.v1.core.or
import org.jetbrains.exposed.v1.jdbc.Database
import org.jetbrains.exposed.v1.jdbc.insertIgnore
import org.jetbrains.exposed.v1.jdbc.selectAll
import org.jetbrains.exposed.v1.jdbc.transactions.transaction
import org.jetbrains.exposed.v1.jdbc.update
import java.util.UUID
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid
import kotlin.uuid.toJavaUuid
import kotlin.uuid.toKotlinUuid

/** Mirrors V2__app_users.sql. created_at/updated_at are filled by the database. */
@OptIn(ExperimentalUuidApi::class)
object AppUsers : Table("app_users") {
    val id = uuid("id")
    val phone = text("phone").nullable()
    val side = enumerationByName<UserSide>("side", SIDE_MAX_LENGTH).nullable()

    override val primaryKey = PrimaryKey(id)
}

private const val SIDE_MAX_LENGTH = 16

data class AppUser(
    val id: UUID,
    val phone: String?,
    val side: UserSide?,
)

interface UserRepository {
    /** Returns the user's row, creating it on their first request. Safe when two first requests race. */
    suspend fun ensure(user: AuthenticatedUser): AppUser

    /**
     * Saves the onboarding choice. Returns the user, or null when a *different* side was already chosen (the choice is
     * final, D-030). Choosing the same side again is accepted, so app retries are safe.
     */
    suspend fun chooseSide(
        id: UUID,
        side: UserSide,
    ): AppUser?
}

@OptIn(ExperimentalUuidApi::class)
class ExposedUserRepository(
    private val database: Database,
) : UserRepository {
    override suspend fun ensure(user: AuthenticatedUser): AppUser =
        db {
            val id = user.id.toKotlinUuid()
            // INSERT ... ON CONFLICT DO NOTHING: parallel first requests can't create two rows.
            AppUsers.insertIgnore {
                it[AppUsers.id] = id
                it[phone] = user.phone
            }
            val row = find(id)
            // Keep the phone in sync if the person changed it in Supabase.
            if (user.phone != null && row[AppUsers.phone] != user.phone) {
                AppUsers.update({ AppUsers.id eq id }) { it[phone] = user.phone }
                find(id).toAppUser()
            } else {
                row.toAppUser()
            }
        }

    override suspend fun chooseSide(
        id: UUID,
        side: UserSide,
    ): AppUser? =
        db {
            val kid = id.toKotlinUuid()
            // Only rows with no side yet (or the same side) match, so a switch updates nothing and never hits the trigger.
            val updated =
                AppUsers.update({ (AppUsers.id eq kid) and (AppUsers.side.isNull() or (AppUsers.side eq side)) }) {
                    it[AppUsers.side] = side
                }
            if (updated == 1) find(kid).toAppUser() else null
        }

    private fun find(id: Uuid): ResultRow = AppUsers.selectAll().where { AppUsers.id eq id }.single()

    private fun ResultRow.toAppUser() =
        AppUser(this[AppUsers.id].toJavaUuid(), this[AppUsers.phone], this[AppUsers.side])

    private suspend fun <T> db(block: () -> T): T = withContext(Dispatchers.IO) { transaction(database) { block() } }
}
