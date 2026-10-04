-- BE-020: who is an admin of our team (D-013, DF-16), and the audit log (DF-28).
-- Platform tables, not salon-owned: admins work across all salons, so neither table is scoped by salon_id.

-- Our database, not Supabase, decides who is an admin. user_id is the Supabase login the admin was invited as;
-- every /v1/admin request is matched on it (never on the token's email).
CREATE TABLE admins (
    user_id      uuid PRIMARY KEY,                                   -- = Supabase auth user id (token "sub")
    email        text NOT NULL UNIQUE
                 CHECK (email = lower(email)
                     AND char_length(email) <= 254
                     AND email ~ '^[^[:space:]@]+@[^[:space:]@]+\.[^[:space:]@]+$'),
    status       text NOT NULL DEFAULT 'INVITED' CHECK (status IN ('INVITED', 'ACTIVE')),
    invited_by   uuid REFERENCES admins (user_id) ON DELETE RESTRICT, -- null only for the first admin
    activated_at timestamptz,                                         -- first admin login with the authenticator app
    created_at   timestamptz NOT NULL DEFAULT now(),
    updated_at   timestamptz NOT NULL DEFAULT now(),
    CHECK ((status = 'ACTIVE') = (activated_at IS NOT NULL))
);

-- Exactly one admin without an inviter: the first one, made by the one-off command. Everyone else is invited.
-- Also makes two racing runs of the command safe.
CREATE UNIQUE INDEX admins_only_one_first ON admins ((true)) WHERE invited_by IS NULL;

CREATE TRIGGER admins_set_updated_at
    BEFORE UPDATE ON admins
    FOR EACH ROW EXECUTE FUNCTION set_updated_at();

-- Every admin action and, from BE-019, every create/update/delete in salons. Append-only: no updated_at because rows
-- never change.
CREATE TABLE audit_log (
    id            uuid PRIMARY KEY DEFAULT gen_random_uuid(),
    actor_user_id uuid,                                  -- who did it; null = a command our team ran on the server
    salon_id      uuid,                                  -- null for platform actions (e.g. inviting an admin)
    action        text NOT NULL CHECK (action ~ '^[A-Z][A-Z_]{1,63}$'),
    entity_type   text NOT NULL CHECK (entity_type ~ '^[a-z][a-z_]{1,63}$'),
    entity_id     text NOT NULL CHECK (char_length(entity_id) BETWEEN 1 AND 128),
    before        jsonb,
    after         jsonb,
    request_id    text CHECK (char_length(request_id) <= 64),
    created_at    timestamptz NOT NULL DEFAULT now()
);

CREATE INDEX audit_log_entity_idx ON audit_log (entity_type, entity_id, created_at);
CREATE INDEX audit_log_salon_idx ON audit_log (salon_id, created_at) WHERE salon_id IS NOT NULL;

-- Nobody rewrites history: UPDATE, DELETE and TRUNCATE are refused for every database user, including ours.
CREATE FUNCTION audit_log_is_append_only() RETURNS trigger
    LANGUAGE plpgsql AS
$$
BEGIN
    RAISE EXCEPTION 'audit_log is append-only' USING ERRCODE = 'insufficient_privilege';
END;
$$;

CREATE TRIGGER audit_log_no_update_or_delete
    BEFORE UPDATE OR DELETE ON audit_log
    FOR EACH ROW EXECUTE FUNCTION audit_log_is_append_only();

CREATE TRIGGER audit_log_no_truncate
    BEFORE TRUNCATE ON audit_log
    FOR EACH STATEMENT EXECUTE FUNCTION audit_log_is_append_only();
