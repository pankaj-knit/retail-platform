CREATE TABLE orders (
    id              BIGSERIAL PRIMARY KEY,
    user_email      VARCHAR(255) NOT NULL,
    status          VARCHAR(30)  NOT NULL DEFAULT 'PENDING',
    total_amount    NUMERIC(12,2) NOT NULL,
    shipping_address TEXT NOT NULL,
    version         BIGINT NOT NULL DEFAULT 0,
    created_at      TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at      TIMESTAMP NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_orders_user_email ON orders(user_email);
CREATE INDEX idx_orders_status     ON orders(status);

CREATE TABLE order_items (
    id                 BIGSERIAL PRIMARY KEY,
    order_id           BIGINT NOT NULL REFERENCES orders(id) ON DELETE CASCADE,
    product_id         BIGINT NOT NULL,
    product_name       VARCHAR(255) NOT NULL,
    quantity           INT NOT NULL CHECK (quantity > 0),
    unit_price         NUMERIC(10,2) NOT NULL,
    subtotal           NUMERIC(12,2) NOT NULL,
    inventory_reserved BOOLEAN NOT NULL DEFAULT FALSE,
    version            BIGINT NOT NULL DEFAULT 0
);

CREATE INDEX idx_order_items_order_id ON order_items(order_id);
