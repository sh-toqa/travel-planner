-- Rename to a clearer column name; RENAME keeps existing data.
ALTER TABLE users RENAME COLUMN name TO display_name;

-- Existing rows receive now() from the default, so NOT NULL is safe.
ALTER TABLE users ADD COLUMN created_at TIMESTAMPTZ NOT NULL DEFAULT now();

-- Replace the case-sensitive unique constraint with a case-insensitive unique index.
ALTER TABLE users DROP CONSTRAINT uk6dotkott2kjsp8vw4d0m25fb7;
CREATE UNIQUE INDEX ux_users_email_lower ON users (lower(email));