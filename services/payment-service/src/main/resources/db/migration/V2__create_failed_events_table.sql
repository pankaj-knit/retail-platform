CREATE TABLE failed_events (
    id            BIGSERIAL PRIMARY KEY,
    topic         VARCHAR(255)  NOT NULL,
    event_key     VARCHAR(255),
    payload       JSONB         NOT NULL,
    error_message TEXT,
    status        VARCHAR(30)   NOT NULL DEFAULT 'FAILED',
    retry_count   INT           NOT NULL DEFAULT 0,
    max_retries   INT           NOT NULL DEFAULT 3,
    version       BIGINT        NOT NULL DEFAULT 0,
    created_at    TIMESTAMP     NOT NULL DEFAULT NOW(),
    updated_at    TIMESTAMP     NOT NULL DEFAULT NOW(),
    resolved_at   TIMESTAMP
);

CREATE INDEX idx_failed_events_status_created_at ON failed_events(status, created_at DESC);
