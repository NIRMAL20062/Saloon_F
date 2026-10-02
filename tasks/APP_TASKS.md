# 📱 Android App: To Do

Kotlin · Jetpack Compose + Material 3 · MVVM (UI → ViewModel → UseCase → Repository) · Hilt · Retrofit. Stack: [docs/TECH_STACK.md](../docs/TECH_STACK.md)
Workflow and template: [README.md](README.md) · Done so far: [APP_TASKS_COMPLETED.md](APP_TASKS_COMPLETED.md)

## Phase 0: Walking skeleton (approved scope, D-006)

## Phase 1: Outline only (details after Q-001, Q-003 in [DECISIONS.md](../docs/DECISIONS.md))

- ⬜ **APP-1xx** Login with **phone OTP by SMS through Supabase Auth** (D-016) → backend verifies the Supabase token; session stored securely
- ⬜ **APP-1xx** Firebase project setup: Crashlytics + Analytics (in the plan, to prove design-partner usage; free Spark plan).
  _Team created the Firebase project (2026-10-02). Needs from team when this task starts: add the Android app(s) in Firebase, send `google-services.json` privately (never via git)._
- ⬜ **APP-1xx** **No-internet mode = view only (D-019)**: the salon app keeps the last loaded appointments and customers in Room
  and shows them with a "No internet, showing saved data" banner; add/edit buttons are disabled until online. _Needs from team: nothing._
- ~~**APP-1xx** Indian languages~~: **not planned, English only (D-018)**. Text stays in resource files so it can be added later.

## Pre-launch: Outline only

- ⬜ **APP-9xx** **Staging build**: separate app (`com.glide.android.staging`) pointing at staging, with a visible **STAGING** banner,
  shipped to testers through Firebase App Distribution by CD. _Needs from team: Firebase project, testers' emails, a Firebase
  service-account key added as a GitHub secret (exact steps given then)._
- ⬜ **APP-9xx** Launcher icon + brand colours/fonts. _Needs from team: icon and designs from Figma._
- ⬜ **APP-9xx** Release signing + Play Store listing. _Needs from team: Google Play Console account (one-time US$25 fee, **not free**),
  generating and safely storing the upload keystore (I'll give the commands), store texts and screenshots._

## Features: added by the team

<!-- Add feature tasks here using the template in README.md and ChatGPT.md-->
