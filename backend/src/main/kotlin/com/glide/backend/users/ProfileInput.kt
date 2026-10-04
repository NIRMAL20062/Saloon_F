package com.glide.backend.users

import com.glide.backend.validation.Emails
import com.glide.shared.me.MeErrorCodes
import com.glide.shared.me.ProfileRules
import com.glide.shared.me.UpdateProfileRequest

/**
 * Checks and normalises `PUT /v1/me/profile` (BE-018): the name is trimmed and must be 2–60 characters without control
 * characters; the email is optional (empty = none), trimmed, lower-cased, at most 254 characters and shaped like an
 * address. The database checks the same limits again (V4).
 */
sealed interface ProfileInput {
    data class Valid(
        val name: String,
        val email: String?,
    ) : ProfileInput

    data class Invalid(
        val code: String,
        val message: String,
    ) : ProfileInput

    companion object {
        fun from(request: UpdateProfileRequest): ProfileInput {
            val name = request.name.trim()
            if (name.length !in ProfileRules.NAME_MIN..ProfileRules.NAME_MAX || name.any { it.isISOControl() }) {
                return Invalid(MeErrorCodes.INVALID_NAME, "Enter a name between 2 and 60 characters.")
            }
            val rawEmail = request.email?.trim()?.ifEmpty { null } ?: return Valid(name, null)
            val email =
                Emails.normalize(rawEmail)
                    ?: return Invalid(MeErrorCodes.INVALID_EMAIL, "Enter a valid email address, or leave it empty.")
            return Valid(name, email)
        }
    }
}
