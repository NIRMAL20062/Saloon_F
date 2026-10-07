package com.glide.shared.salon

import kotlinx.serialization.Serializable

/** Body of `PUT /v1/salon/bank-details` (DF-33). The app asks for the account number twice; the backend gets it once. */
@Serializable
data class BankDetailsRequest(
    /** 1–100 characters after trimming. */
    val accountHolderName: String,
    /** 9–18 digits; spaces are dropped. */
    val accountNumber: String,
    /** 11 characters, e.g. `HDFC0001234`; small letters are fine. */
    val ifsc: String,
)

/** A salon's bank details as the app sees them: never the full account number (DF-24). */
@Serializable
data class BankDetailsResponse(
    val accountHolderName: String,
    /** The last 4 digits only; show them masked, e.g. "•••• 9012". */
    val accountNumberLast4: String,
    val ifsc: String,
)

/** Limits of the bank details, shared so the app can check them before sending (DF-33). */
object BankDetailsRules {
    const val HOLDER_MAX = 100
    const val ACCOUNT_MIN_DIGITS = 9
    const val ACCOUNT_MAX_DIGITS = 18
    const val IFSC_LENGTH = 11
}
