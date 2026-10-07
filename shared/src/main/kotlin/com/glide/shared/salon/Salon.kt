package com.glide.shared.salon

import kotlinx.serialization.Serializable

/** Who the salon serves (PRODUCT §6.1); also a customer search filter later. */
@Serializable
enum class SalonType { MEN, WOMEN, UNISEX }

/**
 * Where the salon is in onboarding (D-033): DRAFT → UNDER_VERIFICATION → LIVE, or REJECTED (with a reason; fix and submit
 * again), or SUSPENDED by our team. Only a LIVE salon adds staff, shows up for customers and takes bookings.
 */
@Serializable
enum class SalonStatus { DRAFT, UNDER_VERIFICATION, LIVE, REJECTED, SUSPENDED }

/** A person's role in their one salon (D-035, D-039). */
@Serializable
enum class SalonRole { OWNER, STAFF }

/** India's 28 states and 8 union territories, for a salon's address (D-046, DF-32). Sent by name, e.g. `KARNATAKA`. */
@Serializable
enum class IndianState(
    val displayName: String,
) {
    ANDHRA_PRADESH("Andhra Pradesh"),
    ARUNACHAL_PRADESH("Arunachal Pradesh"),
    ASSAM("Assam"),
    BIHAR("Bihar"),
    CHHATTISGARH("Chhattisgarh"),
    GOA("Goa"),
    GUJARAT("Gujarat"),
    HARYANA("Haryana"),
    HIMACHAL_PRADESH("Himachal Pradesh"),
    JHARKHAND("Jharkhand"),
    KARNATAKA("Karnataka"),
    KERALA("Kerala"),
    MADHYA_PRADESH("Madhya Pradesh"),
    MAHARASHTRA("Maharashtra"),
    MANIPUR("Manipur"),
    MEGHALAYA("Meghalaya"),
    MIZORAM("Mizoram"),
    NAGALAND("Nagaland"),
    ODISHA("Odisha"),
    PUNJAB("Punjab"),
    RAJASTHAN("Rajasthan"),
    SIKKIM("Sikkim"),
    TAMIL_NADU("Tamil Nadu"),
    TELANGANA("Telangana"),
    TRIPURA("Tripura"),
    UTTAR_PRADESH("Uttar Pradesh"),
    UTTARAKHAND("Uttarakhand"),
    WEST_BENGAL("West Bengal"),
    ANDAMAN_AND_NICOBAR_ISLANDS("Andaman and Nicobar Islands"),
    CHANDIGARH("Chandigarh"),
    DADRA_AND_NAGAR_HAVELI_AND_DAMAN_AND_DIU("Dadra and Nagar Haveli and Daman and Diu"),
    DELHI("Delhi"),
    JAMMU_AND_KASHMIR("Jammu and Kashmir"),
    LADAKH("Ladakh"),
    LAKSHADWEEP("Lakshadweep"),
    PUDUCHERRY("Puducherry"),
}

/** A salon's address in the usual Indian parts (D-046). Each text part is trimmed; limits in [SalonRules]. */
@Serializable
data class SalonAddress(
    /** House, shop or building, and street. */
    val line1: String,
    /** Area or locality. */
    val area: String,
    /** Optional; empty or missing means none. */
    val landmark: String? = null,
    val city: String,
    val state: IndianState,
    /** 6 digits, not starting with 0. */
    val pincode: String,
)

/** Body of `POST /v1/salon/salons` and `PUT /v1/salon/salon`. */
@Serializable
data class SalonProfileRequest(
    /** 1–30 characters after trimming (D-046). */
    val name: String,
    /**
     * The number customers call: an Indian number, with or without +91, 91 or a leading 0; spaces and dashes are fine.
     * Missing or empty: the owner's login number (D-046).
     */
    val phone: String? = null,
    val address: SalonAddress,
    val type: SalonType,
)

/** A salon as the app sees it. */
@Serializable
data class SalonResponse(
    /** UUID. */
    val id: String,
    val name: String,
    /** E.164 without "+", e.g. `919876543210`. */
    val phone: String,
    val address: SalonAddress,
    val type: SalonType,
    val status: SalonStatus,
    /** Why our team rejected it; only when [status] is REJECTED. */
    val rejectionReason: String? = null,
)

/** Body of `GET /v1/salon/me`, `POST /v1/salon/salons` and `PUT /v1/salon/salon`: the person's one salon and their role. */
@Serializable
data class MySalonResponse(
    val salon: SalonResponse,
    val role: SalonRole,
)

/** Limits of a salon's profile, shared so the app can check them before sending (D-046, DF-32). */
object SalonRules {
    const val NAME_MAX = 30
    const val ADDRESS_PART_MAX = 100
    const val CITY_MAX = 50
}

/** Error codes of the /v1/salon endpoints (see ErrorCodes for the shared ones). */
object SalonErrorCodes {
    /** The name is empty, longer than 30 characters, or has control characters. */
    const val INVALID_SALON_NAME = "INVALID_SALON_NAME"

    /** The phone isn't an Indian number of 10 digits (after +91, 91 or 0), or none was given and the login has none. */
    const val INVALID_SALON_PHONE = "INVALID_SALON_PHONE"

    /** An address part is missing or too long, or the PIN code isn't 6 digits; the message names the part. */
    const val INVALID_ADDRESS = "INVALID_ADDRESS"

    /** Creating a salon needs the salon side, chosen at onboarding (D-030). */
    const val NOT_SALON_SIDE = "NOT_SALON_SIDE"

    /** The person already belongs to a salon (one salon per person, D-035). */
    const val ALREADY_IN_SALON = "ALREADY_IN_SALON"

    /** The person belongs to no salon yet. */
    const val NO_SALON = "NO_SALON"

    /** Only the salon's owner can do this (D-039). */
    const val NOT_OWNER = "NOT_OWNER"

    /** A live or suspended salon's profile or bank details can't be changed here, nor can it be submitted again. */
    const val SALON_NOT_EDITABLE = "SALON_NOT_EDITABLE"

    /** The account holder's name is empty, longer than 100 characters, or has control characters. */
    const val INVALID_ACCOUNT_HOLDER = "INVALID_ACCOUNT_HOLDER"

    /** The account number isn't 9 to 18 digits. */
    const val INVALID_ACCOUNT_NUMBER = "INVALID_ACCOUNT_NUMBER"

    /** The IFSC isn't 4 letters, 0, then 6 letters or digits. */
    const val INVALID_IFSC = "INVALID_IFSC"

    /** No bank details saved yet: `GET` answers 404; submitting for verification answers 409. */
    const val NO_BANK_DETAILS = "NO_BANK_DETAILS"

    /** The server has no encryption key for bank details, so it can't save them (503). */
    const val BANK_DETAILS_UNAVAILABLE = "BANK_DETAILS_UNAVAILABLE"
}
