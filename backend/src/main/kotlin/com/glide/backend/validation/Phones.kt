package com.glide.backend.validation

/** Phone numbers as people type them (DF-32). */
object Phones {
    private val SEPARATORS = Regex("[\\s\\-().]")
    private val TEN_DIGITS = Regex("[2-9][0-9]{9}")

    /**
     * An Indian number (mobile, or landline with its STD code) as `91XXXXXXXXXX`, or null. Accepts +91, 91 or a leading 0
     * before the 10 digits, and spaces, dashes, dots or brackets anywhere: "+91 98765 43210" and "080-2345 6789" both work.
     */
    fun indian(raw: String): String? {
        val digits = raw.trim().replace(SEPARATORS, "").removePrefix("+")
        val local =
            when {
                digits.length == 12 && digits.startsWith("91") -> digits.substring(2)
                digits.length == 11 && digits.startsWith("0") -> digits.substring(1)
                else -> digits
            }
        return if (TEN_DIGITS.matches(local)) "91$local" else null
    }
}
