package com.glide.android.data.auth

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Storage logic on Robolectric. The Android Keystore itself doesn't exist on the JVM, so a stand-in cipher is used here;
 * the real KeystoreTokenCipher is exercised on the phone (the login flow in the APP-004 report).
 */
@RunWith(AndroidJUnit4::class)
class EncryptedSessionStoreTest {
    private val prefs =
        ApplicationProvider.getApplicationContext<Context>().getSharedPreferences("test_session", Context.MODE_PRIVATE)
    private val store = EncryptedSessionStore(prefs, ReversingCipher())
    private val session = Session("access-secret", "refresh-secret", 1_700_000_000, "user-1", "919000000001")

    @Test
    fun savedSessionIsLoadedBack() {
        store.save(session)

        assertEquals(session, EncryptedSessionStore(prefs, ReversingCipher()).load())
    }

    @Test
    fun tokensAreNeverStoredInPlainText() {
        store.save(session)

        val raw = prefs.all.values.joinToString()
        assertFalse(raw.contains("access-secret"))
        assertFalse(raw.contains("refresh-secret"))
    }

    @Test
    fun clearSignsOut() {
        store.save(session)

        store.clear()

        assertNull(store.load())
    }

    @Test
    fun unreadableDataMeansSignedOutAndIsRemoved() {
        prefs.edit().putString("session", "not-valid-base64-or-cipher").commit()

        assertNull(store.load())
        assertNull(prefs.getString("session", null))
    }

    /** Stand-in for the Keystore cipher: reversible and not plain text. */
    private class ReversingCipher : TokenCipher {
        override fun encrypt(plain: ByteArray) =
            plain.reversedArray().map { (it.toInt() xor 0x5A).toByte() }.toByteArray()

        override fun decrypt(sealed: ByteArray) =
            sealed.map { (it.toInt() xor 0x5A).toByte() }.toByteArray().reversedArray()
    }
}
