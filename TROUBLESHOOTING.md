# Troubleshooting & FAQs

## Common Issues & Solutions

### 1. Build Error: "Cannot find symbol - method generateReverseHashWithUDF"

**Error Message**:
```
[ERROR] PaymentProcessingService.java:[208,53] cannot find symbol
symbol: method generateReverseHashWithUDF(...)
```

**Cause**: EasebuzzHashUtil doesn't have the method yet

**Solution**:
1. Check if `generateReverseHashWithUDF()` exists in `EasebuzzHashUtil.java`
2. If missing, add the method:
   ```java
   public static String generateReverseHashWithUDF(
       String salt, String status,
       String udf10, String udf9, String udf8, String udf7, String udf6,
       String udf5, String udf4, String udf3, String udf2, String udf1,
       String email, String firstname, String productinfo,
       String amount, String txnid, String key) {
       
       String hashString = salt + "|" + status + "|" + udf10 + "|" + udf9 + "|" + udf8 + "|" + 
                          udf7 + "|" + udf6 + "|" + udf5 + "|" + udf4 + "|" + udf3 + "|" + 
                          udf2 + "|" + udf1 + "|" + email + "|" + firstname + "|" + 
                          productinfo + "|" + amount + "|" + txnid + "|" + key;
       
       return generateSHA512(hashString);
   }
   ```
3. Run: `mvn clean compile -DskipTests`

---

### 2. No Data in H2 Database

**Problem**: POST /payment/success works, but no record in database

**Causes & Solutions**:

| Cause | Check | Solution |
|-------|-------|----------|
| **DDL-Auto Disabled** | `spring.jpa.hibernate.ddl-auto` in config | Set to `create` or `update` |
| **Wrong URL** | H2 JDBC URL mismatch | Ensure URL: `jdbc:h2:mem:testdb` |
| **JPA Disabled** | Missing JPA dependency | Check `pom.xml` has `spring-boot-starter-data-jpa` |
| **Entity Not Mapped** | @Entity annotation | Verify `PaymentTransaction.java` has `@Entity` |
| **Controller Error** | Check logs | Look for exceptions in console output |
| **Service Not Called** | Review code | Verify `PaymentRedirectController` injects `PaymentProcessingService` |

**Debug Steps**:
1. Check logs for "Hibernate: CREATE TABLE" messages
2. Access H2 console: http://localhost:8080/h2-console
3. Run: `SELECT * FROM INFORMATION_SCHEMA.TABLES;` to list tables
4. Check if PAYMENT_TRANSACTIONS table exists

---

### 3. H2 Console Not Accessible

**Problem**: http://localhost:8080/h2-console returns 404

**Solution**:
1. Verify configuration:
   ```yaml
   spring:
     h2:
       console:
         enabled: true
         path: /h2-console
   ```

2. Check if started: Look for log message: "H2 console available at '/h2-console'"

3. Test connection details:
   - Driver: `org.h2.Driver`
   - URL: `jdbc:h2:mem:testdb`
   - User: `sa`
   - Password: (leave empty)

---

### 4. Hash Verification Always Fails

**Problem**: `hash_verified = false` for valid payments

**Causes & Solutions**:

| Cause | Debug | Solution |
|-------|-------|----------|
| **Wrong Salt** | Check config value | Verify `easebuzz.salt` matches gateway |
| **Wrong Key** | Check config value | Verify `easebuzz.key` matches gateway |
| **Missing Fields** | Log both hashes | Check all 18 fields extracted correctly |
| **Field Order Wrong** | Review formula | Order must be: udf10|udf9|...|udf1| (reverse order) |
| **NULL Values** | Inspect parameters | Convert null to empty string in hash string |
| **Case Sensitivity** | Use equalsIgnoreCase() | Already handled in code, but verify |

**Debug Code**:
```java
// Add to PaymentProcessingService.verifyPaymentHash()
String salt = "your_salt";
String key = "your_key";

System.out.println("======= HASH DEBUG =======");
System.out.println("Received Hash: " + paymentResponse.get("hash"));
System.out.println("Status: " + paymentResponse.get("status"));
System.out.println("TxnID: " + paymentResponse.get("txnid"));
System.out.println("Amount: " + paymentResponse.get("amount"));
System.out.println("Email: " + paymentResponse.get("email"));
System.out.println("UDF1: " + paymentResponse.get("udf1"));
// ... check all fields

System.out.println("Building hash string:");
String hashString = salt + "|" + status + "|" + udf10 + "..." + key;
System.out.println("Hash String: " + hashString);

String calculatedHash = EasebuzzHashUtil.generateSHA512(hashString);
System.out.println("Calculated Hash: " + calculatedHash);
System.out.println("Match: " + calculatedHash.equalsIgnoreCase(receivedHash));
```

---

### 5. Duplicate Records in Database

**Problem**: Same payment appears twice after callback retry

**Cause**: Duplicate handling not working

**Solution**:
1. Verify repository method exists:
   ```java
   Optional<PaymentTransaction> findByTxnid(String txnid)
   ```

2. Verify service uses it:
   ```java
   Optional<PaymentTransaction> existing = repository.findByTxnid(txnid);
   if (existing.isPresent()) {
       // Update existing record
       transaction = existing.get();
   } else {
       // Create new record
       transaction = new PaymentTransaction();
   }
   ```

3. Check database for duplicates:
   ```sql
   SELECT txnid, COUNT(*) as count 
   FROM payment_transactions 
   GROUP BY txnid 
   HAVING COUNT(*) > 1;
   ```

---

### 6. Transaction ID (txnid) Not Extracted

**Problem**: Callback received but txnid field empty in database

**Causes & Solutions**:

| Cause | Check | Solution |
|-------|-------|----------|
| **Parameter Named Wrong** | Gateway sends `txn_id` instead of `txnid` | Check gateway docs for parameter name |
| **Missing in Request** | Browser dev tools → Network → Request | Verify parameter exists in POST data |
| **Case Sensitive** | Java code looks for "txnid" | Might be "txnId" or "TXN_ID" |
| **Empty Value** | Check if value present but empty | Add validation |

**Debug**:
```java
// In PaymentRedirectController.handlePaymentSuccess()
System.out.println("All parameters:");
requestParams.forEach((k, v) -> System.out.println(k + " = " + v));

String txnid = requestParams.get("txnid");
System.out.println("Extracted txnid: " + txnid);
```

---

### 7. Raw Response JSON Not Stored

**Problem**: `rawResponse` field is empty or `{}`

**Causes & Solutions**:

| Cause | Solution |
|-------|----------|
| **JSON Conversion Error** | Check ObjectMapper configuration |
| **Large Response** | CLOB might be truncated, increase size if needed |
| **Null Values** | Null values filtered before serialization (expected) |
| **ObjectMapper Not Autowired** | Add `@Autowired private ObjectMapper objectMapper;` |

**Test**:
```java
// In service method, add debug log:
System.out.println("Raw Response before save: " + transaction.getRawResponse());
```

---

### 8. Timestamps Not Set Automatically

**Problem**: `created_at` and `updated_at` are NULL in database

**Solution**:
1. Verify entity has lifecycle hooks:
   ```java
   @PrePersist
   public void setCreatedAtAndUpdatedAt() {
       this.createdAt = LocalDateTime.now();
       this.updatedAt = LocalDateTime.now();
   }
   
   @PreUpdate
   public void setUpdatedAt() {
       this.updatedAt = LocalDateTime.now();
   }
   ```

2. Check column definitions:
   ```java
   @Column(nullable = false, updatable = false)
   private LocalDateTime createdAt;
   
   @Column(nullable = false)
   private LocalDateTime updatedAt;
   ```

---

### 9. API Endpoint Returns 404 for Valid txnid

**Problem**: GET /api/payment/status/TXN123 returns 404, but transaction exists in DB

**Solution**:
1. Verify txnid is exactly the same (case-sensitive):
   ```sql
   SELECT * FROM payment_transactions WHERE txnid = 'TXN123';
   ```

2. Check for leading/trailing spaces:
   ```sql
   SELECT LENGTH(txnid), txnid FROM payment_transactions;
   ```

3. Verify repository method:
   ```java
   Optional<PaymentTransaction> findByTxnid(String txnid)
   ```

4. Test directly:
   ```java
   paymentTransactionRepository.findByTxnid("TXN123");
   ```

---

### 10. Spring Boot Won't Start - Dependency Issues

**Error**:
```
Failed to bind properties under 'spring.datasource.url'
```

**Solution**:
1. Check pom.xml has all required dependencies:
   ```xml
   <dependency>
       <groupId>org.springframework.boot</groupId>
       <artifactId>spring-boot-starter-data-jpa</artifactId>
   </dependency>
   <dependency>
       <groupId>com.h2database</groupId>
       <artifactId>h2</artifactId>
       <scope>runtime</scope>
   </dependency>
   ```

2. Clear Maven cache:
   ```bash
   mvn clean install
   ```

3. Rebuild:
   ```bash
   mvn spring-boot:run
   ```

---

## FAQ

### Q1: Can I use MySQL instead of H2?

**A**: Yes! Update `application.yml`:
```yaml
spring:
  datasource:
    url: jdbc:mysql://localhost:3306/payment_db
    username: root
    password: password
    driver-class-name: com.mysql.cj.jdbc.Driver
  jpa:
    hibernate:
      dialect: org.hibernate.dialect.MySQL8Dialect
```

Add dependency:
```xml
<dependency>
    <groupId>mysql</groupId>
    <artifactId>mysql-connector-java</artifactId>
</dependency>
```

---

### Q2: What happens if payment params exceed field length?

**A**: Database columns have length limits:
```
- email: 255 chars
- phone: 20 chars
- firstname: 255 chars
- udf1-10: 300 chars each
```

If exceeded, database will throw error. Solution:
1. Truncate in service layer before save
2. Or increase column lengths in entity

---

### Q3: How long are receipts kept in database?

**A**: In-memory (H2) H2: Lost on application restart
File-based H2: Permanent (until deleted)
Production with MySQL: Can set retention policy

Example cleanup query (quarterly):
```sql
DELETE FROM payment_transactions 
WHERE created_at < DATE_SUB(NOW(), INTERVAL 1 YEAR);
```

---

### Q4: Can two simultaneous callbacks cause duplicate records?

**A**: No. Repository query `findByTxnid()` uses UNIQUE constraint on database.
Two concurrent writes to same txnid will:
1. First write creates record
2. Second write updates same record (idempotent)

---

### Q5: What if gateway sends callback but network fails before response?

**A**: Transaction saved in database but HTTP response not received by gateway.
Gateway retries. Second callback updates record (idempotent). No error.

---

### Q6: How to export all transactions?

**A**: Use H2 console or SQL:
```sql
-- Export as CSV
SELECT * FROM payment_transactions
ORDER BY created_at DESC;
```

Or use Java:
```java
List<PaymentTransaction> all = 
    paymentTransactionRepository.findAll();

// Convert to CSV or JSON using library
```

---

### Q7: Can I query transactions by date range?

**A**: Yes, create custom repository method:
```java
@Query("SELECT p FROM PaymentTransaction p " +
       "WHERE p.createdAt BETWEEN :start AND :end")
List<PaymentTransaction> findByDateRange(
    @Param("start") LocalDateTime start,
    @Param("end") LocalDateTime end
);
```

---

### Q8: How to handle payment refunds?

**A**: Add to PaymentTransaction entity:
```java
@Column(nullable = true)
private BigDecimal refundedAmount;

@Column(nullable = true)
private LocalDateTime refundedAt;
```

Update service on refund callback:
```java
transaction.setRefundedAmount(refundAmount);
transaction.setRefundedAt(LocalDateTime.now());
repository.save(transaction);
```

---

### Q9: Can I archive old transactions?

**A**: Create archive table:
```java
@Entity
@Table(name = "payment_transactions_archive")
public class PaymentTransactionArchive { ... }
```

Archive script:
```sql
INSERT INTO payment_transactions_archive 
SELECT * FROM payment_transactions 
WHERE created_at < DATE_SUB(NOW(), INTERVAL 1 YEAR);

DELETE FROM payment_transactions 
WHERE created_at < DATE_SUB(NOW(), INTERVAL 1 YEAR);
```

---

### Q10: How to integrate with external systems?

**A**: REST API endpoints provided:
```
GET  /api/payment/status/{txnid}
GET  /api/payment/exists/{txnid}
GET  /api/payment/hash-valid/{txnid}
```

External systems call endpoints to:
- Verify payment status
- Check if transaction exists
- Detect tampering

---

## Performance Tuning

### For High Volume Traffic

1. **Add Index on Status**:
   ```java
   @Column(name = "status")
   @Index(name = "idx_status")
   private String status;
   ```

2. **Add Index on Created Date**:
   ```java
   @Column(name = "created_at")
   @Index(name = "idx_created_at")
   private LocalDateTime createdAt;
   ```

3. **Use Connection Pooling** (HikariCP already included):
   ```yaml
   spring:
     datasource:
       hikari:
         maximum-pool-size: 10
         minimum-idle: 2
   ```

4. **Pagination for Large Queries**:
   ```java
   Page<PaymentTransaction> page = 
       repository.findByStatus("SUCCESS", PageRequest.of(0, 50));
   ```

---

## Security Considerations

1. **Never log sensitive data** (hash, full card details)
   - Currently logs only first 10 chars of hash ✓

2. **Validate all inputs** before database save
   - Already implemented in service ✓

3. **Use transactions** to ensure data consistency
   - Already implemented with @Transactional ✓

4. **Detect tampering** via hash verification
   - Already implemented ✓

5. **Encrypt database** in production
   - Configure in DB settings

6. **Use HTTPS** for all callbacks
   - Configure at gateway level

7. **Validate txnid format** to prevent SQL injection
   - Currently using parameterized queries ✓

---

## Monitoring & Alerts

### Recommended Alerts

```
1. Hash verification failures > 5/day
   → Possible attack or configuration issue

2. Database query time > 1000ms
   → Performance degradation, needs indexing

3. Payment status not updating for > 1 hour
   → Gateway connectivity issue

4. Duplicate callbacks > 10/day
   → Retry loop or gateway issue

5. Database size growing > 1GB/month
   → Need archival strategy
```

### Health Check Query

```sql
-- Run daily via cron job
SELECT 
  COUNT(*) as total_transactions,
  SUM(CASE WHEN status='SUCCESS' THEN 1 ELSE 0 END) as successful,
  SUM(CASE WHEN status='FAILED' THEN 1 ELSE 0 END) as failed,
  SUM(CASE WHEN hash_verified=false THEN 1 ELSE 0 END) as suspicious,
  MAX(created_at) as latest_transaction
FROM payment_transactions;
```

---

## Support

**Error not covered here?**

1. Check logs: `target/logs/application.log` (if configured)
2. Search GitHub issues for similar reports
3. Contact Easebuzz support with transaction ID
4. Enable debug logging:
   ```yaml
   logging:
     level:
       com.sabbpe.merchant: DEBUG
   ```

