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
