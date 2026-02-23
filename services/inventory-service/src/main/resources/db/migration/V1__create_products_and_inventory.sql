-- =============================================================================
-- V1: Products + Inventory tables
-- =============================================================================
-- Two tables:
--   products:  The catalog (name, description, price, image)
--   inventory: Stock levels per product (quantity, reserved)
--
-- Why separate tables instead of a "quantity" column on products?
--   - Products = catalog data (rarely changes)
--   - Inventory = operational data (changes on every order)
--   - Separating them avoids lock contention: updating stock doesn't
--     block someone reading product details.
--   - In a larger system, inventory could be in a different database entirely.
-- =============================================================================

CREATE TABLE products (
    id              BIGSERIAL PRIMARY KEY,
    name            VARCHAR(255)   NOT NULL,
    description     TEXT,
    price           DECIMAL(10, 2) NOT NULL,
    category        VARCHAR(100),
    image_url       VARCHAR(500),
    active          BOOLEAN        NOT NULL DEFAULT true,
    created_at      TIMESTAMP      NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at      TIMESTAMP      NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE inventory (
    id              BIGSERIAL PRIMARY KEY,
    product_id      BIGINT         NOT NULL UNIQUE REFERENCES products(id),
    quantity         INTEGER        NOT NULL DEFAULT 0,
    reserved        INTEGER        NOT NULL DEFAULT 0,
    -- "reserved" tracks stock that's been claimed by an order but not yet
    -- paid for. Available stock = quantity - reserved.
    -- This prevents overselling: two orders can't claim the same item.
    updated_at      TIMESTAMP      NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_products_category ON products(category);
CREATE INDEX idx_products_active ON products(active);
CREATE INDEX idx_inventory_product_id ON inventory(product_id);

-- Seed some sample products for the retail app
INSERT INTO products (name, description, price, category, image_url) VALUES
    ('Wireless Bluetooth Headphones', 'Premium noise-cancelling over-ear headphones with 30hr battery', 79.99, 'Electronics', '/images/headphones.jpg'),
    ('Organic Cotton T-Shirt', 'Comfortable everyday t-shirt made from 100% organic cotton', 24.99, 'Clothing', '/images/tshirt.jpg'),
    ('Stainless Steel Water Bottle', 'Double-wall insulated 750ml bottle keeps drinks cold 24hrs', 19.99, 'Home & Kitchen', '/images/bottle.jpg'),
    ('Programming in Java 25', 'Comprehensive guide to modern Java development', 49.99, 'Books', '/images/java-book.jpg'),
    ('USB-C Fast Charger', '65W GaN charger with 3 ports for laptop and phone', 34.99, 'Electronics', '/images/charger.jpg'),
    ('Running Shoes', 'Lightweight mesh running shoes with responsive cushioning', 89.99, 'Clothing', '/images/shoes.jpg'),
    ('Mechanical Keyboard', 'RGB backlit mechanical keyboard with Cherry MX switches', 129.99, 'Electronics', '/images/keyboard.jpg'),
    ('Coffee Mug Set', 'Set of 4 ceramic mugs, 350ml each, dishwasher safe', 29.99, 'Home & Kitchen', '/images/mugs.jpg');

-- Seed inventory for each product
INSERT INTO inventory (product_id, quantity) VALUES
    (1, 150),
    (2, 300),
    (3, 200),
    (4, 75),
    (5, 250),
    (6, 100),
    (7, 80),
    (8, 120);
