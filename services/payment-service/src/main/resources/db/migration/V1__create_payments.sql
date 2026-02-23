CREATE TABLE payments (
    id              BIGSERIAL PRIMARY KEY,
    order_id        BIGINT NOT NULL UNIQUE,
    user_email      VARCHAR(255) NOT NULL,
    amount          NUMERIC(12,2) NOT NULL,
    status          VARCHAR(30)  NOT NULL DEFAULT 'PENDING',
    transaction_id  VARCHAR(255) UNIQUE,
    failure_reason  TEXT,
    version         BIGINT NOT NULL DEFAULT 0,
    created_at      TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at      TIMESTAMP NOT NULL DEFAULT NOW(),
    completed_at    TIMESTAMP
);

CREATE INDEX idx_payments_order_id   ON payments(order_id);
CREATE INDEX idx_payments_user_email ON payments(user_email);
CREATE INDEX idx_payments_status     ON payments(status);
