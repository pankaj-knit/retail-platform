-- =============================================================================
-- V4: Seed product catalog to 50 items with real images
-- =============================================================================
-- Uses Unsplash Source for product images (free, no auth required).
-- Adds 42 products across 8 categories to complement the existing 8.
-- =============================================================================

-- ─── Update original 8 products with real images ───
UPDATE products SET image_url = 'https://images.unsplash.com/photo-1505740420928-5e560c06d30e?w=400&h=400&fit=crop' WHERE id = 1;
UPDATE products SET image_url = 'https://images.unsplash.com/photo-1521572163474-6864f9cf17ab?w=400&h=400&fit=crop' WHERE id = 2;
UPDATE products SET image_url = 'https://images.unsplash.com/photo-1602143407151-7111542de6e8?w=400&h=400&fit=crop' WHERE id = 3;
UPDATE products SET image_url = 'https://images.unsplash.com/photo-1532012197267-da84d127e765?w=400&h=400&fit=crop' WHERE id = 4;
UPDATE products SET image_url = 'https://images.unsplash.com/photo-1583863788434-e58a36330cf0?w=400&h=400&fit=crop' WHERE id = 5;
UPDATE products SET image_url = 'https://images.unsplash.com/photo-1542291026-7eec264c27ff?w=400&h=400&fit=crop' WHERE id = 6;
UPDATE products SET image_url = 'https://images.unsplash.com/photo-1587829741301-dc798b83add3?w=400&h=400&fit=crop' WHERE id = 7;
UPDATE products SET image_url = 'https://images.unsplash.com/photo-1514228742587-6b1558fcca3d?w=400&h=400&fit=crop' WHERE id = 8;

-- ─── Electronics (IDs 9-16) ───
INSERT INTO products (name, description, price, category, image_url) VALUES
('4K Ultra HD Smart TV 55"', 'Crystal-clear 4K display with built-in streaming apps and voice control', 499.99, 'Electronics', 'https://images.unsplash.com/photo-1593359677879-a4bb92f829d1?w=400&h=400&fit=crop'),
('Wireless Gaming Mouse', 'Ultra-lightweight 59g mouse with 25K DPI sensor and 70hr battery', 69.99, 'Electronics', 'https://images.unsplash.com/photo-1527864550417-7fd91fc51a46?w=400&h=400&fit=crop'),
('Portable Bluetooth Speaker', 'Waterproof IPX7 speaker with 360° sound and 20hr playtime', 44.99, 'Electronics', 'https://images.unsplash.com/photo-1608043152269-423dbba4e7e1?w=400&h=400&fit=crop'),
('Smartwatch Fitness Tracker', 'Heart rate, GPS, sleep tracking with 7-day battery life', 199.99, 'Electronics', 'https://images.unsplash.com/photo-1523275335684-37898b6baf30?w=400&h=400&fit=crop'),
('Noise Cancelling Earbuds', 'True wireless earbuds with ANC and transparency mode', 149.99, 'Electronics', 'https://images.unsplash.com/photo-1590658268037-6bf12f032f55?w=400&h=400&fit=crop'),
('Webcam 4K HDR', 'Auto-framing webcam with built-in ring light for video calls', 89.99, 'Electronics', 'https://images.unsplash.com/photo-1587826080692-f439cd0b70da?w=400&h=400&fit=crop'),
('Tablet 10.9 inch', 'Lightweight tablet with M2 chip, 256GB storage and all-day battery', 449.99, 'Electronics', 'https://images.unsplash.com/photo-1544244015-0df4b3ffc6b0?w=400&h=400&fit=crop'),
('External SSD 1TB', 'Portable NVMe SSD with 2000MB/s read speed, USB-C', 109.99, 'Electronics', 'https://images.unsplash.com/photo-1597872200969-2b65d56bd16b?w=400&h=400&fit=crop');

-- ─── Clothing (IDs 17-22) ───
INSERT INTO products (name, description, price, category, image_url) VALUES
('Denim Jacket Classic', 'Timeless medium-wash denim jacket with brass buttons', 59.99, 'Clothing', 'https://images.unsplash.com/photo-1576995853123-5a10305d93c0?w=400&h=400&fit=crop'),
('Slim Fit Chinos', 'Stretch cotton chinos in khaki, comfortable all-day wear', 39.99, 'Clothing', 'https://images.unsplash.com/photo-1473966968600-fa801b869a1a?w=400&h=400&fit=crop'),
('Wool Blend Sweater', 'Cozy crew-neck sweater in heather grey, machine washable', 54.99, 'Clothing', 'https://images.unsplash.com/photo-1620799140408-edc6dcb6d633?w=400&h=400&fit=crop'),
('Athletic Shorts', 'Quick-dry performance shorts with zippered pocket', 29.99, 'Clothing', 'https://images.unsplash.com/photo-1591195853828-11db59a44f6b?w=400&h=400&fit=crop'),
('Leather Belt', 'Full-grain leather belt with brushed nickel buckle', 34.99, 'Clothing', 'https://images.unsplash.com/photo-1553062407-98eeb64c6a62?w=400&h=400&fit=crop'),
('Polarized Sunglasses', 'UV400 protection aviator sunglasses with spring hinges', 27.99, 'Clothing', 'https://images.unsplash.com/photo-1572635196237-14b3f281503f?w=400&h=400&fit=crop');

-- ─── Home & Kitchen (IDs 23-28) ───
INSERT INTO products (name, description, price, category, image_url) VALUES
('Cast Iron Skillet 12"', 'Pre-seasoned cast iron pan, oven safe to 500°F', 39.99, 'Home & Kitchen', 'https://images.unsplash.com/photo-1585515320310-259814833e62?w=400&h=400&fit=crop'),
('French Press Coffee Maker', 'Borosilicate glass 34oz press with stainless steel filter', 24.99, 'Home & Kitchen', 'https://images.unsplash.com/photo-1572119865084-43c285814d63?w=400&h=400&fit=crop'),
('Bamboo Cutting Board Set', 'Set of 3 organic bamboo boards with juice grooves', 32.99, 'Home & Kitchen', 'https://images.unsplash.com/photo-1594226801341-41427b4e5c22?w=400&h=400&fit=crop'),
('Scented Candle Collection', 'Set of 3 soy wax candles: lavender, vanilla, cedar', 22.99, 'Home & Kitchen', 'https://images.unsplash.com/photo-1602028915047-37269d1a73f7?w=400&h=400&fit=crop'),
('Kitchen Knife Set', '5-piece German steel knife set with acacia wood block', 79.99, 'Home & Kitchen', 'https://images.unsplash.com/photo-1593618998160-e34014e67546?w=400&h=400&fit=crop'),
('Indoor Plant Pot Set', 'Set of 3 ceramic planters with drainage holes and saucers', 34.99, 'Home & Kitchen', 'https://images.unsplash.com/photo-1485955900006-10f4d324d411?w=400&h=400&fit=crop');

-- ─── Books (IDs 29-34) ───
INSERT INTO products (name, description, price, category, image_url) VALUES
('System Design Interview', 'Step-by-step framework for system design with real-world examples', 39.99, 'Books', 'https://images.unsplash.com/photo-1532012197267-da84d127e765?w=400&h=400&fit=crop'),
('Clean Code', 'A handbook of agile software craftsmanship by Robert C. Martin', 44.99, 'Books', 'https://images.unsplash.com/photo-1544716278-ca5e3f4abd8c?w=400&h=400&fit=crop'),
('The Pragmatic Programmer', '20th anniversary edition — your journey to mastery', 49.99, 'Books', 'https://images.unsplash.com/photo-1512820790803-83ca734da794?w=400&h=400&fit=crop'),
('Designing Data-Intensive Apps', 'The big ideas behind reliable, scalable systems by Martin Kleppmann', 54.99, 'Books', 'https://images.unsplash.com/photo-1524995997946-a1c2e315a42f?w=400&h=400&fit=crop'),
('Kubernetes in Action', 'Comprehensive guide to deploying containerized applications', 47.99, 'Books', 'https://images.unsplash.com/photo-1497633762265-9d179a990aa6?w=400&h=400&fit=crop'),
('Spring Boot in Practice', 'Hands-on recipes for building production-grade Spring apps', 42.99, 'Books', 'https://images.unsplash.com/photo-1543002588-bfa74002ed7e?w=400&h=400&fit=crop');

-- ─── Sports & Outdoors (IDs 35-39) ───
INSERT INTO products (name, description, price, category, image_url) VALUES
('Yoga Mat Premium', 'Non-slip 6mm thick TPE mat with carrying strap', 29.99, 'Sports & Outdoors', 'https://images.unsplash.com/photo-1601925260368-ae2f83cf8b7f?w=400&h=400&fit=crop'),
('Resistance Bands Set', '5 latex bands with handles, door anchor and carry bag', 19.99, 'Sports & Outdoors', 'https://images.unsplash.com/photo-1598289431512-b97b0917affc?w=400&h=400&fit=crop'),
('Hiking Backpack 40L', 'Waterproof daypack with hydration sleeve and rain cover', 64.99, 'Sports & Outdoors', 'https://images.unsplash.com/photo-1553062407-98eeb64c6a62?w=400&h=400&fit=crop'),
('Stainless Steel Thermos', 'Vacuum insulated 1L flask keeps hot 12hrs, cold 24hrs', 27.99, 'Sports & Outdoors', 'https://images.unsplash.com/photo-1602143407151-7111542de6e8?w=400&h=400&fit=crop'),
('Camping Hammock', 'Lightweight nylon hammock with tree straps, holds 400lbs', 34.99, 'Sports & Outdoors', 'https://images.unsplash.com/photo-1504280390367-361c6d9f38f4?w=400&h=400&fit=crop');

-- ─── Office & Stationery (IDs 40-44) ───
INSERT INTO products (name, description, price, category, image_url) VALUES
('Standing Desk Converter', 'Height-adjustable desk riser with keyboard tray, fits 2 monitors', 179.99, 'Office', 'https://images.unsplash.com/photo-1518455027359-f3f8164ba6bd?w=400&h=400&fit=crop'),
('Ergonomic Office Chair', 'Mesh back chair with lumbar support and adjustable armrests', 249.99, 'Office', 'https://images.unsplash.com/photo-1580480055273-228ff5388ef8?w=400&h=400&fit=crop'),
('Desk Organizer Bamboo', '5-compartment bamboo organizer for pens, phone and supplies', 24.99, 'Office', 'https://images.unsplash.com/photo-1544816155-12df9643f363?w=400&h=400&fit=crop'),
('LED Desk Lamp', 'Dimmable LED lamp with USB charging port and 5 color modes', 39.99, 'Office', 'https://images.unsplash.com/photo-1507473885765-e6ed057ab6fe?w=400&h=400&fit=crop'),
('Notebook Premium A5', 'Dotted 200-page journal with 100gsm paper and bookmark', 14.99, 'Office', 'https://images.unsplash.com/photo-1531346878377-a5be20888e57?w=400&h=400&fit=crop');

-- ─── Beauty & Personal Care (IDs 45-48) ───
INSERT INTO products (name, description, price, category, image_url) VALUES
('Beard Grooming Kit', 'Oil, balm, brush and comb set in a wooden gift box', 29.99, 'Beauty', 'https://images.unsplash.com/photo-1621607512214-68297480165e?w=400&h=400&fit=crop'),
('Essential Oils Set', '8 pure therapeutic-grade oils: lavender, tea tree, eucalyptus and more', 24.99, 'Beauty', 'https://images.unsplash.com/photo-1608571423902-eed4a5ad8108?w=400&h=400&fit=crop'),
('Bamboo Toothbrush Pack', 'Eco-friendly pack of 4 biodegradable bamboo toothbrushes', 9.99, 'Beauty', 'https://images.unsplash.com/photo-1607613009820-a29f7bb81c04?w=400&h=400&fit=crop'),
('Moisturizing Face Cream', 'Hydrating daily moisturizer with hyaluronic acid, 50ml', 18.99, 'Beauty', 'https://images.unsplash.com/photo-1556228578-0d85b1a4d571?w=400&h=400&fit=crop');

-- ─── Toys & Games (IDs 49-50) ───
INSERT INTO products (name, description, price, category, image_url) VALUES
('1000 Piece Jigsaw Puzzle', 'Scenic mountain landscape puzzle for adults, premium quality', 16.99, 'Toys & Games', 'https://images.unsplash.com/photo-1606503153255-59d8b8b82176?w=400&h=400&fit=crop'),
('Chess Set Wooden', 'Hand-carved walnut and maple chess set with folding board', 44.99, 'Toys & Games', 'https://images.unsplash.com/photo-1529699211952-734e80c4d42b?w=400&h=400&fit=crop');

-- ─── Inventory for all new products (IDs 9-50) ───
INSERT INTO inventory (product_id, quantity) VALUES
(9,  50),  (10, 200), (11, 180), (12, 120), (13, 160), (14, 90),  (15, 40),  (16, 130),
(17, 250), (18, 300), (19, 200), (20, 350), (21, 180), (22, 400),
(23, 100), (24, 150), (25, 120), (26, 200), (27, 80),  (28, 160),
(29, 90),  (30, 110), (31, 100), (32, 85),  (33, 95),  (34, 75),
(35, 220), (36, 300), (37, 140), (38, 170), (39, 190),
(40, 60),  (41, 45),  (42, 180), (43, 150), (44, 250),
(45, 130), (46, 200), (47, 350), (48, 160),
(49, 200), (50, 100);
