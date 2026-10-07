package com.glide.backend.salons

import com.glide.backend.auth.AuthenticatedUser
import com.glide.backend.crypto.FieldCipher
import com.glide.backend.db.RowSecurity
import com.glide.backend.db.Transactor
import com.glide.backend.users.UserRepository
import com.glide.backend.validation.Phones
import com.glide.shared.me.UserSide
import com.glide.shared.salon.SalonRole
import com.glide.shared.salon.SalonStatus
import java.util.UUID

/**
 * A person's one salon (BE-017): create it (they become its owner), read it, and let the owner edit its profile until it
 * is live. The salon always comes from the person's membership (D-036), never from the request. One transaction per call.
 */
class SalonService(
    private val transactor: Transactor,
    private val users: UserRepository,
    private val salons: SalonRepository,
    /** Encrypts bank account numbers (DF-33); null when the server has no key, so bank details can't be saved. */
    private val bankCipher: FieldCipher?,
) {
    sealed interface Outcome {
        data class Saved(
            val salon: Salon,
            val role: SalonRole,
        ) : Outcome

        /** Creating a salon needs the salon side (D-030). */
        data object NotSalonSide : Outcome

        /** One salon per person (D-035). */
        data object AlreadyInSalon : Outcome

        data object NoSalon : Outcome

        data object NotOwner : Outcome

        /** LIVE or SUSPENDED. */
        data object NotEditable : Outcome

        /** No phone given and none to take from the login. */
        data object PhoneNeeded : Outcome

        data class BankSaved(
            val details: StoredBankDetails,
        ) : Outcome

        /** `GET`: none saved yet. */
        data object NoBankDetails : Outcome

        /** Submitting needs bank details first. */
        data object SubmitNeedsBankDetails : Outcome

        /** No encryption key on this server. */
        data object BankUnavailable : Outcome
    }

    suspend fun create(
        user: AuthenticatedUser,
        profile: SalonInput.Valid,
    ): Outcome {
        val salonId = UUID.randomUUID()
        return try {
            transactor.transaction(salon = salonId, user = user.id) {
                val me = users.ensure(user)
                when {
                    me.side != UserSide.SALON -> {
                        Outcome.NotSalonSide
                    }

                    salons.activeMembership(user.id) != null -> {
                        Outcome.AlreadyInSalon
                    }

                    else -> {
                        val loginPhone = user.phone ?: return@transaction Outcome.PhoneNeeded
                        val phone = profile.phone ?: Phones.indian(loginPhone) ?: return@transaction Outcome.PhoneNeeded
                        salons.insert(salonId, profile, phone)
                        // A parallel request of the same person got there first: drop this salon with the transaction.
                        if (!salons.addOwner(salonId, user.id, loginPhone)) throw AlreadyMember()
                        Outcome.Saved(salons.find(salonId)!!, SalonRole.OWNER)
                    }
                }
            }
        } catch (_: AlreadyMember) {
            Outcome.AlreadyInSalon
        }
    }

    suspend fun mine(user: AuthenticatedUser): Outcome =
        transactor.transaction(user = user.id) {
            val membership = salons.activeMembership(user.id) ?: return@transaction Outcome.NoSalon
            RowSecurity.forSalon(membership.salonId)
            Outcome.Saved(salons.find(membership.salonId)!!, membership.role)
        }

    suspend fun updateProfile(
        user: AuthenticatedUser,
        profile: SalonInput.Valid,
    ): Outcome =
        transactor.transaction(user = user.id) {
            val membership = salons.activeMembership(user.id) ?: return@transaction Outcome.NoSalon
            if (membership.role != SalonRole.OWNER) return@transaction Outcome.NotOwner
            RowSecurity.forSalon(membership.salonId)
            val salon = salons.find(membership.salonId)!!
            if (salon.status !in EDITABLE) return@transaction Outcome.NotEditable
            val phone = profile.phone ?: user.phone?.let(Phones::indian) ?: return@transaction Outcome.PhoneNeeded
            salons.updateProfile(salon.id, profile, phone)
            Outcome.Saved(salons.find(salon.id)!!, SalonRole.OWNER)
        }

    /** The owner saves or replaces the bank details while the salon isn't live; only the encrypted number is stored. */
    suspend fun saveBankDetails(
        user: AuthenticatedUser,
        input: BankDetailsInput.Valid,
    ): Outcome {
        val cipher = bankCipher ?: return Outcome.BankUnavailable
        return ownerTransaction(user) { salon ->
            if (salon.status !in EDITABLE) return@ownerTransaction Outcome.NotEditable
            val stored =
                StoredBankDetails(
                    accountHolderName = input.accountHolderName,
                    // Bound to this salon: copied to another salon's row, it no longer decrypts.
                    accountNumberEncrypted = cipher.encrypt(input.accountNumber, salon.id.toString()),
                    accountNumberLast4 = input.last4,
                    ifsc = input.ifsc,
                )
            salons.saveBankDetails(salon.id, stored)
            Outcome.BankSaved(stored)
        }
    }

    /** The owner reads the bank details, masked. */
    suspend fun bankDetails(user: AuthenticatedUser): Outcome =
        ownerTransaction(user) { salon ->
            salons.bankDetails(salon.id)?.let { Outcome.BankSaved(it) }
                ?: Outcome.NoBankDetails
        }

    /** The owner sends the salon to our team (D-033). Safe to retry: already under verification answers the same. */
    suspend fun submitForVerification(user: AuthenticatedUser): Outcome =
        ownerTransaction(user) { salon ->
            when (salon.status) {
                SalonStatus.UNDER_VERIFICATION -> {
                    Outcome.Saved(salon, SalonRole.OWNER)
                }

                SalonStatus.LIVE, SalonStatus.SUSPENDED -> {
                    Outcome.NotEditable
                }

                SalonStatus.DRAFT, SalonStatus.REJECTED -> {
                    if (salons.bankDetails(salon.id) == null) return@ownerTransaction Outcome.SubmitNeedsBankDetails
                    salons.submitForVerification(salon.id)
                    Outcome.Saved(salons.find(salon.id)!!, SalonRole.OWNER)
                }
            }
        }

    /** One transaction for the person's own salon, owners only; [block] runs with the transaction for that salon. */
    private suspend fun ownerTransaction(
        user: AuthenticatedUser,
        block: (Salon) -> Outcome,
    ): Outcome =
        transactor.transaction(user = user.id) {
            val membership = salons.activeMembership(user.id) ?: return@transaction Outcome.NoSalon
            if (membership.role != SalonRole.OWNER) return@transaction Outcome.NotOwner
            RowSecurity.forSalon(membership.salonId)
            block(salons.find(membership.salonId)!!)
        }

    private class AlreadyMember : RuntimeException()

    private companion object {
        /** Details can still be edited while under verification (PRODUCT §6.1) or after a rejection. */
        val EDITABLE = setOf(SalonStatus.DRAFT, SalonStatus.UNDER_VERIFICATION, SalonStatus.REJECTED)
    }
}
