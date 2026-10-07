-- BE-017: a salon, the tenant itself (D-025). Its row is visible and writable only in a transaction for that salon
-- (row-level security, D-027, BE-031), so the backend makes the id first and opens the transaction for it.
-- Profile fields: D-046, DF-32. The owner and staff are in salon_members (V8).
CREATE TABLE salons (
    id               uuid PRIMARY KEY DEFAULT gen_random_uuid(),
    name             text NOT NULL CHECK (char_length(name) BETWEEN 1 AND 30 AND btrim(name) = name),
    phone            text NOT NULL CHECK (phone ~ '^91[2-9][0-9]{9}$'),        -- Indian number, stored as 91XXXXXXXXXX
    address_line1    text NOT NULL CHECK (char_length(btrim(address_line1)) BETWEEN 1 AND 100),  -- house/shop/building, street
    address_area     text NOT NULL CHECK (char_length(btrim(address_area)) BETWEEN 1 AND 100),   -- area or locality
    address_landmark text CHECK (char_length(btrim(address_landmark)) BETWEEN 1 AND 100),         -- optional
    city             text NOT NULL CHECK (char_length(btrim(city)) BETWEEN 1 AND 50),
    state            text NOT NULL CHECK (state IN (
        'ANDHRA_PRADESH', 'ARUNACHAL_PRADESH', 'ASSAM',
        'BIHAR', 'CHHATTISGARH', 'GOA',
        'GUJARAT', 'HARYANA', 'HIMACHAL_PRADESH',
        'JHARKHAND', 'KARNATAKA', 'KERALA',
        'MADHYA_PRADESH', 'MAHARASHTRA', 'MANIPUR',
        'MEGHALAYA', 'MIZORAM', 'NAGALAND',
        'ODISHA', 'PUNJAB', 'RAJASTHAN',
        'SIKKIM', 'TAMIL_NADU', 'TELANGANA',
        'TRIPURA', 'UTTAR_PRADESH', 'UTTARAKHAND',
        'WEST_BENGAL', 'ANDAMAN_AND_NICOBAR_ISLANDS', 'CHANDIGARH',
        'DADRA_AND_NAGAR_HAVELI_AND_DAMAN_AND_DIU', 'DELHI', 'JAMMU_AND_KASHMIR',
        'LADAKH', 'LAKSHADWEEP', 'PUDUCHERRY'
    )),
    pincode          text NOT NULL CHECK (pincode ~ '^[1-9][0-9]{5}$'),
    type             text NOT NULL CHECK (type IN ('MEN', 'WOMEN', 'UNISEX')),
    status           text NOT NULL DEFAULT 'DRAFT'
                     CHECK (status IN ('DRAFT', 'UNDER_VERIFICATION', 'LIVE', 'REJECTED', 'SUSPENDED')),
    rejection_reason text CHECK (char_length(btrim(rejection_reason)) BETWEEN 1 AND 500),
    created_at       timestamptz NOT NULL DEFAULT now(),
    updated_at       timestamptz NOT NULL DEFAULT now(),
    CONSTRAINT salons_rejected_has_reason CHECK (status <> 'REJECTED' OR rejection_reason IS NOT NULL)
);

CREATE TRIGGER salons_set_updated_at BEFORE UPDATE ON salons
    FOR EACH ROW EXECUTE FUNCTION set_updated_at();

ALTER TABLE salons ENABLE ROW LEVEL SECURITY;
ALTER TABLE salons FORCE ROW LEVEL SECURITY;
CREATE POLICY salons_own_salon ON salons
    USING (id = nullif(current_setting('app.salon_id', true), '')::uuid);
