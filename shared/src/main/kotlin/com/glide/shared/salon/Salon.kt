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
