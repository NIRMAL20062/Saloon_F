package com.glide.backend.salons

import com.glide.shared.salon.BankDetailsRequest
import com.glide.shared.salon.BankDetailsRules
import com.glide.shared.salon.SalonErrorCodes

/** Bank details from the app, checked (DF-33). Messages never repeat what was typed. */
sealed interface BankDetailsInput {
    data class Valid(
        val accountHolderName: String,
        /** Digits only. */
        val accountNumber: String,
        /** In capitals. */
        val ifsc: String,
    ) : BankDetailsInput {
        val last4: String get() = accountNumber.takeLast(4)

        override fun toString() = "BankDetailsInput.Valid(accountNumber=****$last4)"
    }

    data class Invalid(
        val code: String,
        val message: String,
    ) : BankDetailsInput

    companion object {
        private val IFSC = Regex("[A-Z]{4}0[A-Z0-9]{6}")
        private val ACCOUNT =
            Regex("[0-9]{${BankDetailsRules.ACCOUNT_MIN_DIGITS},${BankDetailsRules.ACCOUNT_MAX_DIGITS}}")

        fun from(request: BankDetailsRequest): BankDetailsInput {
            val holder = request.accountHolderName.trim()
            val account = request.accountNumber.replace(" ", "")
            val ifsc = request.ifsc.trim().uppercase()
            return when {
                holder.codePointCount(0, holder.length) !in 1..BankDetailsRules.HOLDER_MAX ||
                    holder.any { it.isISOControl() } -> {
                    Invalid(
                        SalonErrorCodes.INVALID_ACCOUNT_HOLDER,
                        "The account holder's name must be 1 to ${BankDetailsRules.HOLDER_MAX} characters.",
                    )
                }

                !ACCOUNT.matches(account) -> {
                    Invalid(
                        SalonErrorCodes.INVALID_ACCOUNT_NUMBER,
                        "The account number must be ${BankDetailsRules.ACCOUNT_MIN_DIGITS} to ${BankDetailsRules.ACCOUNT_MAX_DIGITS} digits.",
                    )
                }

                !IFSC.matches(ifsc) -> {
                    Invalid(SalonErrorCodes.INVALID_IFSC, "The IFSC must be 4 letters, 0, then 6 letters or digits.")
                }

                else -> {
                    Valid(holder, account, ifsc)
                }
            }
        }
    }
}
