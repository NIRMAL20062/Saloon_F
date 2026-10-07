-- BE-032 (DF-24, DF-33): a salon's bank account, where its payouts go; one per salon. The account number is stored only
-- encrypted (AES-256-GCM; the key lives in the environment, never in the database) and bound to its salon; its last 4
-- digits are kept apart for the masked view. A plain number can't be stored: the check accepts only the encrypted form.
-- Row-level security (D-027, BE-031): visible and writable only in the salon's own transaction.
CREATE TABLE salon_bank_details (
    salon_id                 uuid PRIMARY KEY REFERENCES salons (id) ON DELETE RESTRICT,
    account_holder_name      text NOT NULL
                             CHECK (char_length(account_holder_name) BETWEEN 1 AND 100
                                    AND btrim(account_holder_name) = account_holder_name),
    account_number_encrypted text NOT NULL CHECK (account_number_encrypted ~ '^v1:[A-Za-z0-9+/]+={0,2}$'),
    account_number_last4     text NOT NULL CHECK (account_number_last4 ~ '^[0-9]{4}$'),
    ifsc                     text NOT NULL CHECK (ifsc ~ '^[A-Z]{4}0[A-Z0-9]{6}$'),
    created_at               timestamptz NOT NULL DEFAULT now(),
    updated_at               timestamptz NOT NULL DEFAULT now()
);

CREATE TRIGGER salon_bank_details_set_updated_at BEFORE UPDATE ON salon_bank_details
    FOR EACH ROW EXECUTE FUNCTION set_updated_at();

ALTER TABLE salon_bank_details ENABLE ROW LEVEL SECURITY;
ALTER TABLE salon_bank_details FORCE ROW LEVEL SECURITY;
CREATE POLICY salon_bank_details_own_salon ON salon_bank_details
    USING (salon_id = nullif(current_setting('app.salon_id', true), '')::uuid);
