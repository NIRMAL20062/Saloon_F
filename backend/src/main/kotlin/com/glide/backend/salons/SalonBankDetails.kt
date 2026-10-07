package com.glide.backend.salons

import org.jetbrains.exposed.v1.core.Table
import kotlin.uuid.ExperimentalUuidApi

/** Mirrors V9__create_salon_bank_details.sql. Only the salon's own transaction sees its row (BE-031). */
@OptIn(ExperimentalUuidApi::class)
object SalonBankDetails : Table("salon_bank_details") {
    val salonId = uuid("salon_id")
    val accountHolderName = text("account_holder_name")

    /** `v1:...` from [com.glide.backend.crypto.FieldCipher], with the salon id as context. Never the number itself. */
    val accountNumberEncrypted = text("account_number_encrypted")
    val accountNumberLast4 = text("account_number_last4")
    val ifsc = text("ifsc")

    override val primaryKey = PrimaryKey(salonId)
}
