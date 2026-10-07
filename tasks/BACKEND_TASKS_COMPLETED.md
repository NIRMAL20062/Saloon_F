# ⚙️ Backend + Platform: Completed

Newest at the bottom. Each entry keeps the commits so anyone can `git show <hash>` to see exactly what changed.

## Phase 0: Walking skeleton

### BE-001 · Repo foundation
- **Completed:** 2026-10-01 · **Commits:** `74804a7`
- **Built:** `.gitignore` (secrets, keystores, `google-services.json`, `.env` never committed), `.editorconfig`, `.gitattributes`, `.env.example`, `README.md`
- **Security:** secret file patterns ignored from the very first commit

### BE-002 · Record tech stack and decisions
- **Completed:** 2026-10-01 · **Commits:** `839fe04`
- **Built:** [docs/TECH_STACK.md](../docs/TECH_STACK.md) (product plan stack, verbatim), [docs/DECISIONS.md](../docs/DECISIONS.md) (team decisions, veto-able defaults, open questions)

### BE-003 · Gradle build
- **Completed:** 2026-10-01 · **Commits:** `cfa4cea`
- **Built:** Gradle 9.8.0 wrapper, one root build, version catalog `gradle/libs.versions.toml` (Kotlin 2.4.20, AGP 9.4.1), ktlint via Spotless wired into `./gradlew check`
- **Security:** wrapper download pinned by SHA-256, so a tampered Gradle distribution fails the build

### BE-004 · Shared API contract module
- **Completed:** 2026-10-01 · **Commits:** `eee1ac2`
- **Built:** `shared/`: `ApiJson` (one JSON config for both ends; unknown keys ignored so old app versions survive new fields), `ApiRoutes`, `HealthResponse`, `ErrorResponse` + `ErrorCodes`
- **Tests:** `ContractSerializationTest` (4): pins the exact JSON wire format, forward compatibility

### BE-005 · Ktor server skeleton with fail-fast config
- **Completed:** 2026-10-01 · **Commits:** `f3b99db`
- **Built:** Ktor 3.6 on Netty, `AppConfig.fromEnv` (env vars only), `GET /health/live`
- **Tests:** `AppConfigTest` (7), `LivenessRouteTest` (1)
- **Security:** all config problems reported at once; https-only CORS origins in production; DB password never printed

### BE-006 · PostgreSQL: Hikari, Flyway, Exposed
- **Completed:** 2026-10-01 · **Commits:** `84c91a4`
- **Built:** `DatabaseFactory` (Hikari pool, Flyway migrate on startup, Exposed connect), `V1__baseline.sql` (`set_updated_at()` trigger function), `docker-compose.yml` (Postgres 17, bound to 127.0.0.1)
- **Tests:** `MigrationTest` (4) on a real Postgres via Testcontainers: migrations apply, re-run is a no-op, edited migrations are detected, trigger works
- **Database:** `flyway clean` disabled in every environment; no business tables yet (Phase 1)

### BE-007 · `/health` readiness endpoint
- **Completed:** 2026-10-01 · **Commits:** `8ff82cd`
- **Built:** `GET /health` → `200 UP` or `503 DOWN` based on a real DB round-trip (3 s timeout)
- **Tests:** `ReadinessRouteTest` (4): fake UP/DOWN, real Postgres UP, unreachable DB DOWN
- **Flow verified manually:** `docker compose up -d postgres` → `./gradlew :backend:run` → `curl /health` → `{"status":"UP","version":"0.1.0","database":"UP"}`
- **Security:** failure logs carry only the exception type (driver messages can contain hostnames/users)

### BE-008 · Security baseline + standard error envelope
- **Completed:** 2026-10-01 · **Commits:** `5e725ab`
- **Built:** request IDs (`X-Request-Id`), safe access log (path + status only), security headers, HSTS on staging/prod, CORS allowlist, per-IP rate limit (`RATE_LIMIT_PER_MINUTE`, default 300), 1 MB body limit, every error → `{"error":{"code","message","requestId"}}`
- **Tests:** `SecurityBaselineTest` (11), one per control, plus `AppConfigTest` +1
- **Security:** no stack traces, exception messages or class names ever reach clients

### BE-014 · AI tooling: CLAUDE.md files, settings, skills
- **Completed:** 2026-10-01 · **Commits:** `2c052a2`
- **Built:** root [CLAUDE.md](../CLAUDE.md) (work only from `tasks/`, ask instead of guessing, small commits with task IDs, product rules), [shared/CLAUDE.md](../shared/CLAUDE.md), [backend/CLAUDE.md](../backend/CLAUDE.md), [android/CLAUDE.md](../android/CLAUDE.md); skills `/work-task`, `/add-task`, `/add-endpoint`, `/db-migration`, `/feature-security-check` in `.claude/skills/`; `.claude/settings.json`
- **Security:** Claude Code is denied reading/editing `.env*` secrets files, keystores, `google-services.json`, service-account files
- **Deferred:** `admin/CLAUDE.md` is created with WEB-001 (the Next.js generator needs an empty folder)
- **Decisions logged:** DF-11 (money in paise), DF-12 (uuid keys, timestamps), DF-13 (tenant from auth only), DF-14 (skills)

### BE-009 · Backend Docker image
- **Completed:** 2026-10-01 · **Commits:** `f4a8e94`
- **Built:** multi-stage `backend/Dockerfile` (JDK 21 build → JRE 21 alpine, 245 MB), container health check on `/health/live`, `.dockerignore`, `docker-compose.yml` `full` profile (backend + Postgres)
- **Verified:** `docker compose --profile full up -d --build` → container `healthy`, Flyway validated on start, `curl /health` → `{"status":"UP","version":"0.1.0","database":"UP"}` with all security headers
- **Security:** runs as non-root (`uid=100(glide)`); `.env`, keystores, `google-services.json`, `android/`, `admin/`, `docs/` excluded from the build context; Flyway masks the JDBC URL in logs
- **Database:** migrations run on container start; on a **fresh** database verified in CI end-to-end run 36980498414 (2026-10-02)

### BE-010 · OpenAPI spec + contract test
- **Completed:** 2026-10-01 · **Commits:** `16596a2`
- **Built:** [docs/api/openapi.yaml](../docs/api/openapi.yaml) (OpenAPI 3.0.3): `/health/live`, `/health` (200/503), error envelope as `default` response, `X-Request-Id` header; `additionalProperties: false` so undocumented fields fail
- **Tests:** `OpenApiContractTest` (6): 200, 503 and 429 responses validated against the spec; the validator rejects a bad body (guards the guard); every registered backend route is in the spec and vice versa
- **Notes:** validator `swagger-request-validator-core` 2.46.1 (3.0.0 on Maven Central has no jar)

### BE-011 · CI pipeline (GitHub Actions)
- **Completed:** 2026-10-02 · **Commits:** `e8f3d5f`, `2292889` (fix)
- **Built:** `.github/workflows/ci.yml`: path-filtered jobs (backend + shared on real Postgres, Android lint/tests/debug+release builds with APK artifact, admin `pnpm verify`, end-to-end smoke with backend + fresh Postgres in Docker + admin), single required check `ci-ok`
- **Verified:** run 36980498414 all green (~10 min); a real failure (run 36977637474, config bug) turned `ci-ok` red as designed
- **Security:** least-privilege permissions, actions pinned to commit SHAs, Gradle wrapper validated, runner pinned to ubuntu-24.04
- **Team:** made the repo public (unlimited free CI minutes) and protected `main` (PR + `ci-ok`)

### BE-015 · Project docs
- **Completed:** 2026-10-02 · **Commits:** `663db69` (+ README/CLAUDE.md updates in later commits)
- **Built:** ARCHITECTURE, DEVELOPMENT_WORKFLOW, TESTING, SECURITY, DATABASE guides, PR template; README quick start covers backend, both apps and admin

## Phase 1: Login and onboarding

### BE-016 · Backend trusts Supabase logins + `GET /v1/me`
- **Completed:** 2026-10-03 (team's OK and merge) · **Commits:** `3c507d6` `1468aed` `5bb5301` `69fa583`
- **Phase:** 1 · **Status:** ✅ Done · **Owner:** Claude · **Depends on:** BE-011 · Decision: D-016
- **Why:** the apps log in with Supabase; the backend must check every request's token itself.
- **Needs from team:** Supabase project (free, region Mumbai) with Phone provider + Twilio + test phone numbers; project URL shared;
  secret keys put into `.env` by the team (never in chat or git). Steps given in the task report.
- **Scope:**
  - verify the Supabase access token on every protected route: signature (JWKS public keys), issuer, audience, expiry
  - `app_users` table (one row per Supabase user: id, phone, **side** CUSTOMER / SALON / not chosen yet, created_at), created on first request
  - `GET /v1/me` → who is signed in and their chosen side (the app picks the interface from this, D-024). The person's salon and role are added by BE-017, when salons exist (new fields; older app versions ignore them)
  - `PUT /v1/me/side` → save the onboarding choice (customer or salon). **Final once chosen** (D-030, team 2026-10-03): a different side → 409 `SIDE_ALREADY_CHOSEN`, the same side again → 200; a database trigger refuses any change too
- **Done when:**
  - [x] Tests: valid token → 200; missing / expired / wrong-signature / wrong-issuer token → 401 with the error envelope; side saved and returned (`SupabaseTokenVerifierTest` 9, `MeRoutesTest` 6)
  - [x] Database: `V2__app_users.sql`; first-request creation is idempotent: 10 parallel first requests → one row (`UserRepositoryTest`, real Postgres); side enforced by a CHECK constraint; `V3__app_users_side_final.sql`: a trigger refuses changing a chosen side (D-030). A new migration, not an edit of V2, so local databases that already ran V2 keep working
  - [x] Security: tokens and phone numbers never logged (checked in the real run's log); JWKS cached, 5 s download timeout, 5 s retry pause; only ES256/RS256 accepted (HS256 forgery refused); no Supabase secret in the repo or used by the backend
  - [x] OpenAPI spec updated (`/v1/me`, `/v1/me/side`, bearer security, 401 envelope); contract tests pass
  - [x] Flow (real Supabase project): test number OTP → real access token → `GET /v1/me` 200 → `PUT /v1/me/side` CUSTOMER saved and returned → tampered token 401. After D-030 (2026-10-03, on the local database that had run V2): V3 applied without a reset; same side again → 200, other side → 409 `SIDE_ALREADY_CHOSEN`, side unchanged
- **Tests:** backend 59 (`SupabaseTokenVerifierTest` 9, `MeRoutesTest`, `UserRepositoryTest` on real Postgres)
- **Security:** tokens and phone numbers never logged; only ES256/RS256 accepted; no Supabase secret used by the backend
- **Database:** `V2__app_users`, `V3__app_users_side_final` (side is final, D-030)
- **Verified by:** Claude, real Supabase project + local database (2026-10-02/03)

### BE-018 · Profile: name and email
- **Completed:** 2026-10-04 (team's go-ahead, merged) · **Commits:** `819c4c6` `8c4247b` `ef3dc0a` `153b3b4`
- **Phase:** 1 · **Status:** ✅ Done · **Owner:** Claude · **Depends on:** BE-016 · Spec: PRODUCT §5 · Decision: DF-18
- **Scope:** `name` and `email` columns on `app_users` (new migration; one profile per person, no `customers_app_users` table);
  `GET /v1/me` returns them; `PUT /v1/me/profile` (name required 2–60 chars, email optional + valid).
  `PUT /v1/me/side` refuses a change once a side is saved: the choice is final (D-030). The team asked for this fix in BE-016
  (PR #2); if it's already there, only check it here.
- **Done when:**
  - [x] Tests for validation, own profile only, new vs returning user (`MeRoutesTest` +9), database checks (`UserRepositoryTest` +2); a different side after one is saved → 409 (from BE-016); OpenAPI updated, contract test passes. Backend total 70
  - [x] Flow (real Supabase login, local database after V4 applied by itself): profile saved and read back, email lower-cased; one-letter name → 400 `INVALID_NAME`; bad email → 400 `INVALID_EMAIL`; no name, email, phone or token in the log
- **Security:** auth required; the id always comes from the token (own profile only); server + database validation; no names, emails, phones or tokens in logs
- **Database:** `V4__app_users_profile` (name 2–60, email ≤ 254 and email-shaped)
- **Verified by:** Claude, real Supabase login + local database

### BE-020 · Admins: first admin, invites, admin-only routes
- **Completed:** 2026-10-04 (team's OK) · **Commits:** `e0c69d2` `1bf3958` `fb7fd29` `20da4be` `15367bb` `e7a539f` `911f173` `ff21c04` · PR #11
- **Phase:** 1 · **Status:** ✅ Done · **Owner:** Claude · **Depends on:** BE-016 · Decisions: D-013, DF-16
- **Needs from team:** the email of the first admin; Email provider turned on in Supabase (+ free SMTP, e.g. Brevo) and authenticator-app MFA enabled;
  the Supabase **service-role key** put into `.env` by the team (the backend needs it to send invites; never in chat or git).
- **Scope:** `admins` table (our DB decides who is admin); first admin created by a one-off command; `GET /v1/admin/me`;
  `POST /v1/admin/admins/invites` (sends the Supabase invite email); every `/v1/admin` route requires an admin **with MFA completed**.
- **Done when:**
  - [x] tests: non-admin → 403; admin without MFA → 403; invite creates a pending admin; audit-logged
  - [x] Flow: against the real Supabase project, the first admin made by the command; email code only → 403 MFA_REQUIRED; with the authenticator app → 200 ACTIVE; invite → 201; app (phone) user → 403
- **Built:** `admins` + append-only `audit_log` (V5, DF-28); `adminOnly()` guard (login in `admins` by user id **and** `aal2`,
  else 403 `NOT_ADMIN` / `MFA_REQUIRED`; INVITED → ACTIVE on first such request); `GET /v1/admin/me`;
  `POST /v1/admin/admins/invites` (ASCII email, Supabase invite or existing login, 409 / 502 / 503); `addFirstAdmin` command;
  `SupabaseAuthAdmin` (JDK HttpClient); `SUPABASE_SECRET_KEY` (optional locally, required on staging/production); DF-29
- **Tests:** `AdminTablesTest` 7, `AdminRepositoryTest` 6, `AdminRoutesTest` 16, `AddFirstAdminTest` 3, `SupabaseAuthAdminTest` 9;
  added `SupabaseTokenVerifierTest` +3, `AppConfigTest` +5, `OpenApiContractTest` +1, `ContractSerializationTest` +2
- **Security:** admin matched on user id, never email; MFA required; every `/v1/admin` route tested as guarded; secret key never
  printed, publishable key refused in its place; Supabase error text never logged; audit rows can't be changed or deleted
- **Database:** `V5__admins_and_audit_log` (applied to the Supabase dev database 2026-10-04). Team asked afterwards for one
  table/change per migration file from now on (rule added to the db-migration skill); V5 stays as applied
- **Verified by:** Claude against the real Supabase dev project + local backend: first admin `admin@glide.test` by the command;
  email code only → 403 `MFA_REQUIRED`; real authenticator code → 200 ACTIVE (test factor removed afterwards); invite of
  `second.admin@glide.test` (existing login) → 201, again → 409; test phone user → 403 `NOT_ADMIN`; no login → 401

### BE-024 · Fix: database connections lose the `glide` schema after a rollback
- **Completed:** 2026-10-07 (team's go-ahead, merged) · **Commits:** `42d10bc` `edcc994` `032c445`
- **Status:** ✅ Done · **Owner:** Claude · **Depends on:** BE-023 · Decisions: D-042, DF-26
- **Why:** found while testing WEB-005: after Supabase's pooler closed idle connections, new connections answered
  `relation "admins" does not exist` (HTTP 500) until the backend restarted.
- **Cause:** the pool runs with auto-commit off, so HikariCP's `search_path` setting sat inside each connection's first
  transaction; a rollback of that transaction undid it for good.
- **Fix:** `DatabaseFactory` sets the schema with `connectionInitSql`, and `isolateInternalQueries = true` makes HikariCP
  commit it as the connection opens (checked in HikariCP 7.1.0's `PoolBase`: the init SQL is committed only with that flag).
  Still `glide`, never `public`. Side effect: unused pooled connections no longer sit `idle in transaction` on Supabase.
- **Done when:**
  - [x] Test: a fresh pooled connection whose first transaction is rolled back still finds `glide` tables
  - [x] Test: connections replaced by the pool (closed underneath, like the pooler does) still find `glide` tables
  - [x] Database: no migration
  - [x] Flow: on the Supabase database (see "Verified by")
- **Tests:** `ConnectionSchemaTest` 2 (real Postgres, each with its own pool; both failed before the fix with "expected glide
  but was public"). Backend total 124
- **Security:** no routes, queries, tables, env vars or logging changed; `/feature-security-check` PASS
- **Database:** no migration
- **Verified by:** Claude, backend from the branch on the Supabase dev database, a test-number customer and `admin@glide.test`
  (email code from the admin API + a test authenticator, removed afterwards; no SMS or email sent): `/v1/me` 200,
  `/v1/admin/me` 200, `/v1/admin/me` as a customer 403 (×3) at the start; again after all 10 backend connections were closed
  from the database side; again after 32 minutes idle (first try: one call timed out after 20 s, unclear whether Supabase's
  token refresh or our backend; no schema errors; rerun 2 minutes later all 9 passed). HikariCP's keepalive kept the idle
  connections open, so the forced close is the stronger check

### BE-034 · CI refuses a pull request that changes a merged migration
- **Completed:** 2026-10-07 · **Commits:** `b18c857` `e68e308` `9cc1be1` `c528bc0`
- **Status:** ✅ Done · **Owner:** Claude · **Depends on:** - · Decisions: D-042, DF-09
- **Why:** found by the team (2026-10-07): nothing stopped an edited merged migration before it reached `main`.
  `MigrationTest` builds a fresh database from the current files and compares it with those same files, so it always
  passed, yet the db-migration skill and `docs/DATABASE.md` said it "detects edits". Missed by the project review the same day.
- **Built:** CI job "Merged migrations unchanged" (`.github/scripts/merged-migrations-unchanged.sh`): on every pull request
  it compares the merge commit with the target branch and fails when a migration already there is changed, deleted or
  renamed; new files pass; `ci-ok` waits for it. Skill and `DATABASE.md` corrected, with the team's naming style
  (`V<n>__create_<table>`, `V<n>__fix_<what>`, a "why" comment on top). `MigrationTest`'s validation test renamed to what
  it really checks (same assertions).
- **Done when:**
  - [x] Tests: on a throwaway copy of the repo, with merge commits like GitHub's: edit, delete and rename of V3 fail; a
    new V7, a docs-only change, and a V7 added then edited in the same PR pass
  - [x] Security: read-only token (`contents: read`), `persist-credentials: false`, no secrets; checkout pinned by SHA
  - [x] Database: none
  - [x] Flow: the job runs on this task's PR and `ci-ok` needs it
- **Tests:** the CI job itself; `MigrationTest` 5 green after the rename
- **Security:** a pull request could change the script too, but that change shows in its diff like any CI change
- **Database:** no migration. V5 keeps its two tables (written before the one-change rule; it has run on the dev database)

### BE-025 · Services own the database transaction
- **Completed:** 2026-10-07 (team's go-ahead, merged) · **Commits:** `dd7d06c` `e3a52e4` `960428f` `4212c51` `8aa10e2` `2d10f0e`
- **Phase:** 1 · **Status:** ✅ Done · **Owner:** Claude · **Depends on:** BE-024 · Decisions: D-025, D-027
- **Why:** project review (2026-10-07): each repository method opens its own transaction, so "check, then write" (e.g. check a
  slot, then book it) can't be one step, and row-level security's per-transaction `set_config('app.salon_id', …, true)` has
  nowhere to go. Needed before BE-017.
- **Needs from team:** nothing.
- **Scope:**
  - one transaction helper that services call; repositories run inside the caller's transaction and never open their own
  - existing services moved to it (users/profile, admins/invites + audit log)
  - Not included: row-level security itself (BE-017); any change to what the API does
- **Done when:**
  - [x] Tests: a service doing two repository writes keeps neither when the second fails; all existing tests green
  - [x] Security: no behaviour change; the audit row is still written in the same transaction as the change (DF-28)
  - [x] Database: none
  - [x] Flow: on the Supabase database, app login → profile saved, and an admin invite, still work
- **Built:** `db/Transactor` (one `transaction { }` per piece of work; the block can't suspend, so no Supabase call runs
  inside); `UserRepository` and `AdminRepository` run inside the caller's transaction; new `UserService`; `AdminService`
  owns its transactions, with the Supabase calls of an invite between them; the admin check in front of `/v1/admin` goes
  through the service. Only `Transactor` opens transactions. No API change
- **Tests:** `TransactorTest` 3, `UserServiceTest` 2, `AdminRepositoryTest` +1 (invite and audit row kept or dropped
  together). Backend total 130
- **Security:** no new queries, routes, env vars or logging; audit row in the same transaction as the change (tested);
  `/feature-security-check` PASS
- **Database:** none
- **Verified by:** Claude, backend from the branch on the Supabase dev database: test number …003 profile saved with the same
  values, same side again 200, a switch 409, one-letter name 400, profile unchanged; `admin@glide.test` (email code from the
  admin API + test authenticator) `/v1/admin/me` 200, invite of an existing login 201 (no email), again 409, a customer 403;
  test admin, its login and the test authenticator removed afterwards. Not run on the phone (no app change)


### BE-031 · Limited database role and salon context (row-level security groundwork)
- **Completed:** 2026-10-07 (team's go-ahead, merged) · **Commits:** `84ee2a3` `208ca4d` `66f11ca` `6b09d1f`
- **Phase:** 1 · **Status:** ✅ Done · **Owner:** Claude · **Depends on:** BE-025 · Decisions: D-027, DF-26
- **Why:** first part of BE-017, split off so each PR stays small (review 2026-10-07). Row-level security (D-027) needs the
  backend's queries to run as a role that is neither superuser, owner nor `BYPASSRLS`; on Supabase our `postgres` login has
  `BYPASSRLS`, so policies would do nothing for it. The project review also asked that tests run as that role.
- **Needs from team:** nothing (no new secret: see Scope).
- **Scope:**
  - migration: role `glide_app` (no login, no `BYPASSRLS`), allowed to read/write our tables in `glide`; `audit_log` only
    read and insert; the same for tables added later
  - every pooled connection switches to it (`SET ROLE glide_app`, committed when the connection opens, like BE-024);
    Flyway keeps migrating as the owner login
  - `Transactor` can run a transaction for one salon: sets `app.salon_id` for that transaction only (`set_config(…, true)`)
  - tests run as `glide_app` too (the shared test pool is the backend's pool)
  - Not included: any salon table (BE-017); policies for a customer's own data (later, with the first such table)
- **Done when:**
  - [x] Tests: pooled connections are `glide_app`, not superuser, no `BYPASSRLS`; a test table with forced row-level security:
    salon A's transaction sees only A's rows, can't write B's, and with no salon set sees none; the salon setting doesn't
    outlive its transaction; `glide_app` can't change `audit_log` or create tables; all existing tests green as `glide_app`
  - [x] Security: no new secret; the owner login is used only by Flyway
  - [x] Database: one migration (the role and its grants); applied to the Supabase dev database by the backend at startup
  - [x] Flow: backend on the Supabase database: `/v1/me`, profile save and `/v1/admin/me` still work as `glide_app`
- **Built:** migration V6: role `glide_app` (no login, no password, not superuser, owner of nothing, no `BYPASSRLS`), with
  read/write on our tables (audit log read + insert; migration history read), also for tables added later; every pooled
  connection does `SET ROLE glide_app` (committed as it opens); Flyway migrates as the owner login before the pool starts;
  `transactor.transaction(salon)` sets `app.salon_id` for that transaction only. BE-017 was split into BE-031, BE-017,
  BE-032, BE-033 (same scope). DF-31 records the role switch instead of a second login
- **Tests:** `AppRoleTest` 6 (role attributes; a probe table with forced RLS: salon A sees only A, no salon sees none, A
  can't write B; the setting ends with its transaction; no changes to the audit log or migration history, no new tables).
  Every existing test now runs as `glide_app`. Backend total 136
- **Security:** no new secret; on Supabase the owner login has `BYPASSRLS`, so the role switch is what makes policies apply;
  `/feature-security-check` PASS
- **Database:** V6, applied to the Supabase dev database 2026-10-07 (`success=true`); the pooler doesn't carry the role
  switch over to other clients (checked)
- **Verified by:** Claude on the Supabase dev database: `/v1/me`, profile save, side, `/v1/admin/me`, an admin invite
  (audit row written by `glide_app`) all OK; on the team phone (moto g54) with the backend of BE-017's branch, which
  contains this one: login, reopen, logout, onboarding (customer and salon), backend down → Retry


### BE-017 · Salons and owners
- **Completed:** 2026-10-07 (team's go-ahead, merged) · **Commits:** `044b4f3` `949453c` `e074677` `0449baf` `a56ea02` `b558109` `1d1e2da`
- **Phase:** 1 · **Status:** ✅ Done · **Owner:** Claude · **Depends on:** BE-016, BE-031 · Spec: PRODUCT §3, §6.1 · Decisions: D-025–D-027, D-033, D-035, D-036, D-039, D-046, DF-32
- **Needs from team:** nothing (fields answered 2026-10-07: D-046).
- **Scope:**
  - `salons` (name, phone, address, type men/women/unisex, **status** DRAFT → UNDER_VERIFICATION → LIVE, or REJECTED with a
    reason, or SUSPENDED) and `salon_members` (salon, phone, user, role OWNER / STAFF (D-039), status ACTIVE / REMOVED), one
    migration each, row-level security forced on both
  - `POST /v1/salon/salons` (creator becomes OWNER; only a person whose side is SALON, D-030) · `GET /v1/salon/me` (my salon
    and role) · `PUT /v1/salon/salon` (owner edits the profile while not live)
  - one salon per person (D-035); every salon route takes the salon from the signed-in person's membership and checks the role
    (D-036); everything except a staff member's own appointments is OWNER-only (D-039)
- **Done when:**
  - [x] Tests: create → DRAFT with the creator as OWNER; a second salon for the same person refused; a customer refused;
    **salon A can't read or change salon B** (every route); validation errors
  - [x] Database: migrations with constraints (exactly one OWNER per salon, one active membership per phone, valid statuses);
    RLS enabled + forced; test: `glide_app` with salon A set sees zero rows of salon B, with no salon set sees none
  - [x] Security: salon id never trusted from the request; role checked on every route
  - [x] Flow: on the Supabase database, test number A creates "Test Salon A" and reads it back; test number B can't see it
- **Built:** V7 `salons` (profile per D-046/DF-32, status, forced RLS: a row only in its own salon's transaction); V8
  `salon_members` (one active membership per login and per phone across salons, one owner per salon, an owner has a login;
  forced RLS: the salon's transaction or the member themself); `RowSecurity` (salon and person per transaction);
  `POST /v1/salon/salons`, `GET /v1/salon/me`, `PUT /v1/salon/salon` (no salon id in any route, D-036); `Phones.indian`;
  contract + OpenAPI + admin types. The generic 404 now replaces only a bare 404, so `NO_SALON` keeps its code
- **Tests:** `SalonTablesTest` 6, `SalonMembersTablesTest` 6, `SalonRoutesTest` 11 (incl. a 4-request race, staff can't
  edit, two salons isolated), `PhonesTest` 2, `ContractSerializationTest` +2, `OpenApiContractTest` +1. Backend total 162
- **Security:** salon only from membership; owner-only edits; RLS forced on both tables; validation mirrors the database;
  SECURITY.md updated; `/feature-security-check` PASS. Tracked: an unexpected database error's log can hold row values (BE-026)
- **Database:** V7, V8 (applied to the Supabase dev database 2026-10-07)
- **Verified by:** Claude on the Supabase dev database with two throwaway logins (created, read, refused a second salon,
  B couldn't see A, landline phone, edit; all deleted afterwards); on the team phone (moto g54): login, reopen, logout,
  onboarding customer + salon side, backend down → Retry. The salon screens come with APP-006


### BE-032 · Bank details (encrypted) and "submit for verification"
- **Completed:** 2026-10-07 (team's go-ahead, merged) · **Commits:** `dd25e14` `bad1dbf` `76dfcd7` `218e73c` `790eea8`
- **Phase:** 1 · **Status:** ✅ Done · **Owner:** Claude · **Depends on:** BE-017 · Spec: PRODUCT §6.1 · Decisions: D-033, DF-24, DF-33
- **Needs from team:** `BANK_DETAILS_KEY` in `.env` (`openssl rand -base64 32`; DF-33). Until then the bank routes answer 503.
- **Scope:** `salon_bank_details` (account holder name, account number **encrypted**, IFSC; masked in every app response,
  DF-24); `PUT /v1/salon/bank-details` (owner only) · `POST /v1/salon/submit-for-verification` (needs a complete profile and
  bank details; DRAFT or REJECTED → UNDER_VERIFICATION)
- **Done when:**
  - [x] Tests: save → masked; submit without bank details refused; submit → UNDER_VERIFICATION; resubmit after REJECTED;
    STAFF → 403; salon A can't read or change salon B's bank details
  - [x] Database: migration with IFSC format check; RLS forced; the stored value is not the account number in clear
  - [x] Security: account number encrypted at rest with a key from env, masked in responses, never logged; audit-logged once
    BE-019 lands
  - [x] Flow: on the Supabase database, "Test Salon A" saves bank details and submits → UNDER_VERIFICATION
- **Built:** `BANK_DETAILS_KEY` (32 bytes base64; optional on a laptop → bank routes 503; required on staging/production;
  never printed); `FieldCipher` (AES-256-GCM, Java built-in, salon id bound in, `v1:` prefix); V9 `salon_bank_details`
  (encrypted only, last 4 apart, IFSC check, forced RLS); `PUT|GET /v1/salon/bank-details` (owner, masked),
  `POST /v1/salon/submit-for-verification` (DRAFT/REJECTED + bank details → UNDER_VERIFICATION, safe retry). DF-33
- **Tests:** `FieldCipherTest` 4, `AppConfigTest` +3, `SalonBankDetailsTablesTest` 4, `SalonBankRoutesTest` 10, contract
  tests (bank and submit answers match the spec). Backend total 183
- **Security:** account number encrypted at rest, bound to its salon, refused in clear by the database, last 4 only in
  responses and errors, never logged, owner only; SECURITY.md updated; `/feature-security-check` PASS. Audit rows: BE-019
- **Database:** V9 (applied to the Supabase dev database 2026-10-07)
- **Verified by:** Claude on the Supabase dev database (throwaway owners and a throwaway key, all deleted afterwards): submit
  without bank → 409, wrong IFSC → 400, saved and read masked, stored `v1:…` only, submit → UNDER_VERIFICATION, retry
  same, another salon sees nothing; on the team phone (moto g54): login, System status UP, reopen, logout. The bank screens
  come with APP-006


## Platform backlog

### BE-023 · Our database on Supabase's Postgres
- **Completed:** 2026-10-04 (team's go-ahead, merged) · **Commits:** `13a5169` `20561a7` `63a4af5` `4b7f90d`
- **Status:** ✅ Done · **Owner:** Claude · Decisions: D-042, DF-26
- **Scope:** development database moved from Docker on the laptop to Supabase's hosted Postgres (dev project); every table,
  function and trigger in schema `glide` (not `public`, which Supabase's Data API publishes); a Supabase `DATABASE_URL` without
  `sslmode=require` (or `verify-ca`/`verify-full`) stops the backend at startup
- **Done when:**
  - [x] Tests: `AppConfigTest` (a Supabase database needs an encrypted connection), `MigrationTest` (our tables live in `glide`,
    never in `public`). Backend total 72
  - [x] Backend started against Supabase (Session pooler, port 5432): Flyway created `glide` and applied V1–V4
  - [x] Flow on the team phone (fresh app data, test number 3): login → customer → profile → home; still there after reopening;
    `/v1/me`, `/v1/me/side`, `/v1/me/profile` all 200
  - [x] The team saw `glide.app_users` with the row in the Supabase dashboard
- **Security:** connection details only in `.env`; SSL enforced; test login codes removed from `CLAUDE.md` (public repo)
- **Database:** no new migration; same V1–V4, now in schema `glide`
- **Notes:** the dev project is in Tokyo (`ap-northeast-1`), so each API call takes about 1.2–2.5 s from India; staging and
  production should be in Mumbai (`ap-south-1`)
- **Verified by:** Claude, real Supabase login + Supabase database; the team in the dashboard
