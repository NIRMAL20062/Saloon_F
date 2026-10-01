# 📱 Android App: To Do

Kotlin · Jetpack Compose + Material 3 · MVVM (UI → ViewModel → UseCase → Repository) · Hilt · Retrofit. Stack: [docs/TECH_STACK.md](../docs/TECH_STACK.md)
Workflow and template: [README.md](README.md) · Done so far: [APP_TASKS_COMPLETED.md](APP_TASKS_COMPLETED.md)

## Phase 0: Walking skeleton (approved scope, D-006)

### APP-003 · System status screen (end-to-end proof)
- **Phase:** 0 · **Status:** 🔄 In progress · **Owner:** Claude · **Depends on:** APP-002, BE-007
- **Why:** proves the whole chain works (screen → ViewModel → UseCase → Repository → API → Postgres) before real features
- **Scope:**
  - start screen shows backend status, database status and backend version
  - Loading, Success and Error states, plus a Retry button
- **Done when:**
  - [ ] Tests: ViewModel unit tests for every state; Compose UI tests (Robolectric) for every state
  - [ ] Flow: on the phone (or an emulator) against the local backend, the screen shows `UP`; stopping Postgres shows `DOWN`; stopping the backend shows the error state with Retry
  - [ ] Security: error messages shown to the user never include raw server responses
  - [ ] Database: none

## Phase 1: Outline only (details after Q-001, Q-003 in [DECISIONS.md](../docs/DECISIONS.md))

- ⬜ **APP-1xx** Login with phone OTP (Firebase Auth) → backend JWT, stored securely
- ⬜ **APP-1xx** Firebase project setup: Crashlytics + Analytics (needs `google-services.json` from the team)

## Features: added by the team

<!-- Add feature tasks here using the template in README.md and ChatGPT.md-->
