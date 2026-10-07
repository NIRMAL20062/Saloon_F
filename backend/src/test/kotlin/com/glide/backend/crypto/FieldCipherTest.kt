package com.glide.backend.crypto

import com.glide.backend.config.SecretBytes
import java.util.Base64
import javax.crypto.AEADBadTagException
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertNotEquals
import kotlin.test.assertTrue

/** Encrypting a bank account number (DF-24, DF-33). */
class FieldCipherTest {
    private val cipher = FieldCipher(SecretBytes(ByteArray(32) { it.toByte() }))
    private val account = "123456789012"

    @Test
    fun `a value comes back only with the same key and context`() {
        val sealed = cipher.encrypt(account, "salon-a")

        assertEquals(account, cipher.decrypt(sealed, "salon-a"))
        assertFailsWith<AEADBadTagException> { cipher.decrypt(sealed, "salon-b") } // copied to another salon
        val otherKey = FieldCipher(SecretBytes(ByteArray(32) { 7 }))
        assertFailsWith<AEADBadTagException> { otherKey.decrypt(sealed, "salon-a") }
    }

    @Test
    fun `the stored value never shows the number, and differs every time`() {
        val first = cipher.encrypt(account, "salon-a")
        val second = cipher.encrypt(account, "salon-a")

        assertTrue(first.startsWith("v1:"))
        assertFalse(first.contains("6789"))
        assertNotEquals(first, second) // a fresh nonce each time: equal numbers don't look equal
    }

    @Test
    fun `any change to the stored value is detected`() {
        val sealed = cipher.encrypt(account, "salon-a")
        val bytes = Base64.getDecoder().decode(sealed.removePrefix("v1:"))
        bytes[bytes.size - 1] = (bytes[bytes.size - 1] + 1).toByte()

        assertFailsWith<AEADBadTagException> {
            cipher.decrypt(
                "v1:" + Base64.getEncoder().encodeToString(bytes),
                "salon-a",
            )
        }
    }

    @Test
    fun `only a 32-byte key is accepted`() {
        assertFailsWith<IllegalArgumentException> { FieldCipher(SecretBytes(ByteArray(16))) }
    }
}
