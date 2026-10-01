---
name: db-migration
description: Write a safe Flyway migration for Glide's PostgreSQL schema (multi-tenant salon_id, constraints, indexes, updated_at trigger) with tests. Use for any table, column, index or constraint change.
---

# Write a database migration

Rules of record: `docs/DATABASE.md`. Only for schema changes a task asks for.

## 1. File

`backend/src/main/resources/db/migration/V<next>__<snake_case_summary>.sql`, where `<next>` = highest existing V number + 1.
**Never edit a migration that's merged.** Fix forward with a new file (CI's `MigrationTest` detects edits).

## 2. Table checklist

```sql
CREATE TABLE <plural_snake> (
    id          uuid PRIMARY KEY DEFAULT gen_random_uuid(),
    salon_id    uuid NOT NULL REFERENCES salons (id),          -- every business table (tenant)
    -- columns: NOT NULL unless truly optional; CHECK constraints for ranges/enums;
    -- money: bigint paise with CHECK (amount_paise >= 0); timestamps: timestamptz
    created_at  timestamptz NOT NULL DEFAULT now(),
    updated_at  timestamptz NOT NULL DEFAULT now()
);
CREATE INDEX <table>_salon_id_idx ON <table> (salon_id);       -- plus indexes for every frequent filter, salon_id first
CREATE TRIGGER <table>_set_updated_at BEFORE UPDATE ON <table>
    FOR EACH ROW EXECUTE FUNCTION set_updated_at();
```

- Uniqueness is per salon: `UNIQUE (salon_id, <col>)`, not global, unless the task says otherwise.
- Foreign keys to other business tables should also carry `salon_id` so a row can't point into another salon
  (composite FK `(salon_id, x_id) REFERENCES x (salon_id, id)` where practical).
- Decide `ON DELETE` explicitly (usually `RESTRICT`); never cascade-delete payments or invoices.

## 3. Safe changes on tables with data

- Add a column as nullable or with a default; backfill in a later migration if needed; then add `NOT NULL`.
- Rename/drop = breaking for running app versions. Ask first; usually add new → migrate → drop later.
- Large-table indexes: `CREATE INDEX CONCURRENTLY` in its own migration (add `-- flyway:executeInTransaction=false`
  only if needed and agreed).

## 4. Exposed

Table object in the feature package, column names and types identical to the SQL.

## 5. Tests

- `MigrationTest` already proves all migrations apply on real Postgres. Run `./gradlew :backend:test`.
- Add a test per constraint that matters: e.g. inserting a negative amount fails, a duplicate per-salon name fails,
  and the same name in another salon succeeds.
