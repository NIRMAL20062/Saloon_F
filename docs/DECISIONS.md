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

## Open questions

| ID | Question | Blocks |
|---|---|---|
| Q-001 | Product plan sections 1–3 (problem, features, MVP scope, user types) aren't in the repo yet. `ChatGPT.md` is empty on disk | Phase 1+ planning |
| Q-002 | "We will do all things like Supabase": use Supabase only as a **hosted Postgres** for staging/prod (works with Ktor + Exposed), or something else? | Staging setup |
| Q-003 | Which user types need test users? (e.g. salon owner, staff/stylist, receptionist, our internal admin, a salon's customer) | Phase 1 seed data, roles |
| Q-004 | Hosting for backend + Postgres + admin (Railway / Render / VPS / other) | CD deploy step |
| Q-005 | How does our team log in to the admin panel? (e.g. Google Workspace SSO, Firebase email link) | Admin auth, Phase 1 |
| Q-006 | Final product name. "Glide" is also a well-known Android image library (bumptech/glide). Fine as a working name, but check Play Store / trademark before launch | Play Store listing |
