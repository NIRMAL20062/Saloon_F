# Database

PostgreSQL 17 · Flyway migrations (`backend/src/main/resources/db/migration`) · Exposed in Kotlin.
Development database: **Supabase's hosted Postgres** (D-042), schema **`glide`** (DF-26). Tests: a throwaway Postgres in Docker.
Writing a migration: use the `/db-migration` skill.

## Conventions

| Topic | Rule | Decision |
|---|---|---|
| Schema | every table, function and trigger lives in `glide` (Flyway creates it; connections use it); nothing of ours in `public`, which Supabase publishes | DF-26 |
| Tenancy | **salon-owned** tables have `salon_id uuid NOT NULL` + an index starting with `salon_id`; a **customer's own** data is keyed by their `app_user_id`; **platform** tables (`app_users`, `admins`, `webhook_events`, …) have neither | D-025 |
| Tenant source | `salon_id` from the signed-in person's one salon membership (one salon per person); never from the request body | D-035, D-036, DF-13 |
| Row-level security | every salon-owned table has `ENABLE` + `FORCE ROW LEVEL SECURITY` and a policy on `current_setting('app.salon_id')`; Flyway runs as the owner user, the backend as a limited user that is neither superuser nor owner (from BE-017) | D-027 |
| Uniqueness | per salon: `UNIQUE (salon_id, …)` | DF-13 |
| Keys | `id uuid PRIMARY KEY DEFAULT gen_random_uuid()` | DF-12 |
| Names | tables plural `snake_case`; columns `snake_case`; FKs `<thing>_id` | DF-12 |
| Timestamps | `created_at`, `updated_at` `timestamptz NOT NULL DEFAULT now()`; trigger `set_updated_at()` | DF-12 |
| Money | `bigint` **paise** with `CHECK (… >= 0)`; never `numeric`/float for amounts | DF-11 |
| Rules | enforced with `NOT NULL`, `CHECK`, `UNIQUE`, FKs, not only in Kotlin | - |
| Deletes | `ON DELETE RESTRICT` by default; never cascade-delete payments or invoices | - |
| Sensitive data | bank account numbers encrypted in the column (key from env), last 4 digits kept separately for display; never logged | DF-24 |

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
| V3 `app_users_side_final` | trigger: once `side` is set it can't change (D-030, BE-016) |
| V4 `app_users_profile` | `name` (2–60) and `email` (≤ 254, email-shaped) on `app_users`, one profile per person (DF-18, BE-018) |
| V5 `admins_and_audit_log` | `admins`: who is an admin of our team, keyed by Supabase user id; lower-case unique email; INVITED → ACTIVE; `invited_by`; only one admin without an inviter. `audit_log`: who / salon / action / entity / before / after / request ID; UPDATE, DELETE and TRUNCATE refused by triggers (BE-020, DF-28, DF-29) |

## Development database (Supabase)

1. Supabase → **Connect** (top of the project page) → **Session pooler** (not "Transaction pooler": migrations need a session).
2. In `.env` (never in git), turn its `postgresql://USER:PASSWORD@HOST:5432/postgres` into:
   ```
   DATABASE_URL=jdbc:postgresql://HOST:5432/postgres?sslmode=require
   DATABASE_USER=USER              # postgres.<project ref>
   DATABASE_PASSWORD=PASSWORD      # the database password (Project Settings → Database → reset it if unknown)
   ```
3. `./gradlew :backend:run`: Flyway creates the `glide` schema and every table. In the dashboard: **Table Editor → schema
   `glide`**.

Local Postgres in Docker (`docker compose up -d postgres`, the old `.env` values) still works for offline work.

## Local database (optional)

```bash
docker compose up -d postgres                      # start
docker compose exec postgres psql -U glide glide   # SQL shell (then: SET search_path TO glide;)
docker compose down -v                             # wipe local data completely
```

## Backups and access (staging / production)

Decided with hosting (Q-004). Minimum before production: daily automated backups with point-in-time recovery, a restore tested
at least once, and production access limited to named people.
