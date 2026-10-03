-- BE-018: one profile per person on app_users (DF-18): customers and salon people alike.
-- Both optional at the database level: a person has no name until they finish onboarding.
-- The API validates and normalises (trimmed name, lower-case email); these checks are the second guard.

ALTER TABLE app_users
    ADD COLUMN name  text CHECK (char_length(name) BETWEEN 2 AND 60),
    ADD COLUMN email text CHECK (char_length(email) <= 254 AND email ~ '^[^[:space:]@]+@[^[:space:]@]+\.[^[:space:]@]+$');
