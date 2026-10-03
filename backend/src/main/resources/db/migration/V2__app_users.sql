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

-- The onboarding choice is final (D-030): once set, it can't change. The API answers 409 before this is ever hit;
-- the trigger is the database-level guard against any other code path.
CREATE FUNCTION app_users_side_is_final() RETURNS trigger
    LANGUAGE plpgsql AS
$$
BEGIN
    IF OLD.side IS NOT NULL AND NEW.side IS DISTINCT FROM OLD.side THEN
        RAISE EXCEPTION 'app_users.side is final once chosen (D-030)' USING ERRCODE = 'check_violation';
    END IF;
    RETURN NEW;
END;
$$;

CREATE TRIGGER app_users_side_final
    BEFORE UPDATE OF side ON app_users
    FOR EACH ROW EXECUTE FUNCTION app_users_side_is_final();

