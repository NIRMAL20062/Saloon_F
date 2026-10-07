package com.glide.backend.config

import java.security.MessageDigest

/** Key material from the environment. Never printed, also not inside [AppConfig]'s toString. */
class SecretBytes(
    private val bytes: ByteArray,
) {
    val size: Int get() = bytes.size

    /** A copy, so the caller can't change the key. */
    fun copy(): ByteArray = bytes.copyOf()

    override fun toString() = "***"

    override fun equals(other: Any?) = other is SecretBytes && MessageDigest.isEqual(bytes, other.bytes)

    override fun hashCode() = bytes.contentHashCode()
}
