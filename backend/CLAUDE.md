# backend/: Ktor API

Kotlin · Ktor 3 (Netty) · Exposed · Flyway · PostgreSQL 17 · HikariCP. JVM 21.

## Layout (package `com.glide.backend`)

```
Application.kt        main(), AppDependencies, Application.module(config, deps): wires plugins + routes
config/               AppConfig: env vars only, validated at startup
db/                   DatabaseFactory: pool, migrations, Exposed; Transactor: one transaction per piece of work
plugins/              Security, Monitoring (request IDs, logs), ErrorHandling (error envelope), Serialization
health/               /health, /health/live
admins/               admin access guard (adminOnly), /v1/admin routes, Supabase admin API client, addFirstAdmin command
audit/                AuditLog.record(): write inside the same transaction as the change (DF-28)
validation/           shared input rules (Emails)
<feature>/            one package per feature: <Feature>Routes.kt, <Feature>Service.kt, <Feature>Repository.kt, tables
src/main/resources/db/migration/   Flyway SQL: V<n>__<snake_case>.sql
```

## Rules

- Routes are thin: parse + validate input → call a service → respond with a `shared` DTO. Business rules live in services.
- **Services own the transaction** (BE-025): a service runs each piece of work in one `transactor.transaction { }`, so
  "check, then write" is one step; repositories run inside it and never open their own (Exposed refuses a query outside a
  transaction). Never call Supabase or anything slow inside the block (it can't suspend, on purpose).
- All SQL goes through repositories (Exposed). Every query on a salon-owned table filters by `salon_id`, taken from the
  signed-in person's one salon membership (D-035, D-036), with a role check; never from the request body.
  A customer's own data is filtered by their user id (D-025). Row-level security is the second guard (D-027, docs/DATABASE.md).
- External calls (Razorpay, WhatsApp, FCM, R2) go behind an interface in `AppDependencies` so tests can swap in fakes.
- Errors: throw/handle so the client gets `ErrorResponse` with a stable `ErrorCodes` value. Never `call.respondText(e.message)`.
- Config: add new env vars to `AppConfig.fromEnv` (validated), `.env.example`, and the test `testConfig()`.
- Webhooks (Razorpay, WhatsApp): verify the signature **before** parsing, and make handlers idempotent (same event twice = no double effect).
- Admin endpoints go inside `adminOnly(admins) { }` (admin + authenticator app, DF-29); `AdminRoutesTest` fails if one doesn't.
- Logging: never log request bodies, phone numbers, OTPs, tokens, payment or bank details. Log IDs instead.

## Tests

- `testApplication { application { module(testConfig(), deps) } }`: see `health/ReadinessRouteTest.kt`.
- Real DB: `TestDatabase.dataSource` (Testcontainers Postgres, migrated once per run). Needs Docker running.
- Every new control or rule gets its own test (see `plugins/SecurityBaselineTest.kt` for the style).

## Commands

```bash
docker compose up -d postgres
./gradlew :backend:run          # uses repo-root .env
./gradlew :backend:check        # ktlint + tests
./gradlew :backend:addFirstAdmin --args="admin@example.com"   # one-off: first admin (needs SUPABASE_SECRET_KEY)
docker build -f backend/Dockerfile -t glide-backend .   # from repo root
```
