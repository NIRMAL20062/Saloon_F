# android/: Glide Android app

Kotlin · Jetpack Compose + Material 3 · MVVM, layers UI → ViewModel → UseCase → Repository · Hilt · Navigation Compose ·
Coroutines + Flow · Retrofit + OkHttp + kotlinx.serialization. minSdk 24, compile/target SDK 37. Stack: [docs/TECH_STACK.md](../docs/TECH_STACK.md)

Module `:android:app` → `android/app/`. Package `com.glide.android`: `ui/<feature>/` screens + ViewModels,
`navigation/` routes, `ui/theme/` Material 3 theme. More packages (`data/`, `domain/`) arrive with APP-002/003.

## Rules

- **Models come from `:shared`.** Never redeclare an API request/response class in the app.
- Layers: Composables only render state and send events. ViewModels expose one `StateFlow<UiState>`. Use cases hold app
  logic. Repositories talk to Retrofit / Room and return results, never throw to the UI.
- **No secrets in the app**: no API keys, Razorpay secret, WhatsApp tokens, signing passwords, in code, `BuildConfig` or
  resources. The app only knows the backend URL (rule from the product plan).
- **Never call WhatsApp or create Razorpay orders from the app.** Ask the backend.
- Release builds are HTTPS-only. Cleartext is allowed only to `10.0.2.2` in debug.
- Add libraries (Room, WorkManager, Coil, Firebase, Razorpay) only in the task that needs them, through `gradle/libs.versions.toml`.
- Strings in `res/values/strings.xml`, never hard-coded in Composables.

## Tests

- ViewModel: JUnit 4 + `kotlinx-coroutines-test`, fake repositories, one test per UI state.
- Network: MockWebServer against the real Retrofit interface and `ApiJson`.
- UI: Compose UI tests on Robolectric (no emulator needed), one per screen state.

## Commands

```bash
./gradlew :android:app:assembleDebug
./gradlew :android:app:installDebug              # emulator running; backend on the laptop at :8080
./gradlew :android:app:testDebugUnitTest
./gradlew :android:app:lintDebug
```
