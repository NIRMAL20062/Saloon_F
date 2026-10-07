package com.glide.backend.salons

import com.glide.backend.validation.Phones
import com.glide.shared.salon.IndianState
import com.glide.shared.salon.SalonErrorCodes
import com.glide.shared.salon.SalonProfileRequest
import com.glide.shared.salon.SalonRules
import com.glide.shared.salon.SalonType

/**
 * A salon profile from the app, checked (D-046, DF-32). Lengths are counted in characters the way PostgreSQL counts them
 * (code points: an emoji is one), so a value we accept is never refused by the database (V7 checks the same limits).
 */
sealed interface SalonInput {
    data class Valid(
        val name: String,
        /** `91XXXXXXXXXX`, or null: use the owner's login number. */
        val phone: String?,
        val line1: String,
        val area: String,
        val landmark: String?,
        val city: String,
        val state: IndianState,
        val pincode: String,
        val type: SalonType,
    ) : SalonInput

    data class Invalid(
        val code: String,
        val message: String,
    ) : SalonInput

    companion object {
        private val PINCODE = Regex("[1-9][0-9]{5}")

        fun from(request: SalonProfileRequest): SalonInput {
            val name = request.name.trim()
            if (!fits(name, 1, SalonRules.NAME_MAX)) {
                return Invalid(
                    SalonErrorCodes.INVALID_SALON_NAME,
                    "The salon's name must be 1 to ${SalonRules.NAME_MAX} characters.",
                )
            }
            val rawPhone = request.phone?.trim()?.ifEmpty { null }
            val phone =
                rawPhone?.let {
                    Phones.indian(it)
                        ?: return Invalid(
                            SalonErrorCodes.INVALID_SALON_PHONE,
                            "The phone must be an Indian number of 10 digits.",
                        )
                }
            val address = request.address
            val line1 = address.line1.trim()
            val area = address.area.trim()
            val landmark = address.landmark?.trim()?.ifEmpty { null }
            val city = address.city.trim()
            val part = SalonRules.ADDRESS_PART_MAX
            return when {
                !fits(line1, 1, part) -> {
                    invalidAddress("House, shop or building and street", part)
                }

                !fits(area, 1, part) -> {
                    invalidAddress("Area", part)
                }

                landmark != null && !fits(landmark, 1, part) -> {
                    invalidAddress("Landmark", part)
                }

                !fits(city, 1, SalonRules.CITY_MAX) -> {
                    invalidAddress("City", SalonRules.CITY_MAX)
                }

                !PINCODE.matches(address.pincode.trim()) -> {
                    Invalid(SalonErrorCodes.INVALID_ADDRESS, "The PIN code must be 6 digits, not starting with 0.")
                }

                else -> {
                    Valid(name, phone, line1, area, landmark, city, address.state, address.pincode.trim(), request.type)
                }
            }
        }

        private fun invalidAddress(
            part: String,
            max: Int,
        ) = Invalid(SalonErrorCodes.INVALID_ADDRESS, "$part: 1 to $max characters.")

        private fun fits(
            value: String,
            min: Int,
            max: Int,
        ): Boolean = value.codePointCount(0, value.length) in min..max && value.none { it.isISOControl() }
    }
}
