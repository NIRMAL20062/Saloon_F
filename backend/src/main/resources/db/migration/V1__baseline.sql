-- Phase 0 baseline. No business tables yet; they start in Phase 1.
--
-- Conventions every later migration follows (see docs/DATABASE.md):
--   * every business table has salon_id (multi-tenant from day 1)
--   * every table has created_at and updated_at (timestamptz, default now())
--   * updated_at is kept current by the trigger function below
--   * migrations are never edited after merge; fix forward with a new V<n> file

CREATE OR REPLACE FUNCTION set_updated_at() RETURNS trigger
    LANGUAGE plpgsql AS
$$
BEGIN
    NEW.updated_at = now();
    RETURN NEW;
END;
$$;

COMMENT ON FUNCTION set_updated_at() IS
    'Attach with: CREATE TRIGGER <table>_set_updated_at BEFORE UPDATE ON <table> FOR EACH ROW EXECUTE FUNCTION set_updated_at();';
