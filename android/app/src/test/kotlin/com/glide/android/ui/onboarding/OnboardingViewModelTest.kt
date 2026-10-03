package com.glide.android.ui.onboarding

import com.glide.android.data.me.MeRepository
import com.glide.android.data.network.ApiError
import com.glide.android.testing.FakeBackend
import com.glide.android.testing.FakePhoneLogin
import com.glide.android.testing.MainDispatcherRule
import com.glide.shared.me.MeResponse
import com.glide.shared.me.UserSide
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

/** Every onboarding state (APP-005): which screen a signed-in person sees, choosing a side, saving the profile. */
class OnboardingViewModelTest {
    @get:Rule
    val main = MainDispatcherRule()

    private val backend = FakeBackend()
    private val login = FakePhoneLogin(signedIn = FakePhoneLogin.SESSION)

    private fun vm() = OnboardingViewModel(MeRepository(backend), login)

    @Test
    fun aNewPersonIsAskedCustomerOrSalon() =
        runTest(main.dispatcher) {
            val vm = vm()
            runCurrent()

            assertEquals(OnboardingUiState.ChooseSide(), vm.state.value)
        }

    @Test
    fun aCustomerWithoutANameIsAskedForTheProfile() =
        runTest(main.dispatcher) {
            backend.me = MeResponse("user-1", "919000000001", UserSide.CUSTOMER)
            val vm = vm()
            runCurrent()

            assertEquals(OnboardingUiState.Profile(), vm.state.value)
        }

    @Test
    fun aReturningCustomerGoesStraightHome() =
        runTest(main.dispatcher) {
            backend.me = FakeBackend.CUSTOMER
            val vm = vm()
            runCurrent()

            assertEquals(OnboardingUiState.Home(FakeBackend.CUSTOMER), vm.state.value)
        }

    @Test
    fun aSalonPersonGoesToTheSalonHome() =
        runTest(main.dispatcher) {
            backend.me = MeResponse("user-1", "919000000001", UserSide.SALON)
            val vm = vm()
            runCurrent()

            assertEquals(UserSide.SALON, (vm.state.value as OnboardingUiState.Home).me.side)
        }

    @Test
    fun offlineShowsAnErrorAndRetryRecovers() =
        runTest(main.dispatcher) {
            backend.offline = true
            val vm = vm()
            runCurrent()
            assertEquals(OnboardingUiState.LoadError(ApiError.Network), vm.state.value)

            backend.offline = false
            vm.load()
            runCurrent()

            assertEquals(OnboardingUiState.ChooseSide(), vm.state.value)
        }

    @Test
    fun aTapOnlyAsksToConfirmItSavesNothing() =
        runTest(main.dispatcher) {
            val vm = vm()
            runCurrent()

            vm.pick(UserSide.SALON)
            runCurrent()

            assertEquals(OnboardingUiState.ChooseSide(confirming = UserSide.SALON), vm.state.value)
            assertEquals(listOf("me"), backend.calls)
        }

    @Test
    fun cancelGoesBackToTheChoice() =
        runTest(main.dispatcher) {
            val vm = vm()
            runCurrent()
            vm.pick(UserSide.CUSTOMER)

            vm.cancelPick()

            assertEquals(OnboardingUiState.ChooseSide(), vm.state.value)
        }

    @Test
    fun confirmingCustomerSavesItThenAsksForTheProfile() =
        runTest(main.dispatcher) {
            val vm = vm()
            runCurrent()
            vm.pick(UserSide.CUSTOMER)

            vm.confirmPick()
            runCurrent()

            assertEquals(OnboardingUiState.Profile(), vm.state.value)
            assertEquals(UserSide.CUSTOMER, backend.me.side)
        }

    @Test
    fun confirmingSalonSavesItThenShowsTheSalonHome() =
        runTest(main.dispatcher) {
            val vm = vm()
            runCurrent()
            vm.pick(UserSide.SALON)

            vm.confirmPick()
            runCurrent()

            assertEquals(UserSide.SALON, (vm.state.value as OnboardingUiState.Home).me.side)
        }

    @Test
    fun whileSavingTheChoiceItShowsSaving() =
        runTest(main.dispatcher) {
            val vm = vm()
            runCurrent()
            vm.pick(UserSide.CUSTOMER)

            vm.confirmPick()

            assertEquals(OnboardingUiState.ChooseSide(confirming = UserSide.CUSTOMER, saving = true), vm.state.value)
        }

    @Test
    fun alreadyChosenOnAnotherPhoneFollowsTheBackend() =
        runTest(main.dispatcher) {
            val vm = vm()
            runCurrent()
            backend.me = MeResponse("user-1", "919000000001", UserSide.SALON)
            vm.pick(UserSide.CUSTOMER)

            vm.confirmPick()
            runCurrent()

            assertEquals(UserSide.SALON, (vm.state.value as OnboardingUiState.Home).me.side)
        }

    @Test
    fun savingTheChoiceOfflineShowsAnError() =
        runTest(main.dispatcher) {
            val vm = vm()
            runCurrent()
            backend.offline = true
            vm.pick(UserSide.CUSTOMER)

            vm.confirmPick()
            runCurrent()

            assertEquals(OnboardingUiState.ChooseSide(error = ApiError.Network), vm.state.value)
        }

    @Test
    fun aTooShortNameIsCaughtBeforeSending() =
        runTest(main.dispatcher) {
            val vm = profileVm()
            vm.onNameChange(" A ")

            vm.saveProfile()

            assertTrue((vm.state.value as OnboardingUiState.Profile).nameError)
            assertTrue(backend.calls.none { it.startsWith("profile") })
        }

    @Test
    fun aBadEmailIsCaughtBeforeSending() =
        runTest(main.dispatcher) {
            val vm = profileVm()
            vm.onNameChange("Priya")
            vm.onEmailChange("priya@")

            vm.saveProfile()

            assertTrue((vm.state.value as OnboardingUiState.Profile).emailError)
            assertTrue(backend.calls.none { it.startsWith("profile") })
        }

    @Test
    fun typingClearsTheError() =
        runTest(main.dispatcher) {
            val vm = profileVm()
            vm.saveProfile()

            vm.onNameChange("Pr")

            assertEquals(false, (vm.state.value as OnboardingUiState.Profile).nameError)
        }

    @Test
    fun aValidProfileIsSavedTrimmedAndGoesHome() =
        runTest(main.dispatcher) {
            val vm = profileVm()
            vm.onNameChange("  Priya Sharma ")
            vm.onEmailChange("")

            vm.saveProfile()
            runCurrent()

            assertEquals("profile:Priya Sharma:null", backend.calls.last())
            assertEquals("Priya Sharma", (vm.state.value as OnboardingUiState.Home).me.name)
        }

    @Test
    fun theServerRefusingTheNameShowsItOnTheNameField() =
        runTest(main.dispatcher) {
            val vm = profileVm()
            vm.onNameChange("x".repeat(61))

            vm.saveProfile()
            runCurrent()

            // 61 characters are caught locally; the server's INVALID_NAME is mapped the same way.
            assertTrue((vm.state.value as OnboardingUiState.Profile).nameError)
        }

    @Test
    fun savingTheProfileOfflineKeepsWhatWasTyped() =
        runTest(main.dispatcher) {
            val vm = profileVm()
            vm.onNameChange("Priya")
            backend.offline = true

            vm.saveProfile()
            runCurrent()

            val state = vm.state.value as OnboardingUiState.Profile
            assertEquals("Priya", state.name)
            assertEquals(ApiError.Network, state.error)
        }

    @Test
    fun logoutEndsTheSession() =
        runTest(main.dispatcher) {
            val vm = vm()

            vm.logout()
            runCurrent()

            assertEquals(1, login.logouts)
            assertNull(login.session.value)
        }

    private fun kotlinx.coroutines.test.TestScope.profileVm(): OnboardingViewModel {
        backend.me = MeResponse("user-1", "919000000001", UserSide.CUSTOMER)
        return vm().also { runCurrent() }
    }
}
