# Glide: instructions for AI coding tools (and humans)

Multi-tenant salon management product. Android app for salons, Kotlin/Ktor backend, and an internal admin panel for our team.
Full stack: [docs/TECH_STACK.md](docs/TECH_STACK.md) · Every decision so far: [docs/DECISIONS.md](docs/DECISIONS.md)

## Golden rules

1. **Work only from `tasks/`.** Before writing code, find the task in `tasks/APP_TASKS.md`, `tasks/WEB_TASKS.md` or
   `tasks/BACKEND_TASKS.md`, check that its dependencies are ✅, and mark it 🔄. No task = don't build it; ask the team to add one
   (or use `/add-task`). Process: [tasks/README.md](tasks/README.md).
2. **Missing information → ask, never guess.** Don't invent features, user roles, screens, fields, prices, copy, providers or
   hosting. If a small technical gap must be filled to proceed, record it in `docs/DECISIONS.md` under *Implementation defaults*
   so the team can veto it.
3. **Small commits.** One unit of work per commit; each commit builds and passes tests on its own.
   Message: `type(scope): summary (TASK-ID)`, e.g. `feat(backend): add salons table (BE-101)`.
   Types: `feat`, `fix`, `test`, `docs`, `build`, `ci`, `refactor`, `chore`. Scopes: `android`, `backend`, `shared`, `admin`, `ci`, `docs`.
4. **Finish the paperwork.** When a task is done, move its block to the matching `_COMPLETED.md` file with date, commit hashes,
   tests added, and security/database notes.
5. **Stop and report after every task.** Never start the next task in the same session turn. Report to the team:
   what was built, **exact steps to test it themselves** (on the phone or in the browser), and **what you need from them**
   (API keys, accounts, answers). Then wait for their explicit go-ahead.

## Product rules: non-negotiable

- **The app never calls WhatsApp directly.** Always via the backend.
- **Razorpay orders are created and verified on the backend.** Trust the webhook (signature verified), never only the app's success callback.
- **Multi-tenant from day 1.** Every business table has `salon_id`; every query is scoped by it; every feature has a test proving
  salon A can't read or change salon B's data.
- **Secrets only in environment variables.** Never in the app, in git, or in logs.

## Repo map

| Path | What | Read first |
|---|---|---|
| `shared/` | API contract: DTOs used by backend **and** Android | [shared/CLAUDE.md](shared/CLAUDE.md) |
| `backend/` | Ktor API, Exposed, Flyway migrations, PostgreSQL | [backend/CLAUDE.md](backend/CLAUDE.md) |
| `android/` | Android app (Compose, Hilt, Retrofit) | [android/CLAUDE.md](android/CLAUDE.md) |
| `admin/` | Internal admin panel (Next.js) | `admin/CLAUDE.md` |
| `docs/api/openapi.yaml` | API contract for the admin panel (TypeScript types are generated from it) | |
| `tasks/` | What to build and what's done | [tasks/README.md](tasks/README.md) |

## Commands

```bash
docker compose up -d postgres          # local DB (older Docker: docker-compose)
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
- Every endpoint: happy path, validation failure, not-found, and (once auth exists) unauthenticated, wrong role, **other salon**.
- Android: ViewModel unit tests for every UI state; Compose UI tests (Robolectric) for every screen state.
- Never delete, skip or weaken a failing test to get green. Fix the code or ask.

## Never

- Edit a Flyway migration that's already merged. Add a new one instead.
- Commit or read `.env`, `google-services.json`, keystores or service-account files.
- Log phone numbers, OTPs, tokens, payment data or full request bodies.
- Return stack traces or exception messages to clients (use the error envelope).
- Add a dependency inline. Add it to `gradle/libs.versions.toml` (or `admin/package.json`) and mention it in the PR.
- Create or edit `android/app/build/` or generated files by hand.
