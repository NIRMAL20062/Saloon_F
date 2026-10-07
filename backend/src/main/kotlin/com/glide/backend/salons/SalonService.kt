package com.glide.backend.salons

import com.glide.backend.auth.AuthenticatedUser
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

    private class AlreadyMember : RuntimeException()

    private companion object {
        /** Details can still be edited while under verification (PRODUCT §6.1) or after a rejection. */
        val EDITABLE = setOf(SalonStatus.DRAFT, SalonStatus.UNDER_VERIFICATION, SalonStatus.REJECTED)
    }
}
