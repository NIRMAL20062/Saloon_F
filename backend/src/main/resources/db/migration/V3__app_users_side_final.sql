-- The onboarding choice (customer or salon) is final once chosen (D-030). The API answers 409 before this is ever hit;
-- this trigger is the database-level guard against any other code path.
-- A new migration rather than an edit of V2, so databases that already ran V2 keep working (Flyway checksums).
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
