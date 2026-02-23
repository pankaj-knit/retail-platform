-- =============================================================================
-- V2: Failed Events table (Dead Letter persistence)
-- =============================================================================
-- Stores Kafka events that failed processing after all retries.
-- This is the persistent "dead letter queue" that ops can query, review,
-- and manually retry from.
--
-- Without this table, failed events only exist in Kafka's DLT topic
-- (hard to query) or in log files (lost on pod restart).
-- =============================================================================

CREATE TABLE failed_events (
    id              BIGSERIAL PRIMARY KEY,
    topic           VARCHAR(255)   NOT NULL,
    event_key       VARCHAR(255),
    payload         JSONB          NOT NULL,
    error_message   TEXT,
    status          VARCHAR(50)    NOT NULL DEFAULT 'FAILED',
    retry_count     INTEGER        NOT NULL DEFAULT 0,
    max_retries     INTEGER        NOT NULL DEFAULT 3,
    created_at      TIMESTAMP      NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at      TIMESTAMP      NOT NULL DEFAULT CURRENT_TIMESTAMP,
    resolved_at     TIMESTAMP
);

CREATE INDEX idx_failed_events_status ON failed_events(status);
CREATE INDEX idx_failed_events_topic ON failed_events(topic);
CREATE INDEX idx_failed_events_created_at ON failed_events(created_at);
