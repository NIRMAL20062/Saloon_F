# Database

PostgreSQL 17 · Flyway migrations (`backend/src/main/resources/db/migration`) · Exposed in Kotlin.
Writing a migration: use the `/db-migration` skill.

## Conventions

| Topic | Rule | Decision |
|---|---|---|
| Tenancy | every business table has `salon_id uuid NOT NULL` + an index starting with `salon_id` | product rule |
| Tenant source | `salon_id` comes from the authenticated user, never from the request | DF-13 |
| Uniqueness | per salon: `UNIQUE (salon_id, …)` | DF-13 |
| Keys | `id uuid PRIMARY KEY DEFAULT gen_random_uuid()` | DF-12 |
| Names | tables plural `snake_case`; columns `snake_case`; FKs `<thing>_id` | DF-12 |
| Timestamps | `created_at`, `updated_at` `timestamptz NOT NULL DEFAULT now()`; trigger `set_updated_at()` | DF-12 |
| Money | `bigint` **paise** with `CHECK (… >= 0)`; never `numeric`/float for amounts | DF-11 |
| Rules | enforced with `NOT NULL`, `CHECK`, `UNIQUE`, FKs, not only in Kotlin | - |
| Deletes | `ON DELETE RESTRICT` by default; never cascade-delete payments or invoices | - |

## Migrations

- File: `V<n>__<snake_case_summary>.sql`, `n` = next number. Flyway runs them on backend startup.
- **Never edit a merged migration.** Flyway stores a checksum; the backend refuses to start and `MigrationTest` fails.
  Fix forward with a new file.
- **Backward-compatible first**: running app versions must keep working while a migration rolls out.
  Add nullable/defaulted columns → backfill → then `NOT NULL`. Renames/drops need a plan in the task.
- `flyway clean` is disabled in every environment.
- Every migration is tested on real Postgres by `MigrationTest` (all apply, re-run is a no-op, checksums match).

## Current schema

| Version | What |
|---|---|
| V1 `baseline` | `set_updated_at()` trigger function. No business tables yet (Phase 0) |
| V2 `app_users` | one row per signed-in person: Supabase user id, phone, onboarding side (CUSTOMER / SALON). Platform table: no `salon_id` (one person can be a customer and in several salons) (BE-016) |

## Local database

```bash
docker compose up -d postgres                      # start
docker compose exec postgres psql -U glide glide   # SQL shell
docker compose down -v                             # wipe local data completely
```

## Backups and access (staging / production)

Decided with hosting (Q-004). Minimum before production: daily automated backups with point-in-time recovery, a restore tested
at least once, and production access limited to named people.
