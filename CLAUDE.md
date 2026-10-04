# Glide: instructions for AI coding tools (and humans)

Multi-tenant salon marketplace for India. **One Android app** (customer side + salon side, chosen at onboarding), a Kotlin/Ktor
backend, and an internal admin website for our team.
Full stack: [docs/TECH_STACK.md](docs/TECH_STACK.md) · Every decision so far: [docs/DECISIONS.md](docs/DECISIONS.md)

## Where we are (read this first; update it in every task's pull request)

_Last updated 2026-10-04._

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
- **Now:** nothing in progress; waiting for the team's go-ahead to start BE-020, and their answer on Google sign-in for admins
  (today's decision is email code + authenticator app, D-013, DF-16).
- **Next task:** BE-020 → WEB-005 (admin website login). Full order:
  [tasks/README.md § Build order](tasks/README.md#build-order).
- **Supabase (dev project):** `https://uwvaebgbdqitbnymoqfq.supabase.co`, phone login via Twilio, test numbers `919000000001…004`
  (no SMS sent; their codes are in the team's password manager, not in git). Public keys only in the repo; secret keys never. The team rotates the secret key before
  production.
- **Blocked on the team:** BE-020 needs the reset Supabase secret key in `.env` as `SUPABASE_SECRET_KEY`.
- **Waiting on the team (not blocking yet):** WhatsApp Business API approval; launch city (APP-009).
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
