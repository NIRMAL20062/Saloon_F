package com.glide.backend.admins

import com.glide.backend.audit.AuditLog
import com.glide.backend.db.DbNow
import com.glide.backend.db.timestamptz
import com.glide.shared.admin.AdminStatus
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonObject
import org.jetbrains.exposed.v1.core.ResultRow
import org.jetbrains.exposed.v1.core.Table
import org.jetbrains.exposed.v1.core.and
import org.jetbrains.exposed.v1.core.eq
import org.jetbrains.exposed.v1.jdbc.Database
import org.jetbrains.exposed.v1.jdbc.insertIgnore
import org.jetbrains.exposed.v1.jdbc.selectAll
import org.jetbrains.exposed.v1.jdbc.transactions.transaction
import org.jetbrains.exposed.v1.jdbc.update
import java.util.UUID
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.toJavaUuid
import kotlin.uuid.toKotlinUuid

/** Mirrors `admins` (V5). created_at/updated_at are filled by the database. */
@OptIn(ExperimentalUuidApi::class)
object Admins : Table("admins") {
    val userId = uuid("user_id")
    val email = text("email")
    val status = enumerationByName<AdminStatus>("status", STATUS_MAX_LENGTH)
    val invitedBy = uuid("invited_by").nullable()
    val activatedAt = timestamptz("activated_at").nullable()

    override val primaryKey = PrimaryKey(userId)
}

private const val STATUS_MAX_LENGTH = 16

data class Admin(
    /** Supabase user id of the admin's login. */
    val userId: UUID,
    val email: String,
    val status: AdminStatus,
    /** Who invited them; null for the first admin. */
    val invitedBy: UUID?,
)

/** Who our admins are (D-013). Every change writes its audit row in the same transaction (DF-28). */
interface AdminRepository {
    suspend fun find(userId: UUID): Admin?

    suspend fun findByEmail(email: String): Admin?

    /** INVITED → ACTIVE on the admin's first login with the authenticator app. Already ACTIVE: unchanged, no audit row. */
    suspend fun activate(
        userId: UUID,
        requestId: String?,
    ): Admin

    /** Adds the first admin (no inviter). Null when there already is one: later admins are invited by admins. */
    suspend fun addFirst(
        userId: UUID,
        email: String,
    ): Admin?

    /** Adds an invited admin. Null when that login or email is already an admin. */
    suspend fun addInvited(
        userId: UUID,
        email: String,
        invitedBy: UUID,
        requestId: String?,
    ): Admin?
}

@OptIn(ExperimentalUuidApi::class)
class ExposedAdminRepository(
    private val database: Database,
) : AdminRepository {
    override suspend fun find(userId: UUID): Admin? =
        db {
            Admins
                .selectAll()
                .where { Admins.userId eq userId.toKotlinUuid() }
                .singleOrNull()
                ?.toAdmin()
        }

    override suspend fun findByEmail(email: String): Admin? =
        db {
            Admins
                .selectAll()
                .where { Admins.email eq email }
                .singleOrNull()
                ?.toAdmin()
        }

    override suspend fun activate(
        userId: UUID,
        requestId: String?,
    ): Admin =
        db {
            val id = userId.toKotlinUuid()
            // Only an INVITED row matches, so parallel first requests activate (and audit) exactly once.
            val changed =
                Admins.update({ (Admins.userId eq id) and (Admins.status eq AdminStatus.INVITED) }) {
                    it[status] = AdminStatus.ACTIVE
                    it[activatedAt] = DbNow
                }
            if (changed == 1) {
                AuditLog.record(
                    actor = userId,
                    action = "ADMIN_ACTIVATED",
                    entityType = ENTITY,
                    entityId = userId.toString(),
                    requestId = requestId,
                    before = statusJson(AdminStatus.INVITED),
                    after = statusJson(AdminStatus.ACTIVE),
                )
            }
            Admins
                .selectAll()
                .where { Admins.userId eq id }
                .single()
                .toAdmin()
        }

    override suspend fun addFirst(
        userId: UUID,
        email: String,
    ): Admin? = add(userId, email, invitedBy = null, requestId = null, action = "ADMIN_ADDED")

    override suspend fun addInvited(
        userId: UUID,
        email: String,
        invitedBy: UUID,
        requestId: String?,
    ): Admin? = add(userId, email, invitedBy, requestId, action = "ADMIN_INVITED")

    private suspend fun add(
        userId: UUID,
        email: String,
        invitedBy: UUID?,
        requestId: String?,
        action: String,
    ): Admin? =
        db {
            // ON CONFLICT DO NOTHING covers every unique rule at once: the login, the email, and "only one first admin".
            val inserted =
                Admins
                    .insertIgnore {
                        it[Admins.userId] = userId.toKotlinUuid()
                        it[Admins.email] = email
                        it[status] = AdminStatus.INVITED
                        it[Admins.invitedBy] = invitedBy?.toKotlinUuid()
                    }.insertedCount
            if (inserted == 0) return@db null
            AuditLog.record(
                actor = invitedBy,
                action = action,
                entityType = ENTITY,
                entityId = userId.toString(),
                requestId = requestId,
                after =
                    buildJsonObject {
                        put("email", JsonPrimitive(email))
                        put("status", JsonPrimitive(AdminStatus.INVITED.name))
                    },
            )
            Admin(userId, email, AdminStatus.INVITED, invitedBy)
        }

    private fun statusJson(status: AdminStatus) = buildJsonObject { put("status", JsonPrimitive(status.name)) }

    private fun ResultRow.toAdmin() =
        Admin(
            userId = this[Admins.userId].toJavaUuid(),
            email = this[Admins.email],
            status = this[Admins.status],
            invitedBy = this[Admins.invitedBy]?.toJavaUuid(),
        )

    private suspend fun <T> db(block: () -> T): T = withContext(Dispatchers.IO) { transaction(database) { block() } }

    private companion object {
        const val ENTITY = "admin"
    }
}
