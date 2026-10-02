package com.glide.android.data.auth

import android.content.SharedPreferences
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.util.Base64
import com.glide.shared.api.ApiJson
import kotlinx.serialization.Serializable
import java.security.KeyStore
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec

/** A signed-in session. Never logged, never put in analytics. */
@Serializable
data class Session(
    val accessToken: String,
    val refreshToken: String,
    /** Seconds since 1970 when [accessToken] stops working. */
    val expiresAtEpochSeconds: Long,
    val userId: String,
    /** As Supabase returns it, e.g. `919000000001`. */
    val phone: String?,
)

interface SessionStore {
    fun load(): Session?

    fun save(session: Session)

    fun clear()
}

fun interface EpochClock {
    fun nowEpochSeconds(): Long
}

/** Encrypts and decrypts small secrets. Production: a key that never leaves the phone's Android Keystore. */
interface TokenCipher {
    fun encrypt(plain: ByteArray): ByteArray

    fun decrypt(sealed: ByteArray): ByteArray
}

/** Stores the session encrypted in private app storage (backups are already off for the whole app). */
class EncryptedSessionStore(
    private val prefs: SharedPreferences,
    private val cipher: TokenCipher,
) : SessionStore {
    override fun load(): Session? {
        val stored = prefs.getString(KEY, null) ?: return null
        // Anything unreadable (key wiped, app data restored elsewhere, old format) simply means "signed out".
        return runCatching {
            ApiJson.decodeFromString<Session>(cipher.decrypt(Base64.decode(stored, Base64.NO_WRAP)).decodeToString())
        }.getOrElse {
            clear()
            null
        }
    }

    override fun save(session: Session) {
        val sealed = cipher.encrypt(ApiJson.encodeToString(session).encodeToByteArray())
        prefs.edit().putString(KEY, Base64.encodeToString(sealed, Base64.NO_WRAP)).apply()
    }

    override fun clear() {
        prefs.edit().remove(KEY).apply()
    }

    private companion object {
        const val KEY = "session"
    }
}

/** AES-256-GCM with a key generated inside the Android Keystore; the key can't be read out of the phone. */
class KeystoreTokenCipher : TokenCipher {
    override fun encrypt(plain: ByteArray): ByteArray {
        val cipher = Cipher.getInstance(TRANSFORMATION).apply { init(Cipher.ENCRYPT_MODE, key()) }
        return cipher.iv + cipher.doFinal(plain)
    }

    override fun decrypt(sealed: ByteArray): ByteArray {
        val iv = sealed.copyOfRange(0, IV_BYTES)
        val cipher =
            Cipher.getInstance(TRANSFORMATION).apply {
                init(Cipher.DECRYPT_MODE, key(), GCMParameterSpec(TAG_BITS, iv))
            }
        return cipher.doFinal(sealed.copyOfRange(IV_BYTES, sealed.size))
    }

    private fun key(): SecretKey {
        val keyStore = KeyStore.getInstance(ANDROID_KEYSTORE).apply { load(null) }
        (keyStore.getKey(ALIAS, null) as? SecretKey)?.let { return it }
        return KeyGenerator
            .getInstance(KeyProperties.KEY_ALGORITHM_AES, ANDROID_KEYSTORE)
            .apply {
                init(
                    KeyGenParameterSpec
                        .Builder(ALIAS, KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT)
                        .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
                        .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
                        .setKeySize(KEY_BITS)
                        .build(),
                )
            }.generateKey()
    }

    private companion object {
        const val ANDROID_KEYSTORE = "AndroidKeyStore"
        const val ALIAS = "glide_session_key"
        const val TRANSFORMATION = "AES/GCM/NoPadding"
        const val IV_BYTES = 12
        const val TAG_BITS = 128
        const val KEY_BITS = 256
    }
}
