-- BE-031 (D-027): the limited role the backend's queries run as. Not a login, not superuser, owner of nothing, no BYPASSRLS,
-- so row-level security applies to it (on Supabase our owner login has BYPASSRLS). Every pooled connection switches to it
-- (SET ROLE, DatabaseFactory); Flyway keeps migrating as the owner login. No password anywhere: nothing secret here.
DO $$
BEGIN
    IF NOT EXISTS (SELECT FROM pg_roles WHERE rolname = 'glide_app') THEN
        CREATE ROLE glide_app NOLOGIN NOSUPERUSER NOBYPASSRLS NOCREATEDB NOCREATEROLE NOINHERIT;
    END IF;
END
$$;

-- The owner login may switch to it (SET), without taking on its rights (INHERIT FALSE).
GRANT glide_app TO CURRENT_USER WITH INHERIT FALSE, SET TRUE;

GRANT USAGE ON SCHEMA glide TO glide_app;
GRANT SELECT, INSERT, UPDATE, DELETE ON ALL TABLES IN SCHEMA glide TO glide_app;
GRANT USAGE, SELECT ON ALL SEQUENCES IN SCHEMA glide TO glide_app;
-- Only Flyway writes the migration history; the audit log is append-only (DF-28; its triggers refuse it as well).
REVOKE INSERT, UPDATE, DELETE ON flyway_schema_history FROM glide_app;
REVOKE UPDATE, DELETE ON audit_log FROM glide_app;

-- Tables and sequences that later migrations create (as this same owner login) get the same rights.
ALTER DEFAULT PRIVILEGES IN SCHEMA glide GRANT SELECT, INSERT, UPDATE, DELETE ON TABLES TO glide_app;
ALTER DEFAULT PRIVILEGES IN SCHEMA glide GRANT USAGE, SELECT ON SEQUENCES TO glide_app;
