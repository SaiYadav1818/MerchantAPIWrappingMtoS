-- Sample data insertion for H2 in-memory database
-- Tables are created automatically by Hibernate DDL-AUTO=update

-- Insert Sample Merchant Data
INSERT INTO merchants (merchant_id, merchant_name, salt_key, status, created_at, updated_at) 
VALUES ('M123', 'Test Merchant', 'secret_salt_key_123', 'ACTIVE', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP);

-- Additional test merchants
INSERT INTO merchants (merchant_id, merchant_name, salt_key, status, created_at, updated_at) 
VALUES ('M456', 'Production Merchant', 'prod_salt_key_456', 'ACTIVE', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP);

INSERT INTO merchants (merchant_id, merchant_name, salt_key, status, created_at, updated_at) 
VALUES ('M789', 'Inactive Merchant', 'inactive_salt_key_789', 'INACTIVE', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP);
