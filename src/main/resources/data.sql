-- Sample data insertion for H2 in-memory database
-- Tables are created automatically by Hibernate DDL-AUTO=update

-- Insert Sample Merchant Data
-- IMPORTANT: Use valid Easebuzz test merchant key and salt
-- The merchant_key and salt_key must be registered with Easebuzz's test environment
-- For testing, use the key/salt provided by Easebuzz or obtain from their dashboard
INSERT INTO merchants (merchant_id, merchant_name, salt_key, status, created_at, updated_at) 
VALUES ('M123', 'Test Merchant', '5Y7D8PF1GZ', 'ACTIVE', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP);

-- Additional test merchants
INSERT INTO merchants (merchant_id, merchant_name, salt_key, status, created_at, updated_at) 
VALUES ('M456', 'Production Merchant', 'prod_salt_key_456', 'ACTIVE', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP);

INSERT INTO merchants (merchant_id, merchant_name, salt_key, status, created_at, updated_at) 
VALUES ('M789', 'Inactive Merchant', 'inactive_salt_key_789', 'INACTIVE', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP);
