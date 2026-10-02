# Decisions Log

Every decision that shapes the codebase is recorded here, newest at the bottom of each section.
**Rule:** if a decision isn't in the product plan ([TECH_STACK.md](TECH_STACK.md)) or below, ask the team before building it.

Status: **Decided** = agreed by the team · **Default** = picked by the developer/AI to fill a gap; anyone can veto it by opening a PR that changes this file · **Open** = waiting for an answer.

## Team decisions

| ID | Date | Decision | Notes |
|---|---|---|---|
| D-001 | 2026-10-01 | Monorepo: `/android`, `/backend`, `/shared`, `/admin`, `/docs` | Extends the plan's `/android, /backend, /docs` with D-002 and D-004 |
| D-002 | 2026-10-01 | Web app = **internal admin panel** for our team, built with **Next.js** | Not salon-facing, not customer-facing |
| D-003 | 2026-10-01 | Backend = **Ktor + Exposed**, migrations with **Flyway** | Supabase as backend-platform rejected; see Q-002 for Supabase as DB host |
| D-004 | 2026-10-01 | **Shared Kotlin module** holds the API contract (DTOs) used by Android and backend | Admin panel gets TypeScript types generated from `docs/api/openapi.yaml` |
| D-005 | 2026-10-01 | Working name **Glide**, Kotlin package `com.glide`, Android `applicationId` `com.glide.android` | Placeholder, renamable before first Play Store upload |
| D-006 | 2026-10-01 | **Phase 0 = walking skeleton**: all three apps run and talk end-to-end via `/health`; no business tables | Multi-tenant base + auth start in Phase 1 |
| D-007 | 2026-10-01 | **Small commits**: one commit per unit of work, so the team can debug and revert easily | See [DEVELOPMENT_WORKFLOW.md](DEVELOPMENT_WORKFLOW.md) |
| D-008 | 2026-10-01 | **Hosting decided later.** CD publishes build artifacts now. **Test on real services from the start**: staging uses real Postgres, Razorpay test mode, Firebase test phone numbers, and seeded test users for every user type | See [TESTING.md](TESTING.md) § Environments |
| D-009 | 2026-10-01 | **All work is driven by task files** in `tasks/`: To-do + Completed pairs for App, Web and Backend (+ Platform). Nothing is built unless it is a task there first | See [tasks/README.md](../tasks/README.md) |
| D-010 | 2026-10-02 | **Free tiers only** (database, auth, hosting, email, monitoring) until real user growth requires paid plans | Every new service must state its free-tier limits in the task that adds it |
| D-011 | 2026-10-02 | **No hosting during development.** Backend, database and admin run locally; the app is tested on team phones over USB. Hosting starts in the *Pre-launch* phase. Managed services the app needs during development (e.g. auth, Razorpay test mode) are used on their free/test tiers | Replaces the "staging" part of D-008 until Pre-launch; webhooks reach the laptop through a free tunnel when needed |
| D-012 | 2026-10-02 | **User types**: salon owner, staff/stylist, receptionist/manager, salon's customer, plus our internal admin (admin panel). Test users: one per type in **two** test salons | How customers interact (app login vs WhatsApp link only) is decided with the feature list (Q-001) |
| D-013 | 2026-10-02 | **Admin panel login by email; existing admins can add new admins** (invite-only, no self sign-up) | Provider and email method: Q-007 |
| D-014 | 2026-10-02 | **Phase-wise development; the AI stops after every task** and reports what was built, how to test it, and what it needs from the team. Next task only after the team's OK | See [tasks/README.md](../tasks/README.md) |
| D-015 | 2026-10-02 | **Product spec v2 is [ChatGPT.md](../ChatGPT.md)** (committed by the team in `588f011`): customer Android app + salon Android app (multi-module `/android/app-customer`, `/app-salon`, `/core-*`, `/feature-booking`), admin web (React/TS: our Next.js fits), optional public booking web link, marketplace payments via Razorpay Route, disputes, double-entry ledger. **Customers log in with phone OTP** in their own app | Supersedes D-012's "customer may use WhatsApp link only". Converting it to tasks needs Q-008..Q-010 |
| D-016 | 2026-10-02 | **Login = Supabase Auth.** App users (customers, salon owner/staff) log in with **phone OTP by SMS only** (no WhatsApp OTP); SMS sent by **Twilio** configured inside Supabase. Admins log in by email (D-013) through the same Supabase project | Development uses Supabase **test phone numbers** (fixed codes, no SMS cost); Twilio India SMS ≈ ₹7 each later. See [research/AUTH_OPTIONS.md](research/AUTH_OPTIONS.md) |
| D-017 | 2026-10-02 | **API paths** (team delegated the choice): `/v1/c/...` customer app, `/v1/salon/...` salon app, `/v1/admin/...` admin panel, `/webhooks/razorpay` (unversioned: the URL is registered with Razorpay) | Version prefix lets old app versions keep working after a breaking change (DF-07) |
| D-018 | 2026-10-02 | **App language: English only** | All text stays in resource files, so adding languages later is cheap |
| D-019 | 2026-10-02 | **No internet in the salon app = view only**: the last loaded appointments and customers stay visible (Room cache); nothing can be added or changed until the connection is back | Simpler than drafts + sync; WorkManager still used for retries/background refresh |
| D-020 | 2026-10-02 | **Deadline isn't the driver**: finish Phase 0 in order, then Phase 1+ (Q-010). Code lives at `git@github.com:NIRMAL20062/Saloon_F.git` (private, MIT license) | Team: "we can complete before it" |
| D-021 | 2026-10-02 | **Every task arrives as a pull request.** Claude works on `task/<ID>-short-name` branches and shares the PR link; the team reviews and merges after `ci-ok` is green. `main` is protected | Small commits stay visible inside each PR (DF-15) |
| D-022 | 2026-10-02 | **Tasks are product features first** (login, screens, flows from ChatGPT.md + salon plan). Platform work (security scanning, CD) sits in a backlog and never blocks features | Team feedback 2026-10-02 |

## Implementation defaults (veto-able)

| ID | Default | Why |
|---|---|---|
| DF-01 | **One root Gradle build** for `shared`, `backend`, `android` | One Kotlin version and one version catalog; a contract change breaks both sides in the same build |
| DF-02 | Versions: Kotlin 2.4.20, AGP 9.4.1, Gradle 9.8.0, Ktor 3.6.0, Exposed 1.5.0, Flyway 13.9.0, Compose BOM 2026.09.00, Hilt 2.60.1 | Latest stable on 2026-10-01; AGP matches the team's Android Studio (2026.1.4). Pinned in `gradle/libs.versions.toml` |
| DF-03 | Bytecode: backend JVM 21, `shared` + Android JVM 17; CI builds on JDK 21 | Android tooling is most reliable on 17; backend runs on the JRE 21 LTS image |
| DF-04 | Android: compileSdk/targetSdk **37** (Android 17, latest stable), minSdk **24** | Plan says "Min SDK 24, target latest stable" |
| DF-05 | Formatting: **ktlint via Spotless** (Kotlin), ESLint (admin). detekt skipped for now | Stable detekt (1.23.x) doesn't support Kotlin 2.4 yet; revisit when detekt 2.0 is stable |
| DF-06 | Tests: backend JUnit 5 + **Testcontainers (real Postgres, no mocks for DB)**; Android JUnit 4 + Robolectric + MockWebServer; admin Vitest + Testing Library + **Playwright** E2E | Real DB in tests catches migration/constraint bugs early |
| DF-07 | Health: `GET /health/live` (process up) and `GET /health` (includes DB). Business APIs live under `/v1/...` | Hosting platforms need a liveness URL; versioned API lets old app versions keep working |
| DF-08 | Error envelope for every non-2xx: `{"error":{"code","message","requestId"}}`. Never leak stack traces | One shape for Android, admin and logs |
| DF-09 | Flyway migrations run on backend startup | Fine for a single instance; revisit before running multiple instances |
| DF-10 | Android debug builds call `http://127.0.0.1:8080` and reach the laptop through `adb reverse tcp:8080 tcp:8080` (works for a USB phone **and** an emulator); cleartext HTTP allowed **only** to localhost in debug. Release builds are HTTPS-only. _Changed 2026-10-01 from `10.0.2.2` (emulator-only) because the team tests on real phones_ | Same setup for every teammate; nothing reachable over Wi-Fi |
| DF-11 | Money is an integer number of **paise** everywhere (`bigint` in SQL, `Long` in Kotlin, `number` in TS), never floating point | Razorpay amounts are in paise; floats lose money in rounding |
| DF-12 | Tables: plural `snake_case`, `uuid` primary keys (`gen_random_uuid()`), `created_at`/`updated_at` `timestamptz` on every table. API timestamps are ISO-8601 UTC strings | IDs can be created anywhere and can't be guessed by counting |
| DF-13 | Tenant (`salon_id`) always comes from the authenticated user, never from request input; uniqueness is per salon (`UNIQUE (salon_id, …)`) | Enforces the multi-tenant rule in code **and** in the database |
| DF-14 | Shared AI workflow in `.claude/skills/`: `/work-task`, `/add-task`, `/add-endpoint`, `/db-migration`, `/feature-security-check` | Every teammate and AI tool follows the same steps and Definition of Done |
| DF-15 | PRs merge with a **merge commit** (not squash); `main` requires 1 approval + the `ci-ok` check | Keeps the small commits visible for debugging (D-007) |
| DF-16 | Admin email login = **email one-time code + mandatory authenticator-app (TOTP) MFA**, both free in Supabase; only rows in our `admins` table are admins; admins invite admins through the backend | Recommended in the auth research; no passwords to leak. Veto if you prefer magic links |
| DF-17 | Salon permissions = the table in the salon plan §1 with the open cells resolved conservatively: receptionist/manager **view** services and prices; only the **owner** manages staff, prices, reports and the subscription; a **stylist** sees only their own appointments and those customers; one person **may** belong to several salons | Least privilege first; widening a permission later is easy, taking one back isn't |
| DF-18 | App IDs and names: salon app `com.glide.salon` "Glide Salon", customer app `com.glide.customer` "Glide" (replaces `com.glide.android`, D-005). Kotlin packages `com.glide.salon`, `com.glide.customer`, `com.glide.core.*` | Placeholders like D-005; renamable before the first store upload |

## Open questions

| ID | Question | Blocks |
|---|---|---|
| Q-001 | ~~Product plan not in the repo~~ **Partly answered 2026-10-02 → D-015** (ChatGPT.md v2). Still missing: the salon-side plan it builds on (Q-009) | Phase 1+ planning |
| Q-002 | ~~Supabase only as hosted Postgres?~~ Partly answered 2026-10-02: free tiers (D-010), no hosting during development (D-011). Which free Postgres host at Pre-launch is decided together with Q-007 | Pre-launch |
| Q-003 | ~~Which user types need test users?~~ **Answered 2026-10-02 → D-012** | - |
| Q-004 | ~~Hosting provider?~~ **Answered 2026-10-02 → D-011**: none until development is done; free tier then. Provider picked at Pre-launch | Pre-launch |
| Q-005 | ~~How does our team log in to the admin panel?~~ **Answered 2026-10-02 → D-013** (email, admins add admins) | - |
| Q-006 | Final product name. **Not final** (team, 2026-10-02). "Glide" is also a well-known Android image library (bumptech/glide); check Play Store / trademark before launch | Play Store listing |
| Q-007 | ~~Auth provider~~ **Answered 2026-10-02 → D-016.** Was: **Auth provider.** The plan says Firebase phone OTP → backend JWT; the team now prefers **Supabase Auth everywhere** (app users and admins) and asked for a researched recommendation. **Research done: [research/AUTH_OPTIONS.md](research/AUTH_OPTIONS.md)**, with 4 questions for the team at the end | Phase 1 auth tasks |
| Q-008 | ~~API paths~~ **Answered 2026-10-02 → D-017.** Was: **API paths.** Spec v2 uses `/c/...` (customer), `/salon/...`, `/admin/...`, `/webhooks/razorpay`; Phase 0 used a `/v1` prefix (DF-07). Use `/v1/c/...`, `/v1/salon/...` (versioned, so old app versions keep working), or the spec's paths without a version? | First Phase 1 endpoint |
| Q-009 | **Team asked Claude to write it (2026-10-02)** as a draft for review → [../Salon_App_Task_Wise_Development.md](../Salon_App_Task_Wise_Development.md). Was: Spec v2 says it builds on **`Salon_App_Task_Wise_Development.md`** (salon onboarding, services, staff, customers, appointment engine, billing, subscriptions, admin; epics E2–E12). That file isn't in the repo | Salon-side tasks |
| Q-010 | ~~Deadline~~ **Answered 2026-10-02 → D-020.** Was: **Deadline.** Spec v2's sprint plan ends with **MVP submission on 2026-10-31**. Keep finishing Phase 0 in full, or do only CI next and jump to the salon core + customer booking? | Task order |
