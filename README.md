# Merchant API Wrapper - Hash Verification Flow

Complete Spring Boot 3 (Java 17) implementation for Merchant → Sabbpe hash verification flow.

## Overview

This application provides a secure payment initiation API with hash-based authentication. It validates merchant credentials and transactions using SHA-256 hash verification.

## Architecture

```
Controller Layer (PaymentController)
    ↓
Service Layer (PaymentService)
    ↓
Repository Layer (MerchantRepository, TransactionRepository)
    ↓
Entity Layer (Merchant, Transaction)
```

## Technology Stack

- **Framework**: Spring Boot 3.2.0
- **Language**: Java 17
- **Database**: MySQL
- **ORM**: Spring Data JPA
- **Build Tool**: Maven
- **Lombok**: For reducing boilerplate code
- **Logging**: SLF4J

## Prerequisites

- Java 17 or higher
- MySQL 8.0 or higher
- Maven 3.6.0 or higher

## Project Setup

### 1. Database Configuration

Update `application.yml` with your MySQL credentials:

```yaml
spring:
  datasource:
    url: jdbc:mysql://localhost:3306/merchant_db
    username: root
    password: root
```

### 2. Create Database

```sql
CREATE DATABASE merchant_db;
```

### 3. Build Project

```bash
mvn clean install
```

### 4. Run Application

```bash
mvn spring-boot:run
```

The application will start on `http://localhost:8080`

## API Endpoints

### Payment Initiation

**Endpoint**: `POST /api/payment/initiate`

**Request Body**:
```json
{
  "merchantId": "M123",
  "orderId": "ORD1001",
  "amount": "1000.00",
  "hash": "generated_hash_value"
}
```

**Hash Generation Process**:

The client must generate SHA-256 hash using:
```
SHA-256(merchantId + orderId + amount + saltKey)
```

**Example Hash Calculation** (JavaScript):
```javascript
const crypto = require('crypto');

const merchantId = 'M123';
const orderId = 'ORD1001';
const amount = '1000.00';
const saltKey = 'secret_salt_key_123';

const hashInput = merchantId + orderId + amount + saltKey;
const hash = crypto.createHash('sha256').update(hashInput).digest('hex');
console.log(hash);
```

**Success Response** (200 OK):
```json
{
  "status": "SUCCESS",
  "internalToken": "550e8400-e29b-41d4-a716-446655440000",
  "orderId": "ORD1001"
}
```

**Error Responses**:

**401 Unauthorized** (Merchant not found or inactive):
```json
{
  "status": "FAILURE",
  "message": "Merchant not found for ID: M123",
  "errorCode": "MERCHANT_NOT_FOUND"
}
```

**400 Bad Request** (Hash mismatch):
```json
{
  "status": "FAILURE",
  "message": "Hash verification failed for order: ORD1001",
  "errorCode": "HASH_MISMATCH"
}
```

**400 Bad Request** (Validation error):
```json
{
  "status": "FAILURE",
  "errorCode": "VALIDATION_FAILED",
  "message": "Request validation failed",
  "errors": {
    "merchantId": "Merchant ID cannot be blank",
    "orderId": "Order ID cannot be blank",
    "amount": "Amount cannot be null",
    "hash": "Hash cannot be blank"
  }
}
```

## Verification Flow

### Step-by-Step Process

1. **Merchant Request**: Client sends payment initiation request with hash
2. **Merchant Validation**: Server fetches merchant from database
3. **Status Check**: Verify merchant is ACTIVE
4. **Hash Generation**: Server generates SHA-256 hash using:
   - merchantId
   - orderId
   - amount
   - saltKey (from database)
5. **Hash Verification**: Compare server-generated hash with client-provided hash
6. **Transaction Creation**: If hash matches, create transaction record with UUID token
7. **Response**: Return success response with internal token

## Database Schema

### Merchants Table

```sql
CREATE TABLE merchants (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    merchant_id VARCHAR(255) UNIQUE NOT NULL,
    merchant_name VARCHAR(255) NOT NULL,
    salt_key VARCHAR(255) NOT NULL,
    status ENUM('ACTIVE', 'INACTIVE') NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
);
```

### Transactions Table

```sql
CREATE TABLE transactions (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    order_id VARCHAR(255) NOT NULL,
    merchant_id VARCHAR(255) NOT NULL,
    amount DECIMAL(10, 2) NOT NULL,
    status ENUM('INITIATED', 'PROCESSING', 'COMPLETED', 'FAILED', 'CANCELLED') NOT NULL,
    internal_token VARCHAR(255) UNIQUE NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
);
```

## Key Features

### 1. Security
- SHA-256 hash verification
- Merchant salt key management
- Token-based transaction tracking
- No credentials exposed in responses

### 2. Validation
- Input validation using Jakarta Validation
- Null/blank field checks
- Type validation

### 3. Exception Handling
- Global exception handler
- Custom exceptions for specific scenarios
- Consistent error response format

### 4. Transaction Management
- @Transactional support
- ACID compliance
- Automatic rollback on errors

### 5. Logging
- Debug-level logging for verification steps
- Info-level logging for successful operations
- Error-level logging for failures

## File Structure

```
src/main/java/com/sabbpe/merchant/
├── MerchantApiWrapperApplication.java
├── controller/
│   └── PaymentController.java
├── service/
│   └── PaymentService.java
├── repository/
│   ├── MerchantRepository.java
│   └── TransactionRepository.java
├── entity/
│   ├── Merchant.java
│   ├── MerchantStatus.java
│   ├── Transaction.java
│   └── TransactionStatus.java
├── dto/
│   ├── PaymentInitiateRequest.java
│   └── PaymentResponse.java
├── util/
│   └── HashUtil.java
├── exception/
│   ├── GlobalExceptionHandler.java
│   ├── HashMismatchException.java
│   └── MerchantNotFoundException.java
└── config/
    └── DataInitializationConfig.java

src/main/resources/
└── application.yml
```

## Testing with cURL

### Generate Test Hash

```bash
# Using OpenSSL
echo -n "M123ORD10011000.00secret_salt_key_123" | sha256sum
# Output: 7f8e9c0abc456def...
```

### Test API Call

```bash
curl -X POST http://localhost:8080/api/payment/initiate \
  -H "Content-Type: application/json" \
  -d '{
    "merchantId": "M123",
    "orderId": "ORD1001",
    "amount": "1000.00",
    "hash": "7f8e9c0abc456def..."
  }'
```

## Logging

Logs are configured in application.yml:

```yaml
logging:
  level:
    com.sabbpe.merchant: DEBUG
```

Check console output for detailed logs:
- Hash verification steps
- Transaction creation
- Error details

## Best Practices Implemented

1. **Separation of Concerns**: Controller, Service, Repository layers
2. **Dependency Injection**: Autowired services
3. **Transaction Safety**: @Transactional on service methods
4. **Exception Handling**: Centralized error handling
5. **Input Validation**: @Valid annotation
6. **Logging**: Comprehensive logging at all levels
7. **Naming Conventions**: Clear, meaningful class and method names
8. **Documentation**: Javadoc comments on key methods

## Error Codes

| Error Code | HTTP Status | Description |
|-----------|------------|-------------|
| MERCHANT_NOT_FOUND | 401 | Merchant not found or inactive |
| HASH_MISMATCH | 400 | Hash verification failed |
| VALIDATION_FAILED | 400 | Request validation failed |
| INTERNAL_SERVER_ERROR | 500 | Unexpected server error |

## Future Enhancements

1. JWT token generation instead of UUID
2. Rate limiting
3. API versioning
4. Database encryption for salt keys
5. Audit logging
6. Multi-tenant support
7. Webhook notifications

## Support

For issues and questions, please contact the development team.
