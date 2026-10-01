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
- **Database:** migrations run on container start (fresh-database run happens in CI end-to-end tests, BE-011)

### BE-010 · OpenAPI spec + contract test
- **Completed:** 2026-10-01 · **Commits:** `16596a2`
- **Built:** [docs/api/openapi.yaml](../docs/api/openapi.yaml) (OpenAPI 3.0.3): `/health/live`, `/health` (200/503), error envelope as `default` response, `X-Request-Id` header; `additionalProperties: false` so undocumented fields fail
- **Tests:** `OpenApiContractTest` (6): 200, 503 and 429 responses validated against the spec; the validator rejects a bad body (guards the guard); every registered backend route is in the spec and vice versa
- **Notes:** validator `swagger-request-validator-core` 2.46.1 (3.0.0 on Maven Central has no jar)
