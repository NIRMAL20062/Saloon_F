# 📱 Android App: To Do

Kotlin · Jetpack Compose + Material 3 · MVVM (UI → ViewModel → UseCase → Repository) · Hilt · Retrofit. Stack: [docs/TECH_STACK.md](../docs/TECH_STACK.md)
Workflow and template: [README.md](README.md) · Done so far: [APP_TASKS_COMPLETED.md](APP_TASKS_COMPLETED.md)

## Phase 0: Walking skeleton (approved scope, D-006)

### APP-003 · System status screen (end-to-end proof)
- **Phase:** 0 · **Status:** 🔄 Built, waiting for the team's phone check · **Owner:** Claude · **Depends on:** APP-002, BE-007 · **Commits:** `f8280b2`
- **Why:** proves the whole chain works (screen → ViewModel → UseCase → Repository → API → Postgres) before real features
- **Scope:**
  - start screen shows backend status, database status and backend version
  - Loading, Success and Error states, plus a Retry button
- **Done when:**
  - [x] Tests: ViewModel unit tests for every state; Compose UI tests (Robolectric) for every state (`StatusViewModelTest` 6, `StatusScreenTest` 7, `NetworkHealthRepositoryTest` 4, `AppLaunchTest` with Hilt fake)
  - [ ] Flow: on the phone (or an emulator) against the local backend, the screen shows `UP` ✅ (verified on moto g54); stopping Postgres shows `DOWN` ✅ (verified); stopping the backend shows the error state with Retry ⏳ (covered by tests, **team to confirm on phone**)
  - [x] Security: error messages shown to the user never include raw server responses (`serverErrorShowsOnlyOurTextWithStatusAndReference`)
  - [x] Database: none

## Phase 1: Outline only (details after Q-001, Q-003 in [DECISIONS.md](../docs/DECISIONS.md))

- ⬜ **APP-1xx** Login (provider: Q-007; plan said phone OTP) → backend session, stored securely
- ⬜ **APP-1xx** Firebase project setup: Crashlytics + Analytics (in the plan, to prove design-partner usage; free Spark plan).
  _Needs from team: create a Firebase project, add the app, send `google-services.json` privately (never via git)._
- ⬜ **APP-1xx** **Offline plan**: decide which screens/actions must work without internet (salons often have weak signal), then
  build them with Room (local cache + drafts) and WorkManager (sync + retry), both already in the stack.
  _Needs from team: after the feature list, mark which screens must work offline._
- ⬜ **APP-1xx** **Indian languages**: all app text is already in resource files; add translations + an in-app language switch.
  _Needs from team: which languages (e.g. Hindi, Marathi…); a native speaker to review the translations I draft._

## Pre-launch: Outline only

- ⬜ **APP-9xx** **Staging build**: separate app (`com.glide.android.staging`) pointing at staging, with a visible **STAGING** banner,
  shipped to testers through Firebase App Distribution by CD. _Needs from team: Firebase project, testers' emails, a Firebase
  service-account key added as a GitHub secret (exact steps given then)._
- ⬜ **APP-9xx** Launcher icon + brand colours/fonts. _Needs from team: icon and designs from Figma._
- ⬜ **APP-9xx** Release signing + Play Store listing. _Needs from team: Google Play Console account (one-time US$25 fee, **not free**),
  generating and safely storing the upload keystore (I'll give the commands), store texts and screenshots._

## Features: added by the team

<!-- Add feature tasks here using the template in README.md and ChatGPT.md-->
