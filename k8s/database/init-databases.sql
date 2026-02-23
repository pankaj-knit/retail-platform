-- =============================================================================
-- init-databases.sql
-- =============================================================================
-- This script runs ONCE when the Postgres container starts for the first time.
-- It creates 4 separate databases inside the single Postgres instance.
--
-- How it works:
-- The official Postgres Docker image looks for .sql files in
-- /docker-entrypoint-initdb.d/ and executes them on first boot.
-- We mount this file there via a Kubernetes ConfigMap.
--
-- Each microservice connects to its OWN database using a distinct JDBC URL:
--   - User Service    -> jdbc:postgresql://postgres:5432/user_db
--   - Order Service   -> jdbc:postgresql://postgres:5432/order_db
--   - Payment Service -> jdbc:postgresql://postgres:5432/payment_db
--   - Inventory Svc   -> jdbc:postgresql://postgres:5432/inventory_db
--
-- This gives us logical isolation: each service can only see its own tables.
-- =============================================================================

-- Create the 4 databases
CREATE DATABASE user_db;
CREATE DATABASE order_db;
CREATE DATABASE payment_db;
CREATE DATABASE inventory_db;

-- Grant full privileges to the default user (retail_user) on each database.
-- In production, you'd create separate users per service for true access isolation.
GRANT ALL PRIVILEGES ON DATABASE user_db TO retail_user;
GRANT ALL PRIVILEGES ON DATABASE order_db TO retail_user;
GRANT ALL PRIVILEGES ON DATABASE payment_db TO retail_user;
GRANT ALL PRIVILEGES ON DATABASE inventory_db TO retail_user;
