-- migration_01_users.sql — Extends the users table for self-service sign up.
-- Idempotent and back-compatible: the existing demo user (nulls in the new
-- columns) keeps working after this runs.

ALTER TABLE users ADD COLUMN IF NOT EXISTS full_name   VARCHAR(100);
ALTER TABLE users ADD COLUMN IF NOT EXISTS email       VARCHAR(120);
ALTER TABLE users ADD COLUMN IF NOT EXISTS phone       VARCHAR(20);
ALTER TABLE users ADD COLUMN IF NOT EXISTS created_at  TIMESTAMP NOT NULL DEFAULT NOW();

-- Unique on email as an index so legacy rows with NULL email never conflict
-- (PostgreSQL allows multiple NULLs in a unique index).
CREATE UNIQUE INDEX IF NOT EXISTS uniq_users_email ON users (email);
