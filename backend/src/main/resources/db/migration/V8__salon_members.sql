-- BE-017: who belongs to a salon, and as what (D-035, D-039). Salon-owned. Row-level security (D-027, BE-031): a salon's
-- transaction sees its members, and a person sees their own membership, which is how the backend finds their one salon
-- (D-036) before any salon is set.
CREATE TABLE salon_members (
    id         uuid PRIMARY KEY DEFAULT gen_random_uuid(),
    salon_id   uuid NOT NULL REFERENCES salons (id) ON DELETE RESTRICT,
    phone      text NOT NULL CHECK (phone ~ '^[1-9][0-9]{7,14}$'),  -- login phone, E.164 without "+", like app_users
    user_id    uuid REFERENCES app_users (id) ON DELETE RESTRICT,  -- null until an added staff number logs in (BE-033)
    role       text NOT NULL CHECK (role IN ('OWNER', 'STAFF')),
    status     text NOT NULL DEFAULT 'ACTIVE' CHECK (status IN ('ACTIVE', 'REMOVED')),
    created_at timestamptz NOT NULL DEFAULT now(),
    updated_at timestamptz NOT NULL DEFAULT now(),
    CONSTRAINT salon_members_owner_has_user CHECK (role <> 'OWNER' OR user_id IS NOT NULL)
);

CREATE INDEX salon_members_salon_id_idx ON salon_members (salon_id);
-- One salon per person (D-035): at most one active membership per login and per phone, across all salons.
CREATE UNIQUE INDEX salon_members_one_active_per_user ON salon_members (user_id) WHERE status = 'ACTIVE';
CREATE UNIQUE INDEX salon_members_one_active_per_phone ON salon_members (phone) WHERE status = 'ACTIVE';
-- One owner per salon (D-039); the owner is added in the same transaction that creates the salon.
CREATE UNIQUE INDEX salon_members_one_owner ON salon_members (salon_id) WHERE role = 'OWNER' AND status = 'ACTIVE';

CREATE TRIGGER salon_members_set_updated_at BEFORE UPDATE ON salon_members
    FOR EACH ROW EXECUTE FUNCTION set_updated_at();

ALTER TABLE salon_members ENABLE ROW LEVEL SECURITY;
ALTER TABLE salon_members FORCE ROW LEVEL SECURITY;
CREATE POLICY salon_members_salon_or_self ON salon_members
    USING (salon_id = nullif(current_setting('app.salon_id', true), '')::uuid
           OR user_id = nullif(current_setting('app.user_id', true), '')::uuid)
    WITH CHECK (salon_id = nullif(current_setting('app.salon_id', true), '')::uuid);
