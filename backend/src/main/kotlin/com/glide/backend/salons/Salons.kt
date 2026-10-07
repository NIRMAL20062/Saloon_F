package com.glide.backend.salons

import com.glide.shared.salon.IndianState
import com.glide.shared.salon.SalonStatus
import com.glide.shared.salon.SalonType
import org.jetbrains.exposed.v1.core.Table
import kotlin.uuid.ExperimentalUuidApi

private const val ENUM_MAX_LENGTH = 64

/** Mirrors V7__salons.sql. A row is visible only in a transaction for that salon (row-level security, BE-031). */
@OptIn(ExperimentalUuidApi::class)
object Salons : Table("salons") {
    val id = uuid("id")
    val name = text("name")
    val phone = text("phone")
    val addressLine1 = text("address_line1")
    val addressArea = text("address_area")
    val addressLandmark = text("address_landmark").nullable()
    val city = text("city")
    val state = enumerationByName<IndianState>("state", ENUM_MAX_LENGTH)
    val pincode = text("pincode")
    val type = enumerationByName<SalonType>("type", ENUM_MAX_LENGTH)
    val status = enumerationByName<SalonStatus>("status", ENUM_MAX_LENGTH)
    val rejectionReason = text("rejection_reason").nullable()

    override val primaryKey = PrimaryKey(id)
}
