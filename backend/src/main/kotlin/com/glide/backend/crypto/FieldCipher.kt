package com.glide.backend.crypto

import com.glide.backend.config.SecretBytes
import java.security.SecureRandom
import java.util.Base64
import javax.crypto.Cipher
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.SecretKeySpec

/**
 * Encrypts one sensitive field for the database (DF-24, DF-33): AES-256-GCM with a fresh random nonce each time. The
 * stored value is `v1:` + base64(nonce | ciphertext | tag). [context] (e.g. the salon id) is bound in as associated data,
 * so a value copied to another row no longer decrypts; any change to the stored value is detected.
 */
class FieldCipher(
    key: SecretBytes,
) {
    private val key =
        SecretKeySpec(key.copy().also { require(it.size == KEY_BYTES) { "the key must be 32 bytes" } }, "AES")
    private val random = SecureRandom()

    fun encrypt(
        plain: String,
        context: String,
    ): String {
        val nonce = ByteArray(NONCE_BYTES).also(random::nextBytes)
        val cipher = cipher(Cipher.ENCRYPT_MODE, nonce, context)
        return VERSION + Base64.getEncoder().encodeToString(nonce + cipher.doFinal(plain.toByteArray()))
    }

    /** The plain value. Throws for anything not sealed by this key with this [context]. */
    fun decrypt(
        sealed: String,
        context: String,
    ): String {
        require(sealed.startsWith(VERSION)) { "unknown format" }
        val bytes = Base64.getDecoder().decode(sealed.removePrefix(VERSION))
        val cipher = cipher(Cipher.DECRYPT_MODE, bytes.copyOfRange(0, NONCE_BYTES), context)
        return String(cipher.doFinal(bytes, NONCE_BYTES, bytes.size - NONCE_BYTES))
    }

    private fun cipher(
        mode: Int,
        nonce: ByteArray,
        context: String,
    ): Cipher =
        Cipher.getInstance("AES/GCM/NoPadding").apply {
            init(mode, key, GCMParameterSpec(TAG_BITS, nonce))
            updateAAD(context.toByteArray())
        }

    companion object {
        const val KEY_BYTES = 32
        private const val NONCE_BYTES = 12
        private const val TAG_BITS = 128
        private const val VERSION = "v1:"
    }
}
