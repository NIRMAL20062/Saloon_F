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
