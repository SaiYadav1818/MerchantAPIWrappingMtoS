# Payment Database Integration - Quick Reference

## One-Line Overview
Complete Spring Boot payment gateway integration with H2 database persistence, hash verification, and REST API for transaction status queries.

---

## File Locations

| Component | File Path | Purpose |
|-----------|-----------|---------|
| **Entity** | `entity/PaymentTransaction.java` | JPA entity with 50+ fields |
| **Repository** | `repository/PaymentTransactionRepository.java` | Data access layer |
| **Service** | `service/PaymentProcessingService.java` | Business logic & hash verification |
| **Webhook** | `controller/PaymentRedirectController.java` | Receive payment callbacks |
| **REST API** | `controller/PaymentStatusController.java` | Query transaction status |
| **DTO** | `dto/PaymentStatusResponse.java` | API response format |
| **Hash Util** | `util/EasebuzzHashUtil.java` | Hash generation utilities |
| **Config** | `config/EasebuzzConfig.java` | Easebuzz settings |
| **DB Config** | `resources/application.yml` | H2 database settings |

---

## Key Methods Flowchart

```
Payment Redirect Callback
    ↓
PaymentRedirectController.handlePaymentSuccess()
    ↓
PaymentProcessingService.processSuccessResponse()
    ├─ verifyPaymentHash() → true/false
    ├─ findByTxnid() → optional existing transaction
    ├─ enrichTransactionFromResponse() → set all fields
    ├─ storeRawResponse() → JSON serialization
    └─ repository.save() → persist to H2
    ↓
Return PaymentTransaction (saved to DB with ID)
    ↓
PaymentRedirectController renders payment-success.html
```

---

## Database Queries

### Query transactions by status
```sql
SELECT * FROM payment_transactions 
WHERE status = 'SUCCESS'
ORDER BY created_at DESC;
```

### Find by transaction ID
```sql
SELECT * FROM payment_transactions 
WHERE txnid = 'TXN123456';
```

### Find by merchant (UDF1)
```sql
SELECT * FROM payment_transactions 
WHERE udf1 = 'MERCHANT123';
```

### Find by order (UDF2)
```sql
SELECT * FROM payment_transactions 
WHERE udf2 = 'ORDER456';
```

### Find tampered payments (hash failed)
```sql
SELECT * FROM payment_transactions 
WHERE hash_verified = false;
```

### Statistics
```sql
SELECT status, COUNT(*) as count 
FROM payment_transactions 
GROUP BY status;
```

---

## API Endpoints

### Get Transaction Details
```
GET /api/payment/status/{txnid}

Response: 200 OK with complete transaction data
{
  "txnid": "TXN123",
  "status": "SUCCESS",
  "amount": 1000.00,
  "hash_verified": true,
  "udf1": "MERCHANT123",
  "udf2": "ORDER456",
  ...
}
```

### Check Transaction Exists
```
GET /api/payment/exists/{txnid}

Response: 200 OK
{
  "txnid": "TXN123",
  "exists": true
}
```

### Check Hash Status
```
GET /api/payment/hash-valid/{txnid}

Response: 200 OK
{
  "txnid": "TXN123",
  "hash_verified": true,
  "status": "SUCCESS"
}
```

---

## Service Methods

### Process Payment Response
```java
// In PaymentRedirectController
PaymentTransaction transaction = 
  paymentProcessingService.processSuccessResponse(requestParams);

// transaction will have:
// - ID (auto-generated)
// - All fields extracted from requestParams
// - Hash verification result
// - Created/Updated timestamps
// - Raw response as JSON
```

### Query Payment
```java
// In your code
Optional<PaymentTransaction> transaction = 
  paymentProcessingService.getTransaction("TXN123");

if (transaction.isPresent()) {
  PaymentTransaction tx = transaction.get();
  System.out.println("Amount: $" + tx.getAmount());
  System.out.println("Status: " + tx.getStatus());
  System.out.println("Hash Valid: " + tx.isHashValid());
  System.out.println("Merchant: " + tx.getUDFValue(1));  // UDF1
  System.out.println("Order: " + tx.getUDFValue(2));     // UDF2
}
```

### Get Successful Transactions for Merchant
```java
List<PaymentTransaction> transactions = 
  paymentProcessingService.getMerchantTransactions("MERCHANT123");
```

### Find Suspicious Transactions (Hash Mismatch)
```java
List<PaymentTransaction> suspicious = 
  paymentProcessingService.getHashVerificationFailures();

for (PaymentTransaction tx : suspicious) {
  System.out.println("Suspicious: " + tx.getTxnid());
  // Review and take action
}
```

---

## Configuration

### Check H2 Database Configuration
```yaml
# src/main/resources/application.yml

spring:
  datasource:
    url: jdbc:h2:mem:testdb
    username: sa
    password:
  h2:
    console:
      enabled: true
      path: /h2-console
  jpa:
    hibernate:
      ddl-auto: create
    show-sql: true
```

### Access H2 Web Console
1. Start application: `mvn spring-boot:run`
2. Open Browser: `http://localhost:8080/h2-console`
3. Enter credentials:
   - Driver: `org.h2.Driver`
   - URL: `jdbc:h2:mem:testdb`
   - User: `sa`
   - Password: (leave empty)

---

## Hash Verification

### How It Works

1. **Receive** hash from gateway
2. **Extract** fields: status, udf1-10, email, firstname, productinfo, amount, txnid
3. **Build** string: `salt|status|udf10|...|udf1|email|firstname|productinfo|amount|txnid|key`
4. **Calculate** SHA-512 of string
5. **Compare** with received hash
6. **Save** result in `hashVerified` field

### Check Hash in Code
```java
PaymentTransaction tx = transaction.get();

if (tx.isHashValid()) {
  System.out.println("✓ Payment is authentic");
} else {
  System.out.println("✗ Possible tampering detected!");
  // Take security action
}
```

---

## UDF Fields

### Set UDF Values (10 Custom Fields)
```java
transaction.setUDFValue(1, "MERCHANT123");   // UDF1 = Merchant ID
transaction.setUDFValue(2, "ORDER456");      // UDF2 = Order ID
transaction.setUDFValue(3, "ITEM_SKU");      // UDF3 = custom
transaction.setUDFValue(4, "CATEGORY");      // UDF4 = custom
// ... up to UDF10
```

### Get UDF Values
```java
String merchantId = transaction.getUDFValue(1);   // Get UDF1
String orderId = transaction.getUDFValue(2);      // Get UDF2
```

### Query by UDF
```java
// Find transactions by merchant (UDF1)
List<PaymentTransaction> transactions = 
  paymentTransactionRepository.findByUdf1("MERCHANT123");

// Find transactions by order (UDF2)
List<PaymentTransaction> byOrder = 
  paymentTransactionRepository.findByUdf2("ORDER456");
```

---

## Error Handling

### Missing Transaction ID
```
Error: 400 BAD REQUEST
Message: Transaction ID (txnid) is required
Action: Check if gateway sent txnid parameter
```

### Transaction Not Found
```
Error: 404 NOT FOUND  
Message: Transaction not found for this ID
Action: Query database, verify txnid is correct
```

### Database Error
```
Error: 500 INTERNAL SERVER ERROR
Message: Failed to retrieve transaction
Action: Check database connection, review logs
```

### Hash Verification Failed
```
Status: 200 (still saves)
Error Message: Hash verification failed - Payment may be tampered
Action: Review transaction, don't process, contact gateway
```

---

## Testing Checklist

- [ ] Application starts without errors
- [ ] H2 console accessible at http://localhost:8080/h2-console
- [ ] POST /payment/success creates database record
- [ ] POST /payment/failure creates database record
- [ ] GET /api/payment/status/{txnid} returns 200 with data
- [ ] GET /api/payment/status/invalid returns 404
- [ ] Hash verification marks tampered payments correctly
- [ ] Duplicate callbacks don't create duplicate records
- [ ] All UDF fields stored in database
- [ ] Raw response JSON stored in CLOB field
- [ ] Timestamps automatically set (created_at, updated_at)
- [ ] Query by status returns correct transactions

---

## Common Scenarios

### Scenario 1: Valid Payment Received
```
1. Gateway POSTs to /payment/success with all parameters
2. PaymentProcessingService verifies hash ✓
3. Transaction saved with status=SUCCESS, hash_verified=true
4. User sees "Payment Successful" page
5. Database contains complete payment data
```

### Scenario 2: Duplicate Callback
```
1. Gateway sends same callback again
2. processSuccessResponse() called
3. findByTxnid() finds existing transaction
4. Updates existing record instead of creating new
5. No duplicate records created
6. updated_at timestamp changes, created_at remains same
```

### Scenario 3: Hash Verification Fails
```
1. Callback received with modified parameters
2. Calculated hash != received hash
3. hashVerified set to false
4. Transaction still saved (not rejected)
5. User sees warning: "Possible tampering detected"
6. Admin reviews for security breach
```

### Scenario 4: Query Payment Status
```
1. Merchant queries: GET /api/payment/status/TXN123
2. Service looks up transaction by txnid
3. Returns complete transaction data as JSON
4. Includes all fields, UDFs, timestamps, raw response
5. Frontend can display or process data
```

---

## Important Fields Reference

| Field | Type | Purpose | Example |
|-------|------|---------|---------|
| `txnid` | String | Unique transaction ID | TXN123456 |
| `status` | String | SUCCESS, FAILED, HASH_MISMATCH | SUCCESS |
| `amount` | BigDecimal | Payment amount | 1000.00 |
| `email` | String | Customer email | user@example.com |
| `firstname` | String | Customer name | John Doe |
| `hash` | String | Gateway hash signature | abcd...efgh |
| `hashVerified` | Boolean | Hash verification result | true |
| `udf1-10` | String | Custom merchant fields | MERCHANT123 |
| `rawResponse` | CLOB | Complete JSON response | {...} |
| `createdAt` | LocalDateTime | Record creation time | 2026-02-18 09:55:20 |
| `updatedAt` | LocalDateTime | Last update time | 2026-02-18 09:55:20 |

---

## Debug Tips

### View All Logs
```bash
# In IDE console during mvn spring-boot:run
# Look for: PaymentProcessingService and PaymentRedirectController logs
```

### Check Database via SQL
```sql
-- Quick check: how many transactions?
SELECT COUNT(*) FROM payment_transactions;

-- Check latest transaction
SELECT * FROM payment_transactions 
ORDER BY created_at DESC LIMIT 1;

-- Find specific txnid
SELECT * FROM payment_transactions WHERE txnid = 'TXN123';
```

### Test Hash Verification
```java
// Debug: Extract both hashes
String receivedHash = paymentResponse.get("hash");
String calculatedHash = EasebuzzHashUtil.generateReverseHashWithUDF(
  salt, status, udf10, ..., udf1, email, firstname, productinfo, amount, txnid, key);

System.out.println("Received:   " + receivedHash.substring(0, 10) + "...");
System.out.println("Calculated: " + calculatedHash.substring(0, 10) + "...");
System.out.println("Match: " + receivedHash.equalsIgnoreCase(calculatedHash));
```

---

## Performance Considerations

- H2 in-memory database: < 100ms per query
- Idempotent check (findByTxnid): O(1) with unique index
- Hash verification: < 10ms for SHA-512
- JSON serialization: < 5ms for typical response
- Database save: < 20ms with auto-increment ID

---

## Next Steps After Deployment

1. **Monitor** hash verification failures daily
2. **Backup** H2 database periodically
3. **Archive** old transactions to reduce database size
4. **Alert** on suspicious transactions (hash_verified=false)
5. **Track** payment statistics by status
6. **Handle** webhook retries gracefully (already idempotent)

