# 📱 Android App: To Do

Kotlin · Jetpack Compose + Material 3 · MVVM (UI → ViewModel → UseCase → Repository) · Hilt · Retrofit. Stack: [docs/TECH_STACK.md](../docs/TECH_STACK.md)
Workflow and template: [README.md](README.md) · Done so far: [APP_TASKS_COMPLETED.md](APP_TASKS_COMPLETED.md)

## Phase 0: Walking skeleton (approved scope, D-006)

### APP-002 · Network layer on the shared contract
- **Phase:** 0 · **Status:** ⬜ To do · **Depends on:** APP-001, BE-008
- **Why:** every feature talks to the backend the same way, with the same models as the server
- **Scope:**
  - Retrofit + OkHttp + kotlinx.serialization using `shared` DTOs and `ApiJson` (no duplicated models)
  - API base URL per build type: debug → `http://10.0.2.2:8080` (emulator → laptop), release → HTTPS URL (placeholder until Q-004)
  - every request sends an `X-Request-Id` so app and server logs line up
  - backend error envelope mapped to one app-side error type
  - HTTP body logging in debug builds only
- **Done when:**
  - [ ] Tests: MockWebServer tests for success, 503, error envelope and no network
  - [ ] Security: network security config allows cleartext **only** to `10.0.2.2` and **only** in debug; release is HTTPS-only
  - [ ] Database: none

### APP-003 · System status screen (end-to-end proof)
- **Phase:** 0 · **Status:** ⬜ To do · **Depends on:** APP-002, BE-007
- **Why:** proves the whole chain works (screen → ViewModel → UseCase → Repository → API → Postgres) before real features
- **Scope:**
  - start screen shows backend status, database status and backend version
  - Loading, Success and Error states, plus a Retry button
- **Done when:**
  - [ ] Tests: ViewModel unit tests for every state; Compose UI tests (Robolectric) for every state
  - [ ] Flow: on an emulator against the local backend, the screen shows `UP`; stopping Postgres shows `DOWN`; stopping the backend shows the error state with Retry
  - [ ] Security: error messages shown to the user never include raw server responses
  - [ ] Database: none

## Phase 1: Outline only (details after Q-001, Q-003 in [DECISIONS.md](../docs/DECISIONS.md))

- ⬜ **APP-1xx** Login with phone OTP (Firebase Auth) → backend JWT, stored securely
- ⬜ **APP-1xx** Firebase project setup: Crashlytics + Analytics (needs `google-services.json` from the team)

## Features: added by the team

<!-- Add feature tasks here using the template in README.md and ChatGPT.md-->
