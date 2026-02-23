ALTER TABLE products ADD COLUMN version BIGINT NOT NULL DEFAULT 0;
ALTER TABLE inventory ADD COLUMN version BIGINT NOT NULL DEFAULT 0;
ALTER TABLE failed_events ADD COLUMN version BIGINT NOT NULL DEFAULT 0;

CREATE INDEX idx_failed_events_status_created_at ON failed_events(status, created_at DESC);
