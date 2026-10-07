# Glide: instructions for AI coding tools (and humans)

Multi-tenant salon marketplace for India. **One Android app** (customer side + salon side, chosen at onboarding), a Kotlin/Ktor
backend, and an internal admin website for our team.
Full stack: [docs/TECH_STACK.md](docs/TECH_STACK.md) · Every decision so far: [docs/DECISIONS.md](docs/DECISIONS.md)

## Where we are (read this first; update it in every task's pull request)

_Last updated 2026-10-07._

- **What we build:** [docs/PRODUCT.md](docs/PRODUCT.md), the whole MVP as decided (D-038). The old planning files are gone; where
  anything disagrees, [docs/DECISIONS.md](docs/DECISIONS.md) wins.
- **Key rules:** one app, the customer/salon choice is final (D-023, D-030); one salon per person (D-035); our admins verify a
  salon before it goes live (D-033); two roles: the Owner has full control, Staff only see and handle their own appointments (D-039); online payments only, all through our Razorpay (D-028, D-029);
  modern, interactive screens from one design system, looking like the team's mockup, light mode only (D-031, D-040, D-041).
- **Done (Phase 0):** backend skeleton, database, Android app skeleton, admin website skeleton, CI (`ci-ok`). Details:
  `tasks/*_COMPLETED.md`.
- **Phase 1 (login and onboarding), merged 2026-10-03:** **BE-016** (Supabase logins, `/v1/me`, side final),
  **APP-004** (phone login), **APP-011** (design system), **APP-013** (the team's mockup look, welcome screen, light only).
  Details: `tasks/*_COMPLETED.md`.
- **BE-018 + APP-005** merged 2026-10-04 (profile API; "How will you use Glide?" and the customer profile, mockups 4–5).
- **BE-023** merged 2026-10-04: our development database is Supabase's Postgres, schema `glide` (D-042, DF-26).
- **WEB-002** merged 2026-10-04: the admin website calls the backend through a typed, server-only client (DF-27).
- **BE-020** merged 2026-10-04: admins list, admin-only routes (admin + authenticator app), invites, append-only audit log
  (DF-28, DF-29). First admin in the dev project: `admin@glide.test` (its codes come from the admin API, no inbox).
- **WEB-005** merged 2026-10-07: admin website login (email code + authenticator app), every page locked, 30 min idle /
  12 h sessions (DF-30). Checked in a real browser against real Supabase + backend.
- **BE-024** merged 2026-10-07: database connections keep the `glide` schema after a rollback (no more backend restarts).
- **WEB-008** merged 2026-10-07: admin login follow-up from the WEB-005 re-check (prefetches no longer skip the session
  check, pages apply the 30 min / 12 h limits themselves, no redirect loop, `/api-keys`-like pages covered).
- **Project review 2026-10-07** turned into tasks: BE-025–BE-028, APP-014–APP-017, WEB-009, WEB-010, a line added to
  BE-017, WEB-004 + BE-012 moved up, open questions Q-017–Q-020. Razorpay and the Play Console wait until later (team).
  Follow-up (2026-10-07): BE-029 (remove an admin, D-043; before WEB-006), BE-030, WEB-011, APP-014 extended. No PR
  reviewer for now: the team merges after `ci-ok` (D-044).
- **BE-034** merged 2026-10-07: CI refuses a pull request that changes, deletes or renames a merged migration
  (`MigrationTest` never could, though the docs said so). **`main` ruleset active** (team, 2026-10-07): changes only by pull
  request with `ci-ok` green; no force-push, no deleting `main`.
- **BE-025** merged 2026-10-07: services own the database transaction (one transaction per piece of work; repositories
  never open their own).
- **Now:** nothing in progress.
- **Next task:** BE-017 + APP-006 (create a salon), with the review fixes in
  between when the team picks them (list under the build order). Full order: [tasks/README.md § Build order](tasks/README.md#build-order).
- **Supabase (dev project):** `https://uwvaebgbdqitbnymoqfq.supabase.co`, phone login via Twilio, test numbers `919000000001…004`
  (no SMS sent; their codes are in the team's password manager, not in git). Admin login emails go through Brevo (custom
  SMTP, set up by the team 2026-10-07); the Magic Link email shows only the code, no link (template in WEB-005; changed by the team 2026-10-07). Public keys only in the repo;
  secret keys never. The team rotates the secret key before
  production.
- **Blocked on the team:** nothing.
- **Waiting on the team (not blocking yet):** WhatsApp Business API approval; launch city (APP-009); Q-019, Q-020.
  Google sign-in is off in Supabase (team, 2026-10-07).
  **Team will do later (2026-10-07):** India SMS rules (DLT: business, sender ID, message template). **SMS cost protection:**
  Supabase SMS rate limits (or CAPTCHA), Twilio Geo permissions India only, a Twilio spending alert. Later (team,
  2026-10-07): Razorpay Route application + KYC, Play Console account.
- **Open pull requests:** none.

## Golden rules

1. **Work only from `tasks/`.** Before writing code, find the task in `tasks/APP_TASKS.md`, `tasks/WEB_TASKS.md` or
   `tasks/BACKEND_TASKS.md`, check that its dependencies are ✅, and mark it 🔄. No task = don't build it; ask the team to add one
   (or use `/add-task`). Process: [tasks/README.md](tasks/README.md).
2. **Missing information → ask, never guess.** Don't invent features, user roles, screens, fields, prices, copy, providers or
   hosting. If a small technical gap must be filled to proceed, record it in `docs/DECISIONS.md` under *Implementation defaults*
   so the team can veto it.
3. **One task = one pull request, made of small commits.** Work on `task/<ID>-short-name`, push, share the PR link; the team
   merges after `ci-ok` is green. Never push to `main`. Each commit is one unit of work that builds and passes tests on its own.
   Message: `type(scope): summary (TASK-ID)`, e.g. `feat(backend): add salons table (BE-101)`.
   Types: `feat`, `fix`, `test`, `docs`, `build`, `ci`, `refactor`, `chore`. Scopes: `android`, `backend`, `shared`, `admin`, `ci`, `docs`, `tasks`.
4. **Finish the paperwork.** After the team's OK, as the last commit of the task's PR (before it's merged, DF-21), move its block
   to the matching `_COMPLETED.md` file with date, commit hashes, tests added, and security/database notes, **and update
   "Where we are" above** (done, next task, blockers, open PRs).
5. **Test on the phone, then stop and report after every task.** Install the build on the team phone and run the flow yourself
   (tap only inside our app). Never start the next task in the same session turn. Report to the team:
   what was built, **exact steps to test it themselves** (on the phone or in the browser), and **what you need from them**
   (API keys, accounts, answers). Then wait for their explicit go-ahead.

## Product rules: non-negotiable

- **The app never calls WhatsApp directly.** Always via the backend.
- **Razorpay orders are created and verified on the backend.** Trust the webhook (signature verified), never only the app's success callback.
- **Multi-tenant from day 1.** Every salon-owned table has `salon_id`; every query on it is scoped by it, after the backend checks
  the user's membership (D-025, D-036); every feature has a test proving salon A can't read or change salon B's data, and
  customer A can't see customer B's.
- **Secrets only in environment variables.** Never in the app, in git, or in logs.

## Repo map

| Path | What | Read first |
|---|---|---|
| `shared/` | API contract: DTOs used by backend **and** Android | [shared/CLAUDE.md](shared/CLAUDE.md) |
| `backend/` | Ktor API, Exposed, Flyway migrations, PostgreSQL | [backend/CLAUDE.md](backend/CLAUDE.md) |
| `android/` | The one Android app, customer side + salon side (Compose, Hilt, Retrofit) | [android/CLAUDE.md](android/CLAUDE.md) |
| `admin/` | Internal admin panel (Next.js) | `admin/CLAUDE.md` |
| `docs/api/openapi.yaml` | API contract for the admin panel (TypeScript types are generated from it) | |
| `tasks/` | What to build and what's done | [tasks/README.md](tasks/README.md) |

## Commands

```bash
docker compose up -d postgres          # optional local DB; development uses Supabase's Postgres (D-042, docs/DATABASE.md)
./gradlew :backend:run                 # API on :8080, reads repo-root .env
./gradlew check                        # everything Kotlin: ktlint + all tests (needs Docker for Testcontainers)
./gradlew spotlessApply                # auto-format Kotlin
./gradlew :backend:test --tests '*Health*'   # one test class
./gradlew :android:app:assembleDebug   # Android debug build
cd admin && pnpm verify                # admin: lint + typecheck + tests + build
```

Run `./gradlew check` (and `pnpm verify` if you touched `admin/`) before declaring a task done.

## Building a feature: vertical slice order

One feature usually spans several tasks (`BE-` → `APP-` / `WEB-`). Build in this order so each step compiles and is testable:

1. **Contract**: DTOs in `shared/` (+ `docs/api/openapi.yaml`)
2. **Database**: new Flyway migration (`/db-migration` skill)
3. **Backend**: repository → service → route, with integration tests on real Postgres (`/add-endpoint` skill)
4. **Android**: repository → use case → ViewModel → screen, with tests
5. **Admin**: regenerate API types → page, with tests
6. **Flow**: end-to-end check on the real stack; `/feature-security-check` before opening the PR

## Testing rules

- Anything touching the database is tested against **real PostgreSQL** (Testcontainers). Never mock the database.
- Every endpoint: happy path, validation failure, not-found, and (once auth exists) unauthenticated, wrong role, **other salon**,
  **other customer**.
- Android: ViewModel unit tests for every UI state; Compose UI tests (Robolectric) for every screen state.
- Never delete, skip or weaken a failing test to get green. Fix the code or ask.

## Never

- Edit a Flyway migration that's already merged. Add a new one instead.
- Commit or read `.env`, `google-services.json`, keystores or service-account files.
- Log phone numbers, OTPs, tokens, payment data or full request bodies.
- Return stack traces or exception messages to clients (use the error envelope).
- Add a dependency inline. Add it to `gradle/libs.versions.toml` (or `admin/package.json`) and mention it in the PR.
- Create or edit `android/app/build/` or generated files by hand.
