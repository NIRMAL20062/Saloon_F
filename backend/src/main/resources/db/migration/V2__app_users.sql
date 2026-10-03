-- BE-016: one row per person who has signed in (any side).
-- Not tenant-scoped on purpose: one person can be a customer AND a member of several salons.
-- Salon data stays in salon tables with salon_id (BE-017).

CREATE TABLE app_users (
    id         uuid PRIMARY KEY,                                  -- = Supabase auth user id (token "sub")
    phone      text,                                              -- as Supabase returns it, e.g. 919000000001
    side       text CHECK (side IN ('CUSTOMER', 'SALON')),        -- null until onboarding (D-024)
    created_at timestamptz NOT NULL DEFAULT now(),
    updated_at timestamptz NOT NULL DEFAULT now()
);

CREATE INDEX app_users_phone_idx ON app_users (phone);            -- salon invites look people up by phone (BE-017)

CREATE TRIGGER app_users_set_updated_at
    BEFORE UPDATE ON app_users
    FOR EACH ROW EXECUTE FUNCTION set_updated_at();
