-- =============================================================================
-- V1__create_users_table.sql
-- =============================================================================
-- Flyway migration naming convention:
--   V<version>__<description>.sql
--   V1 = version 1 (first migration)
--   __ = double underscore separator
--   create_users_table = human-readable description
--
-- Flyway runs migrations in version order (V1, V2, V3...).
-- Once a migration has been applied, it is NEVER run again.
-- Flyway tracks applied migrations in a "flyway_schema_history" table.
--
-- IMPORTANT: Never modify a migration after it's been applied to a database.
-- Instead, create a new migration (V2__...) with the changes.
-- =============================================================================

CREATE TABLE users (
    id              BIGSERIAL PRIMARY KEY,
    email           VARCHAR(255) NOT NULL UNIQUE,
    password_hash   VARCHAR(255) NOT NULL,
    first_name      VARCHAR(100) NOT NULL,
    last_name       VARCHAR(100) NOT NULL,
    phone           VARCHAR(20),
    role            VARCHAR(20)  NOT NULL DEFAULT 'CUSTOMER',
    created_at      TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at      TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- Index on email for fast login lookups.
-- Without this, every login would scan the entire table.
CREATE INDEX idx_users_email ON users(email);
