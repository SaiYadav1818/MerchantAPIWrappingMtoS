# Merchant API Wrapper - Complete API Reference

## Base URL
```
http://localhost:8080
```

## API Endpoints Summary

| Method | Endpoint | Description |
|--------|----------|-------------|
| POST | /api/payment/initiate | Initiate a payment transaction |
| POST | /api/payment/generate-hash | Generate payment hash |
| POST | /api/payment/payment-url | Get payment URL from gateway |
| POST | /api/payment/easebuzz/initiate | Direct Easebuzz payment initiation |
| POST | /api/refund | Process refund |
| GET | /api/payment/status/{token} | Get payment status |
| POST | /api/payment/callback | Payment callback handler |

---

## 1. Initiate Payment

### Endpoint
```
POST /api/payment/initiate
```

### Request Headers
```
Content-Type: application/json
```

### Request Body
```
json
{
  "merchantId": "M123",
  "orderId": "ORD001",
  "amount": "1000.00",
  "hash": "a1b2c3d4e5f6..."
}
```

### Request Fields
| Field | Type | Required | Description |
|-------|------|----------|-------------|
| merchantId | String | Yes | Unique merchant identifier |
| orderId | String | Yes | Unique order ID |
| amount | String | Yes | Payment amount (decimal format) |
| hash | String | Yes | SHA-256 hash (64 characters) |

### Success Response (200 OK)
```
json
{
  "status": "SUCCESS",
  "internalToken": "550e8400-e29b-41d4-a716-446655440000",
  "orderId": "ORD001"
}
```

### Error Response (400 Bad Request)
```
json
{
  "status": "FAILURE",
  "message": "Hash verification failed for order: ORD001",
  "errorCode": "HASH_MISMATCH"
}
```

### Example cURL
```
bash
curl -X POST http://localhost:8080/api/payment/initiate \
  -H "Content-Type: application/json" \
  -d '{
    "merchantId": "M123",
    "orderId": "ORD001",
    "amount": "1000.00",
    "hash": "a1b2c3d4e5f6a1b2c3d4e5f6a1b2c3d4e5f6a1b2c3d4e5f6a1b2c3d4e5f6"
  }'
```

---

## 2. Generate Hash

### Endpoint
```
POST /api/payment/generate-hash
```

### Request Headers
```
Content-Type: application/json
```

### Request Body
```
json
{
  "merchantId": "M123",
  "orderId": "ORD001",
  "amount": "1000.00"
}
```

### Request Fields
| Field | Type | Required | Description |
|-------|------|----------|-------------|
| merchantId | String | Yes | Unique merchant identifier |
| orderId | String | Yes | Unique order ID |
| amount | String | Yes | Payment amount |

### Success Response (200 OK)
```
json
{
  "hash": "a1b2c3d4e5f6...",
  "merchantId": "M123",
  "orderId": "ORD001",
  "amount": "1000.00"
}
```

### Example cURL
```
bash
curl -X POST http://localhost:8080/api/payment/generate-hash \
  -H "Content-Type: application/json" \
  -d '{
    "merchantId": "M123",
    "orderId": "ORD001",
    "amount": "1000.00"
  }'
```

---

## 3. Get Payment URL

### Endpoint
```
POST /api/payment/payment-url
```

### Request Headers
```
Content-Type: application/json
```

### Request Body
```
json
{
  "internalToken": "550e8400-e29b-41d4-a716-446655440000",
  "productInfo": "Product Name",
  "firstName": "John",
  "email": "john@example.com",
  "phone": "9999999999"
}
```

### Request Fields
| Field | Type | Required | Description |
|-------|------|----------|-------------|
| internalToken | String | Yes | Token from payment initiation |
| productInfo | String | Yes | Product description |
| firstName | String | Yes | Customer first name |
| email | String | Yes | Customer email |
| phone | String | Yes | Customer phone number |

### Success Response (200 OK)
```
json
{
  "status": "SUCCESS",
  "paymentUrl": "https://easbuzz.in/pay/abc123def456",
  "txnId": "TXN2023123456789",
  "orderId": "ORD001",
  "message": "Payment URL generated successfully",
  "timestamp": 1702745600000
}
```

### Error Response (500 Internal Server Error)
```
json
{
  "status": "FAILURE",
  "paymentUrl": null,
  "txnId": null,
  "orderId": "ORD001",
  "message": "Invalid internal token",
  "errorCode": "INVALID_TOKEN",
  "timestamp": 1702745600000
}
```

### Example cURL
```
bash
curl -X POST http://localhost:8080/api/payment/payment-url \
  -H "Content-Type: application/json" \
  -d '{
    "internalToken": "550e8400-e29b-41d4-a716-446655440000",
    "productInfo": "Premium Subscription",
    "firstName": "John Doe",
    "email": "john.doe@example.com",
    "phone": "9876543210"
  }'
```

---

## 4. Easebuzz Direct Payment Initiation

### Endpoint
```
POST /api/payment/easebuzz/initiate
```

### Request Headers
```
Content-Type: application/json
```

### Request Body
```
json
{
  "txnid": "TXN2023123456789",
  "amount": "1000.00",
  "productinfo": "Premium Subscription",
  "firstname": "John Doe",
  "email": "john.doe@example.com",
  "phone": "9876543210",
  "surl": "https://example.com/success",
  "furl": "https://example.com/failure",
  "udf1": "custom1",
  "udf2": "custom2",
  "udf3": "custom3",
  "udf4": "custom4",
  "udf5": "custom5"
}
```

### Request Fields
| Field | Type | Required | Description |
|-------|------|----------|-------------|
| txnid | String | Yes | Transaction ID |
| amount | BigDecimal | Yes | Payment amount |
| productinfo | String | Yes | Product information |
| firstname | String | Yes | Customer first name |
| email | String | Yes | Customer email |
| phone | String | Yes | Customer phone |
| surl | String | Yes | Success URL |
| furl | String | Yes | Failure URL |
| udf1-udf10 | String | No | User defined fields |

### Success Response (200 OK)
```
json
{
  "status": "SUCCESS",
  "message": "Payment initiated successfully",
  "data": "https://easbuzz.in/pay/abc123def456",
  "txnId": "TXN2023123456789"
}
```

### Error Response
```
json
{
  "status": "FAILURE",
  "message": "Payment initiation failed",
  "error_code": "E001",
  "data": null,
  "txnId": null
}
```

### Example cURL
```
bash
curl -X POST http://localhost:8080/api/payment/easebuzz/initiate \
  -H "Content-Type: application/json" \
  -d '{
    "txnid": "TXN2023123456789",
    "amount": "1000.00",
    "productinfo": "Premium Subscription",
    "firstname": "John Doe",
    "email": "john.doe@example.com",
    "phone": "9876543210",
    "surl": "https://example.com/success",
    "furl": "https://example.com/failure",
    "udf1": "custom1",
    "udf2": "custom2"
  }'
```

---

## 5. Payment Status

### Endpoint
```
GET /api/payment/status/{token}
```

### Path Parameters
| Parameter | Type | Description |
|-----------|------|-------------|
| token | String | Internal payment token |

### Success Response (200 OK)
```
json
{
  "status": "SUCCESS",
  "paymentStatus": "SUCCESS",
  "txnId": "TXN2023123456789",
  "orderId": "ORD001",
  "amount": "1000.00",
  "message": "Payment successful"
}
```

### Example cURL
```
bash
curl -X GET http://localhost:8080/api/payment/status/550e8400-e29b-41d4-a716-446655440000
```

---

## 6. Process Refund

### Endpoint
```
POST /api/refund
```

### Request Headers
```
Content-Type: application/json
```

### Request Body
```
json
{
  "txnId": "TXN2023123456789",
  "refundAmount": "500.00",
  "refundType": "PARTIAL"
}
```

### Request Fields
| Field | Type | Required | Description |
|-------|------|----------|-------------|
| txnId | String | Yes | Original transaction ID |
| refundAmount | String | Yes | Amount to refund |
| refundType | String | Yes | FULL or PARTIAL |

### Success Response (200 OK)
```
json
{
  "status": "SUCCESS",
  "refundId": "REF2023123456789",
  "txnId": "TXN2023123456789",
  "refundAmount": "500.00",
  "message": "Refund processed successfully"
}
```

### Example cURL
```
bash
curl -X POST http://localhost:8080/api/refund \
  -H "Content-Type: application/json" \
  -d '{
    "txnId": "TXN2023123456789",
    "refundAmount": "500.00",
    "refundType": "PARTIAL"
  }'
```

---

## 7. Payment Callback

### Endpoint
```
POST /api/payment/callback
```

### Request Body (Easebuzz Callback)
```
json
{
  "txnid": "TXN2023123456789",
  "amount": "1000.00",
  "productinfo": "Premium Subscription",
  "firstname": "John Doe",
  "email": "john.doe@example.com",
  "phone": "9876543210",
  "udf1": "custom1",
  "udf2": "custom2",
  "udf3": "custom3",
  "udf4": "custom4",
  "udf5": "custom5",
  "hash": "abc123...",
  "status": "success"
}
```

### Response
```
json
{
  "status": "success"
}
```

---

## Complete Payment Flow Example

### Step 1: Generate Hash
```
bash
# Request
curl -X POST http://localhost:8080/api/payment/generate-hash \
  -H "Content-Type: application/json" \
  -d '{
    "merchantId": "M123",
    "orderId": "ORD001",
    "amount": "1000.00"
  }'

# Response
{
  "hash": "a1b2c3d4e5f6...",
  "merchantId": "M123",
  "orderId": "ORD001",
  "amount": "1000.00"
}
```

### Step 2: Initiate Payment
```
bash
# Request
curl -X POST http://localhost:8080/api/payment/initiate \
  -H "Content-Type: application/json" \
  -d '{
    "merchantId": "M123",
    "orderId": "ORD001",
    "amount": "1000.00",
    "hash": "a1b2c3d4e5f6..."
  }'

# Response
{
  "status": "SUCCESS",
  "internalToken": "550e8400-e29b-41d4-a716-446655440000",
  "orderId": "ORD001"
}
```

### Step 3: Get Payment URL
```bash
# Request
curl -X POST http://localhost:8080/api/payment/payment-url \
  -H "Content-Type: application/json" \
  -d '{
    "internalToken": "550e8400-e29b-41d4-a716-446655440000",
    "productInfo": "Premium Subscription",
    "firstName": "John Doe",
    "email": "john.doe@example.com",
    "phone": "9876543210"
  }'

# Response
{
  "status": "SUCCESS",
  "paymentUrl": "https://easbuzz.in/pay/abc123def456",
  "txnId": "TXN2023123456789",
  "orderId": "ORD001",
  "message": "Payment URL generated successfully",
  "timestamp": 1702745600000
}
```

---

## Error Codes

| Error Code | Description |
|------------|-------------|
| MERCHANT_NOT_FOUND | Merchant ID not found or inactive |
| HASH_MISMATCH | Hash verification failed |
| VALIDATION_FAILED | Request validation failed |
| INVALID_TOKEN | Invalid or expired payment token |
| GATEWAY_ERROR | Payment gateway error |
| INTERNAL_SERVER_ERROR | Unexpected server error |

---

## Testing with Postman

### Environment Variables
```
base_url: http://localhost:8080
```

### Collection Import
Import the following JSON into Postman:

```
json
{
  "info": {
    "name": "Merchant API Wrapper",
    "description": "API for payment processing"
  },
  "item": [
    {
      "name": "Initiate Payment",
      "request": {
        "method": "POST",
        "url": "{{base_url}}/api/payment/initiate",
        "header": [
          {"key": "Content-Type", "value": "application/json"}
        ],
        "body": {
          "mode": "raw",
          "raw": "{\n  \"merchantId\": \"M123\",\n  \"orderId\": \"ORD001\",\n  \"amount\": \"1000.00\",\n  \"hash\": \"a1b2c3d4e5f6a1b2c3d4e5f6a1b2c3d4e5f6a1b2c3d4e5f6a1b2c3d4e5f6\"\n}"
        }
      }
    },
    {
      "name": "Generate Hash",
      "request": {
        "method": "POST",
        "url": "{{base_url}}/api/payment/generate-hash",
        "header": [
          {"key": "Content-Type", "value": "application/json"}
        ],
        "body": {
          "mode": "raw",
          "raw": "{\n  \"merchantId\": \"M123\",\n  \"orderId\": \"ORD001\",\n  \"amount\": \"1000.00\"\n}"
        }
      }
    },
    {
      "name": "Get Payment URL",
      "request": {
        "method": "POST",
        "url": "{{base_url}}/api/payment/payment-url",
        "header": [
          {"key": "Content-Type", "value": "application/json"}
        ],
        "body": {
          "mode": "raw",
          "raw": "{\n  \"internalToken\": \"550e8400-e29b-41d4-a716-446655440000\",\n  \"productInfo\": \"Premium Subscription\",\n  \"firstName\": \"John Doe\",\n  \"email\": \"john.doe@example.com\",\n  \"phone\": \"9876543210\"\n}"
        }
      }
    }
  ]
}
```

---

## Base URL Configuration

The base URL can be configured in `application.yml`:

```
yaml
server:
  port: 8080

easebuzz:
  url:
    initiate: https://ebiz.test.in/papi/_initiateButton
    callback: https://ebiz.test.in/papi/_callback
```

For production, update the URLs to:
```
yaml
easebuzz:
  url:
    initiate: https://ebiz.com/papi/_initiateButton
    callback: https://ebiz.com/papi/_callback
