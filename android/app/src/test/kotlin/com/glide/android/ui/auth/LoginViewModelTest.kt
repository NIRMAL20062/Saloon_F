package com.glide.android.ui.auth

import com.glide.android.data.auth.AuthError
import com.glide.android.data.auth.AuthResult
import com.glide.android.testing.FakePhoneLogin
import com.glide.android.testing.MainDispatcherRule
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

/** Every login state and transition, with a fake Supabase login and virtual time for the resend countdown. */
class LoginViewModelTest {
    @get:Rule
    val mainDispatcher = MainDispatcherRule()

    private val login = FakePhoneLogin()
    private val vm = LoginViewModel(login)

    @Test
    fun `starts on the phone step`() {
        assertEquals(LoginUiState.EnterPhone(), vm.state.value)
    }

    @Test
    fun `phone keeps digits only, at most ten`() {
        vm.onPhoneChange("+91 98765-43210 99")

        assertEquals("9198765432", (vm.state.value as LoginUiState.EnterPhone).phone)
    }

    @Test
    fun `an invalid number is refused without calling Supabase`() {
        vm.onPhoneChange("12345")
        vm.onSendCode()
        vm.onPhoneChange("5123456789") // Indian mobiles start with 6-9
        vm.onSendCode()

        assertEquals(AuthError.INVALID_PHONE, (vm.state.value as LoginUiState.EnterPhone).error)
        assertTrue(login.otpRequests.isEmpty())
    }

    @Test
    fun `a valid number asks for a code with +91 and moves to the code step`() =
        runTest(mainDispatcher.dispatcher) {
            vm.onPhoneChange("9000000001")
            vm.onSendCode()
            runCurrent()

            assertEquals(listOf("919000000001"), login.otpRequests)
            assertEquals(
                LoginUiState.EnterCode(phone = "9000000001", resendInSeconds = RESEND_WAIT_SECONDS),
                vm.state.value,
            )
        }

    @Test
    fun `too many requests keeps the phone step with a message`() =
        runTest(mainDispatcher.dispatcher) {
            login.otpResult = AuthResult.Failure(AuthError.RATE_LIMITED)
            vm.onPhoneChange("9000000001")

            vm.onSendCode()
            runCurrent()

            assertEquals(LoginUiState.EnterPhone("9000000001", AuthError.RATE_LIMITED, loading = false), vm.state.value)
        }

    @Test
    fun `resend unlocks after 60 seconds and restarts the wait`() =
        runTest(mainDispatcher.dispatcher) {
            reachCodeStep()
            vm.onResend() // too early: ignored
            assertEquals(1, login.otpRequests.size)

            advanceTimeBy(RESEND_WAIT_SECONDS * 1_000L + 1)
            assertEquals(0, (vm.state.value as LoginUiState.EnterCode).resendInSeconds)

            vm.onResend()
            runCurrent()
            assertEquals(2, login.otpRequests.size)
            assertEquals(RESEND_WAIT_SECONDS, (vm.state.value as LoginUiState.EnterCode).resendInSeconds)
        }

    @Test
    fun `code keeps digits only, at most six, and verify needs all six`() =
        runTest(mainDispatcher.dispatcher) {
            reachCodeStep()

            vm.onCodeChange("12a3")
            vm.onVerify()
            runCurrent()

            assertEquals("123", (vm.state.value as LoginUiState.EnterCode).code)
            assertTrue(login.verifications.isEmpty())
        }

    @Test
    fun `a wrong code shows a message and clears the field`() =
        runTest(mainDispatcher.dispatcher) {
            reachCodeStep()
            login.verifyResult = AuthResult.Failure(AuthError.INVALID_CODE)

            vm.onCodeChange("000000")
            vm.onVerify()
            runCurrent()

            val state = vm.state.value as LoginUiState.EnterCode
            assertEquals(AuthError.INVALID_CODE, state.error)
            assertEquals("", state.code)
        }

    @Test
    fun `the right code signs in`() =
        runTest(mainDispatcher.dispatcher) {
            reachCodeStep()

            vm.onCodeChange("123456")
            vm.onVerify()
            runCurrent()

            assertEquals(listOf("919000000001" to "123456"), login.verifications)
            assertNotNull(login.session.value)
        }

    @Test
    fun `change number goes back with the number kept`() =
        runTest(mainDispatcher.dispatcher) {
            reachCodeStep()

            vm.onChangeNumber()
            advanceUntilIdle()

            assertEquals(LoginUiState.EnterPhone(phone = "9000000001"), vm.state.value)
        }

    private fun kotlinx.coroutines.test.TestScope.reachCodeStep() {
        vm.onPhoneChange("9000000001")
        vm.onSendCode()
        runCurrent()
    }
}
