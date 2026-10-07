package com.glide.backend.salons

import com.glide.shared.salon.SalonRole
import org.jetbrains.exposed.v1.core.Table
import kotlin.uuid.ExperimentalUuidApi

private const val ENUM_MAX_LENGTH = 16

/** Membership status: REMOVED keeps the history of someone who left (PRODUCT §6.2). */
enum class MemberStatus { ACTIVE, REMOVED }

/** Mirrors V8__salon_members.sql. Visible in its salon's transaction, and to the member themself (BE-031). */
@OptIn(ExperimentalUuidApi::class)
object SalonMembers : Table("salon_members") {
    val id = uuid("id")
    val salonId = uuid("salon_id")
    val phone = text("phone")
    val userId = uuid("user_id").nullable()
    val role = enumerationByName<SalonRole>("role", ENUM_MAX_LENGTH)
    val status = enumerationByName<MemberStatus>("status", ENUM_MAX_LENGTH)

    override val primaryKey = PrimaryKey(id)
}
