# android/: Glide Android app

**One app for everyone (D-023):** onboarding asks "customer or salon?" and the app shows that side (D-024). Customer-side screens
live under `ui/customer/`, salon-side screens under `ui/salon/`, shared ones (login, onboarding) under `ui/auth/` and `ui/common/`.

Kotlin · Jetpack Compose + Material 3 · MVVM, layers UI → ViewModel → UseCase → Repository · Hilt · Navigation Compose ·
Coroutines + Flow · Retrofit + OkHttp + kotlinx.serialization. minSdk 24, compile/target SDK 37. Stack: [docs/TECH_STACK.md](../docs/TECH_STACK.md)

Module `:android:app` → `android/app/`. Package `com.glide.android`: `ui/<feature>/` screens + ViewModels,
`domain/<feature>/` use cases, `data/` repositories and the network layer (`data/network/`: Retrofit, `apiCall`, `ApiResult`),
`navigation/` routes, `ui/theme/` Material 3 theme. Example of every layer: the status screen (APP-003).

## Rules

- **Models come from `:shared`.** Never redeclare an API request/response class in the app.
- Layers: Composables only render state and send events. ViewModels expose one `StateFlow<UiState>`. Use cases hold app
  logic. Repositories talk to Retrofit / Room and return results, never throw to the UI.
- **No secrets in the app**: no API keys, Razorpay secret, WhatsApp tokens, signing passwords, in code, `BuildConfig` or
  resources. The app only knows the backend URL (rule from the product plan).
- **Never call WhatsApp or create Razorpay orders from the app.** Ask the backend.
- Release builds are HTTPS-only. Cleartext is allowed only to localhost in debug (phone/emulator reach the laptop via `adb reverse tcp:8080 tcp:8080`).
- Add libraries (Room, WorkManager, Coil, Firebase, Razorpay) only in the task that needs them, through `gradle/libs.versions.toml`.
- Strings in `res/values/strings.xml`, never hard-coded in Composables.
- **Look and feel (D-031):** screens should feel modern, smooth and interactive. From APP-011 on, build them only from the design
  system (`ui/theme/`, `ui/components/`): no one-off colours, sizes or animations; every screen has loading, empty and error states.
- **Sides and roles:** the customer/salon choice is final, with no "switch side" (D-030). On the salon side, staff see only their own
  appointments (D-039); hiding a button is never the only guard, the backend refuses too.

## Tests

- ViewModel: JUnit 4 + `kotlinx-coroutines-test`, fake repositories, one test per UI state.
- Network: MockWebServer against the real Retrofit interface and `ApiJson`.
- UI: Compose UI tests on Robolectric (no emulator needed), one per screen state.

## Commands

```bash
./gradlew :android:app:assembleDebug
./gradlew :android:app:installDebug              # phone (USB) or emulator connected
adb reverse tcp:8080 tcp:8080                    # lets the app reach the backend on this laptop
./gradlew :android:app:testDebugUnitTest
./gradlew :android:app:lintDebug
```
