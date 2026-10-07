package com.glide.backend.validation

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

/** Indian phone numbers as people type them (DF-32). */
class PhonesTest {
    @Test
    fun `mobiles and landlines in the usual ways become 91 plus 10 digits`() {
        mapOf(
            "9876543210" to "919876543210",
            "+91 98765 43210" to "919876543210",
            "91-98765-43210" to "919876543210",
            "098765 43210" to "919876543210",
            "080-2345 6789" to "918023456789",
            "(080) 2345.6789" to "918023456789",
            " 919000000001 " to "919000000001",
        ).forEach { (typed, stored) -> assertEquals(stored, Phones.indian(typed), typed) }
    }

    @Test
    fun `anything else is refused`() {
        listOf(
            "",
            "12345",
            "0123456789",
            "1234567890",
            "+1 415 555 0100",
            "98765432101",
            "98765abc10",
            "+91 0876543210",
        ).forEach { assertNull(Phones.indian(it), it) }
    }
}
