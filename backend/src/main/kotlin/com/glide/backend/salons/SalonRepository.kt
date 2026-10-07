package com.glide.backend.salons

import com.glide.shared.salon.IndianState
import com.glide.shared.salon.SalonRole
import com.glide.shared.salon.SalonStatus
import com.glide.shared.salon.SalonType
import org.jetbrains.exposed.v1.core.ResultRow
import org.jetbrains.exposed.v1.core.and
import org.jetbrains.exposed.v1.core.eq
import org.jetbrains.exposed.v1.jdbc.insert
import org.jetbrains.exposed.v1.jdbc.insertIgnore
import org.jetbrains.exposed.v1.jdbc.selectAll
import org.jetbrains.exposed.v1.jdbc.update
import org.jetbrains.exposed.v1.jdbc.upsert
import java.util.UUID
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.toJavaUuid
import kotlin.uuid.toKotlinUuid

/** A person's place in their one salon (D-035). */
data class Membership(
    val salonId: UUID,
    val role: SalonRole,
)

/** Bank details as stored: the account number only encrypted, plus its last 4 digits (DF-24, DF-33). */
data class StoredBankDetails(
    val accountHolderName: String,
    val accountNumberEncrypted: String,
    val accountNumberLast4: String,
    val ifsc: String,
)

data class Salon(
    val id: UUID,
    val name: String,
    val phone: String,
    val line1: String,
    val area: String,
    val landmark: String?,
    val city: String,
    val state: IndianState,
    val pincode: String,
    val type: SalonType,
    val status: SalonStatus,
    val rejectionReason: String?,
)

/**
 * Salons and their members (BE-017). Runs inside the caller's transaction (BE-025). Row-level security decides what is
 * visible (BE-031): the person's own membership once the transaction is for them, a salon's rows once it is for that salon.
 */
interface SalonRepository {
    /** The person's active membership, or null. The transaction must be for [userId] ([com.glide.backend.db.RowSecurity]). */
    fun activeMembership(userId: UUID): Membership?

    /** A new DRAFT salon. The transaction must be for [id]. */
    fun insert(
        id: UUID,
        profile: SalonInput.Valid,
        phone: String,
    )

    /** Adds the owner. False when the person or their phone already has an active membership in any salon (D-035). */
    fun addOwner(
        salonId: UUID,
        userId: UUID,
        phone: String,
    ): Boolean

    /** The salon, or null (also when the transaction isn't for it). */
    fun find(id: UUID): Salon?

    fun updateProfile(
        id: UUID,
        profile: SalonInput.Valid,
        phone: String,
    )

    /** Saves or replaces the salon's bank details. The transaction must be for [salonId]. */
    fun saveBankDetails(
        salonId: UUID,
        details: StoredBankDetails,
    )

    fun bankDetails(salonId: UUID): StoredBankDetails?

    /** DRAFT or REJECTED → UNDER_VERIFICATION, clearing an old rejection reason (D-033). */
    fun submitForVerification(salonId: UUID)
}

@OptIn(ExperimentalUuidApi::class)
class ExposedSalonRepository : SalonRepository {
    override fun activeMembership(userId: UUID): Membership? =
        SalonMembers
            .selectAll()
            .where { (SalonMembers.userId eq userId.toKotlinUuid()) and (SalonMembers.status eq MemberStatus.ACTIVE) }
            .singleOrNull()
            ?.let { Membership(it[SalonMembers.salonId].toJavaUuid(), it[SalonMembers.role]) }

    override fun insert(
        id: UUID,
        profile: SalonInput.Valid,
        phone: String,
    ) {
        Salons.insert {
            it[Salons.id] = id.toKotlinUuid()
            it[Salons.phone] = phone
            it.write(profile)
        }
    }

    override fun addOwner(
        salonId: UUID,
        userId: UUID,
        phone: String,
    ): Boolean =
        // ON CONFLICT DO NOTHING: the unique indexes of V8 decide, also against salons this transaction can't see.
        SalonMembers
            .insertIgnore {
                it[id] = UUID.randomUUID().toKotlinUuid()
                it[SalonMembers.salonId] = salonId.toKotlinUuid()
                it[SalonMembers.userId] = userId.toKotlinUuid()
                it[SalonMembers.phone] = phone
                it[role] = SalonRole.OWNER
                it[status] = MemberStatus.ACTIVE
            }.insertedCount == 1

    override fun find(id: UUID): Salon? =
        Salons
            .selectAll()
            .where { Salons.id eq id.toKotlinUuid() }
            .singleOrNull()
            ?.toSalon()

    override fun updateProfile(
        id: UUID,
        profile: SalonInput.Valid,
        phone: String,
    ) {
        Salons.update({ Salons.id eq id.toKotlinUuid() }) {
            it[Salons.phone] = phone
            it.write(profile)
        }
    }

    override fun saveBankDetails(
        salonId: UUID,
        details: StoredBankDetails,
    ) {
        SalonBankDetails.upsert {
            it[SalonBankDetails.salonId] = salonId.toKotlinUuid()
            it[accountHolderName] = details.accountHolderName
            it[accountNumberEncrypted] = details.accountNumberEncrypted
            it[accountNumberLast4] = details.accountNumberLast4
            it[ifsc] = details.ifsc
        }
    }

    override fun bankDetails(salonId: UUID): StoredBankDetails? =
        SalonBankDetails
            .selectAll()
            .where { SalonBankDetails.salonId eq salonId.toKotlinUuid() }
            .singleOrNull()
            ?.let {
                StoredBankDetails(
                    it[SalonBankDetails.accountHolderName],
                    it[SalonBankDetails.accountNumberEncrypted],
                    it[SalonBankDetails.accountNumberLast4],
                    it[SalonBankDetails.ifsc],
                )
            }

    override fun submitForVerification(salonId: UUID) {
        Salons.update({ Salons.id eq salonId.toKotlinUuid() }) {
            it[status] = SalonStatus.UNDER_VERIFICATION
            it[rejectionReason] = null
        }
    }

    private fun org.jetbrains.exposed.v1.core.statements.UpdateBuilder<*>.write(profile: SalonInput.Valid) {
        this[Salons.name] = profile.name
        this[Salons.addressLine1] = profile.line1
        this[Salons.addressArea] = profile.area
        this[Salons.addressLandmark] = profile.landmark
        this[Salons.city] = profile.city
        this[Salons.state] = profile.state
        this[Salons.pincode] = profile.pincode
        this[Salons.type] = profile.type
    }

    private fun ResultRow.toSalon() =
        Salon(
            id = this[Salons.id].toJavaUuid(),
            name = this[Salons.name],
            phone = this[Salons.phone],
            line1 = this[Salons.addressLine1],
            area = this[Salons.addressArea],
            landmark = this[Salons.addressLandmark],
            city = this[Salons.city],
            state = this[Salons.state],
            pincode = this[Salons.pincode],
            type = this[Salons.type],
            status = this[Salons.status],
            rejectionReason = this[Salons.rejectionReason],
        )
}
