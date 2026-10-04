package com.glide.backend.audit

import com.glide.backend.db.jsonb
import kotlinx.serialization.json.JsonObject
import org.jetbrains.exposed.v1.core.Table
import org.jetbrains.exposed.v1.jdbc.insert
import java.util.UUID
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.toKotlinUuid

/**
 * Mirrors `audit_log` (V5, DF-28): append-only, the database refuses UPDATE, DELETE and TRUNCATE.
 * Write rows with [record] **inside the same transaction** as the change, so there is never a change without its row.
 */
@OptIn(ExperimentalUuidApi::class)
object AuditLog : Table("audit_log") {
    val id = uuid("id")
    val actorUserId = uuid("actor_user_id").nullable()
    val salonId = uuid("salon_id").nullable()
    val action = text("action")
    val entityType = text("entity_type")
    val entityId = text("entity_id")
    val before = jsonb("before").nullable()
    val after = jsonb("after").nullable()
    val requestId = text("request_id").nullable()

    override val primaryKey = PrimaryKey(id)

    /**
     * Adds one row. [actor] null = a command our team ran on the server. Never put secrets, tokens, phone numbers or bank
     * details in [before]/[after].
     */
    fun record(
        actor: UUID?,
        action: String,
        entityType: String,
        entityId: String,
        requestId: String?,
        before: JsonObject? = null,
        after: JsonObject? = null,
        salon: UUID? = null,
    ) {
        insert {
            it[id] = UUID.randomUUID().toKotlinUuid()
            it[actorUserId] = actor?.toKotlinUuid()
            it[salonId] = salon?.toKotlinUuid()
            it[AuditLog.action] = action
            it[AuditLog.entityType] = entityType
            it[AuditLog.entityId] = entityId
            it[AuditLog.before] = before?.toString()
            it[AuditLog.after] = after?.toString()
            it[AuditLog.requestId] = requestId?.take(REQUEST_ID_MAX)
        }
    }

    private const val REQUEST_ID_MAX = 64
}
