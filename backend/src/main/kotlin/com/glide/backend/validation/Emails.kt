package com.glide.backend.validation

/** One rule for email addresses everywhere (profiles BE-018, admins BE-020). The database checks the same shape. */
object Emails {
    const val MAX_LENGTH = 254
    private val SHAPE = Regex("^[^\\s@]+@[^\\s@]+\\.[^\\s@]+$")

    /** Trimmed and lower-cased, or null when it isn't shaped like an address or is longer than [MAX_LENGTH]. */
    fun normalize(raw: String): String? = raw.trim().lowercase().takeIf { it.length <= MAX_LENGTH && SHAPE.matches(it) }
}
