package com.glide.android.testing

import com.glide.android.data.auth.AuthResult
import com.glide.android.data.auth.PhoneLogin
import com.glide.android.data.auth.Session
import kotlinx.coroutines.flow.MutableStateFlow

/** Records calls and answers with whatever the test sets. */
class FakePhoneLogin(
    signedIn: Session? = null,
) : PhoneLogin {
    override val session = MutableStateFlow(signedIn)

    var otpResult: AuthResult<Unit> = AuthResult.Success(Unit)
    var verifyResult: AuthResult<Session>? = null
    val otpRequests = mutableListOf<String>()
    val verifications = mutableListOf<Pair<String, String>>()
    var logouts = 0

    override suspend fun requestOtp(phone: String): AuthResult<Unit> {
        otpRequests += phone
        return otpResult
    }

    override suspend fun verifyOtp(
        phone: String,
        code: String,
    ): AuthResult<Session> {
        verifications += phone to code
        val result = verifyResult ?: AuthResult.Success(SESSION)
        if (result is AuthResult.Success) session.value = result.value
        return result
    }

    override suspend fun logout() {
        logouts++
        session.value = null
    }

    companion object {
        val SESSION = Session("access", "refresh", Long.MAX_VALUE, "user-1", "919000000001")
    }
}
