# backend/: Ktor API

Kotlin · Ktor 3 (Netty) · Exposed · Flyway · PostgreSQL 17 · HikariCP. JVM 21.

## Layout (package `com.glide.backend`)

```
Application.kt        main(), AppDependencies, Application.module(config, deps): wires plugins + routes
config/               AppConfig: env vars only, validated at startup
db/                   DatabaseFactory: pool, migrations, Exposed
plugins/              Security, Monitoring (request IDs, logs), ErrorHandling (error envelope), Serialization
health/               /health, /health/live
<feature>/            one package per feature: <Feature>Routes.kt, <Feature>Service.kt, <Feature>Repository.kt, tables
src/main/resources/db/migration/   Flyway SQL: V<n>__<snake_case>.sql
```

## Rules

- Routes are thin: parse + validate input → call a service → respond with a `shared` DTO. Business rules live in services.
- All SQL goes through repositories (Exposed). Every query on a salon-owned table filters by `salon_id`, taken from the
  `X-Salon-Id` header **only after** checking the user is a member with the needed role (D-026); never from the request body.
  A customer's own data is filtered by their user id (D-025). Row-level security is the second guard (D-027, docs/DATABASE.md).
- External calls (Razorpay, WhatsApp, FCM, R2) go behind an interface in `AppDependencies` so tests can swap in fakes.
- Errors: throw/handle so the client gets `ErrorResponse` with a stable `ErrorCodes` value. Never `call.respondText(e.message)`.
- Config: add new env vars to `AppConfig.fromEnv` (validated), `.env.example`, and the test `testConfig()`.
- Webhooks (Razorpay, WhatsApp): verify the signature **before** parsing, and make handlers idempotent (same event twice = no double effect).
- Logging: never log request bodies, phone numbers, OTPs, tokens or payment details. Log IDs instead.

## Tests

- `testApplication { application { module(testConfig(), deps) } }`: see `health/ReadinessRouteTest.kt`.
- Real DB: `TestDatabase.dataSource` (Testcontainers Postgres, migrated once per run). Needs Docker running.
- Every new control or rule gets its own test (see `plugins/SecurityBaselineTest.kt` for the style).

## Commands

```bash
docker compose up -d postgres
./gradlew :backend:run          # uses repo-root .env
./gradlew :backend:check        # ktlint + tests
docker build -f backend/Dockerfile -t glide-backend .   # from repo root
```
