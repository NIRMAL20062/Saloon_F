# 📱 Android App: Completed

Newest at the bottom. Each entry keeps the commits so anyone can `git show <hash>` to see exactly what changed.

> Groundwork the app already relies on (done in the backend/platform files):
> [BE-003](BACKEND_TASKS_COMPLETED.md#be-003--gradle-build) Gradle build ·
> [BE-004](BACKEND_TASKS_COMPLETED.md#be-004--shared-api-contract-module) shared API contract (the app reuses these models)

## Phase 0: Walking skeleton

### APP-001 · Android project skeleton
- **Completed:** 2026-10-01 · **Commits:** `a0ad079`
- **Built:** `android/app` in the root Gradle build: app name **Glide**, `com.glide.android`, minSdk 24, compile/target SDK 37; Material 3 theme (baseline/dynamic colours until Figma designs), Hilt (`GlideApplication`, `MainActivity`), type-safe Navigation Compose (`HomeRoute` → `HomeScreen`); debug + release (R8 minify + resource shrinking, 968 KB APK); `GLIDE_BACKEND_ONLY=true` skips the module for Docker
- **Tests:** `AppLaunchTest` (1): launches the real `MainActivity` on Robolectric, so the Hilt graph, theme and navigation are all exercised without an emulator
- **Verified on device:** moto g54 5G, Android 15 (API 35): installs, shows the start screen, cold start 2.0 s
- **Security:** `allowBackup=false`, `fullBackupContent=false`, data-extraction rules exclude everything from cloud backup and device transfer; no secrets in code, `BuildConfig` or resources; no INTERNET permission yet (comes with APP-002)
- **Database:** none
- **Fixes along the way:** Espresso pinned to 3.7.0 (Compose's transitive 3.5.0 calls `InputManager.getInstance()`, removed in recent Android); test JVM `--add-opens java.base/jdk.internal.access` for Robolectric on JDK 25
- **Follow-up (needs design):** launcher icon. Lint warns `MissingApplicationIcon` until the Figma asset exists

### APP-002 · Network layer on the shared contract
- **Completed:** 2026-10-01 · **Commits:** `e67dbdd` (DF-10 change), `41de918`
- **Built:** `data/network/`: `GlideApi` (Retrofit, paths + models from `:shared`), `createOkHttpClient` / `createRetrofit` (plain functions, so tests use the exact app setup), `RequestIdInterceptor` (`X-Request-Id: android-<uuid>`), `apiCall {}` → `ApiResult` / `ApiError` (`Network`, `Http(status, code, requestId)`, `Unexpected`), Hilt `NetworkModule`; `BuildConfig.API_BASE_URL` per build type
- **Tests:** `NetworkLayerTest` (8, MockWebServer): success, unknown fields ignored, error envelope, non-envelope error, no connection, contract violation, request ID format accepted by backend, `/health` 503 body readable. `NetworkSecurityConfigTest` (3): release has no cleartext, debug cleartext only to localhost, base URL per build type
- **Security:** release HTTPS-only with system CAs; debug cleartext only to `127.0.0.1`/`localhost` (reached via `adb reverse`); body logging debug-only with `Authorization` redacted; raw server text never leaves the network layer (only error **codes**); release URL is a non-resolving `.invalid` placeholder until Q-004
- **Database:** none
- **Changed from plan:** debug URL is `http://127.0.0.1:8080` + `adb reverse` instead of `10.0.2.2`, so real phones work too (DF-10 updated)

### APP-003 · System status screen (end-to-end proof)
- **Completed:** 2026-10-02 · **Commits:** `f8280b2` (+ `c052ed4` task notes)
- **Built:** `ui/status/` (`StatusRoute`, stateless `StatusScreen`, `StatusViewModel` with Loading/Loaded/Error), `domain/status/GetSystemStatusUseCase` → `SystemStatus`, `data/health/NetworkHealthRepository` (503 from our backend = "database DOWN"; 503 from anything else = HTTP error), Hilt `DataModule`. Start route now opens the status screen
- **Tests:** `StatusViewModelTest` (6), `StatusScreenTest` (7, Robolectric), `NetworkHealthRepositoryTest` (4, MockWebServer); `AppLaunchTest` runs the full Hilt graph with a fake repository. App total: 29 tests
- **Verified on device (Claude, moto g54 5G, against the Docker backend):** "Server UP / Database UP / 0.1.0"; after `docker-compose stop postgres` + Refresh: "Database DOWN" with explanation. Backend-stopped → error + Retry is covered by tests and was in the team's phone-test steps; the team said to move ahead on 2026-10-02
- **Security:** only the app's own wording is shown; HTTP status + request ID for support; raw server text never displayed
- **Database:** none
- **Fix found by tests:** Retry now switches to Loading synchronously, so a stale result is never shown after the tap

## Phase 1: Login and onboarding

### APP-004 · Phone login (everyone)
- **Completed:** 2026-10-03 (team's OK and merge) · **Commits:** `562e030` `9b48aaf` `493f463` `777caed` `371deba` `eb813fc` `ad36953` `223eccc` `8c95a3c`
- **Phase:** 1 · **Status:** ✅ Done · **Owner:** Claude · **Depends on:** BE-016 · Spec: PRODUCT §4
- **Why:** customers and salon people all sign in the same way: phone number + OTP.
- **Needs from team:** Supabase project with the phone provider on (Twilio) and test phone numbers (steps in the BE-016 report).
- **Flow:** open app → enter phone number (+91) → receive OTP (test numbers: fixed code, no SMS) → enter OTP → signed in.
  Stays signed in after closing the app; logout from the menu.
- **Look (team, 2026-10-03: "everything polished", D-031):** the login is built with the Glide theme and components now:
  brand gradient header, rounded sheet, +91 chip, six code boxes (blinking caret, pop-in digits, shake + haptic on a wrong
  code, SMS code autofill), auto-verify on the 6th digit, sliding steps, press feedback, light and dark mode.
- **Done when:**
  - [x] Screens: phone entry (10-digit Indian mobile), OTP entry (6 digits, resend after 60 s, wrong-code message), loading and error states (`LoginScreenTest` 12)
  - [x] Session stored encrypted on the device (Android Keystore AES-GCM) and refreshed automatically; logout clears it (`EncryptedSessionStoreTest`, `AuthRepositoryTest`)
  - [x] Every backend call sends the login token, refreshed once on 401; `GET /v1/me` answers with the signed-in user (`AuthenticatedClientTest`, `SignedInTest`)
  - [x] Tests: ViewModel tests for every state; Compose UI tests for every screen state and every component state (`ComponentsTest` 12); 91 app tests in total
  - [x] Security: OTP and tokens never logged (HTTP logs at BASIC level, debug only); rate-limit message shown when Supabase limits OTPs
  - [x] Flow: on the phone, log in with a test number → close and reopen (still logged in) → logout. Verified by Claude on 2026-10-03,
    moto g54 5G (Android 15, dark mode) against the local backend and the real Supabase project: test number 9000000001 →
    code sent, 60 s countdown → wrong code 111111 auto-checked, red boxes + our message → 123456 → signed in, `GET /v1/me` 200 →
    force-stop + reopen: still signed in → Log out → login screen, still logged out after another restart. No code or token in
    the app's logs or the backend log
- **Tests:** 91 app tests at the time (login ViewModel and screens, encrypted session, token on calls, components)
- **Security:** session encrypted with an Android Keystore AES-GCM key; OTP and tokens never logged; BASIC HTTP logs in debug only
- **Database:** none
- **Verified by:** Claude on the moto g54 5G (Android 15), real Supabase test number

### APP-011 · Look and feel: the rest of the design system
- **Completed:** 2026-10-03 (team's OK and merge) · **Commits:** `ff83f85` `f6f7e9c` `30955ac` `c2f5603` `35ed9c3` `76d4fc7`
- **Phase:** 1 · **Status:** ✅ Done · **Owner:** Claude · **Depends on:** APP-004 · Decision: D-031
- **Why:** the team wants screens that feel as modern and smooth as popular consumer apps. APP-004 already built the base
  (team, 2026-10-03: "everything polished"): theme in `ui/theme/` (placeholder brand colours, light + dark, type, shapes,
  spacing) and `ui/components/` (buttons with press feedback + loading, +91 phone field, 6-box OTP field, field message,
  brand header, sheet card, error state, loading skeleton). This task adds what the next screens need, so they don't
  invent their own.
- **Needs from team:** optional: 2–3 apps whose look you like, and a brand colour / logo (one file to swap: `ui/theme/Color.kt`).
- **Scope:**
  - components: cards (incl. a salon card), chips, bottom sheet, top bar, list item, empty state, snackbar
  - a debug-only "components" screen showing every piece in light and dark
  - not included: logo and launcher icon (Pre-launch), screens of later tasks
- **Done when:**
  - [x] Tests: Compose UI test per component state (normal, disabled, loading, error), light and dark (`DesignSystemTest` 28 = 14 × light/dark, `ComponentsGalleryTest` 7, `ColorContrastTest` 3); app total 129
  - [x] Accessibility: text contrast ≥ 4.5:1 for every text/background pair, light and dark (`ColorContrastTest`); touch targets ≥ 48 dp, TalkBack labels (back button, rating), largest font size (tests + on the phone)
  - [x] Flow: on the phone, the components screen looks right in light and dark mode; the largest font still fits. Verified by
    Claude on 2026-10-03, moto g54 5G: signed in → Design components → light, dark switch, bottom sheet, "Saved" message;
    font size 2.0 (restored to the user's 1.3 afterwards) everything fits, long title shortens with "…". Found and fixed:
    status-bar icons invisible on light screens (`StatusBarIcons`)
- **Tests:** `DesignSystemTest`, `ComponentsGalleryTest`, `ColorContrastTest`
- **Security:** UI only; the components gallery is reachable only in debug builds
- **Database:** none
- **Verified by:** Claude on the moto g54 5G

### APP-013 · The mockup look: red brand, light only, welcome screen
- **Completed:** 2026-10-03 (team's OK and merge) · **Commits:** `1534c2c` `c6f2a52` `9edd2b9` `879942d` `738ffb6` `1b9099f`
- **Phase:** 1 · **Status:** ✅ Done · **Owner:** Claude · **Depends on:** APP-011 · Decisions: D-040, D-041
- **Why:** the team shared the design it wants ([docs/design/customer-flow-1.webp](../docs/design/customer-flow-1.webp)) and
  asked for no dark mode. Everything built so far takes that look, so later screens start from it.
- **Needs from team:** the welcome photo (given 2026-10-03; the team holds a licence for it, app and Play Store included).
- **Scope:**
  - theme: brand red `#E02430`, warm white, light-pink tints, Poppins font, pill buttons; light mode only (dark removed everywhere)
  - "Glide" wordmark (red G) and the three-petal leaf mark, as vectors
  - **welcome screen** (mockup 1): full-screen photo, "Look Good / Feel Amazing", "Book trusted salons near you", Get Started → phone login
  - phone and code screens like mockup 2–3: back arrow, centred wordmark, centred title, flag +91 field, red Send Code, phone
    illustration; code boxes and "Verifying automatically…"
  - all components restyled (buttons, chips, cards, the salon card as in mockup 8: photo left, rating, distance, tags,
    "₹… onwards"), signed-in and status screens, the components gallery (no dark switch)
  - not included: the mockup's own number keypad (the phone's keyboard and SMS autofill do this), favourites (♥, after the
    MVP), screens of later tasks (they're built in this look)
- **Done when:**
  - [x] Tests: welcome screen, every login state, components, contrast (light only), no dark scheme left (`WelcomeScreenTest` 2,
    `LoginScreenTest` 15, `ComponentsTest`: stays light in night mode, `ColorContrastTest`, `AppLaunchTest` welcome → login → back);
    app total 121
  - [x] Flow: on the phone, welcome → Get Started → phone → code → signed in looks like the mockup; it stays light with the
    phone in dark mode. Verified by Claude on 2026-10-03, moto g54 5G with system dark mode on, real Supabase test number.
    Found and fixed on the phone: +91 box height, keyboard not opening by itself
- **Tests:** `WelcomeScreenTest`, `LoginScreenTest` 15, light in night mode, contrast; app total 121
- **Security:** UI only; font (OFL) and the team's licensed photo listed in `android/licenses`
- **Database:** none
- **Verified by:** Claude on the moto g54 5G with system dark mode on

### APP-005 · Onboarding: customer or salon?
- **Completed:** 2026-10-04 (team's go-ahead, merged) · **Commits:** `69c0dce` `168f040` `7a0c427`
- **Phase:** 1 · **Status:** ✅ Done · **Owner:** Claude · **Depends on:** APP-004, APP-013, BE-016, BE-018 · Decisions: D-024, D-030, DF-23
- **Why:** one app, two kinds of users; each sees only their own interface.
- **Needs from team:** nothing.
- **Flow (first login only):** "How will you use Glide?"
  - **"I want to book salons"** → your name (required) + email (optional) → customer side
  - **"I run a salon"** → salon onboarding (APP-006) → salon side. Staff don't pick this: their owner adds their number (APP-012)
  - a number already added by a salon skips this screen and opens the salon side (DF-23)
  - the choice is saved on the backend and is **final**: no switching side (D-030)
- **Done when:**
  - [x] Tests: every screen state; customer path, salon path, returning user goes straight to their side (`OnboardingViewModelTest` 19,
    `OnboardingScreenTest` 11, `OnboardingFlowTest`); app total 150. The choice is confirmed in a sheet first ("You can't change
    this later"), since it's final. **Moved:** "an added staff number skips the question" needs salons and staff, so it's tested in
    BE-017 / APP-012
  - [x] Security: a customer can only read/change their own profile (backend test in BE-018)
  - [x] Flow: on the phone, new test number → "book salons" → name → customer side; reopen → straight there. Verified by Claude on
    2026-10-04, moto g54 5G, real Supabase test number, local backend: choice sheet, empty name refused, home greets by name
- **Security:** no new permissions; calls only our backend; nothing personal logged
- **Database:** none
- **Verified by:** Claude on the moto g54 5G, real Supabase test number
