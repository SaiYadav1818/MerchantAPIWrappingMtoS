# Quick Start Guide

## Prerequisites
- Java 17 or higher
- MySQL 8.0 or higher
- Maven 3.6.0 or higher
- Git

## 1. Clone/Setup Project

```bash
cd c:\Users\dudim\MerchantAPIWapper
```

## 2. Create Database

Connect to MySQL and run:

```sql
CREATE DATABASE merchant_db;
USE merchant_db;

-- Run the SQL script from src/main/resources/data.sql
```

Or use the provided data.sql file:

```bash
mysql -u root -p merchant_db < src/main/resources/data.sql
```

## 3. Configure Database Connection

Edit `src/main/resources/application.yml`:

```yaml
spring:
  datasource:
    url: jdbc:mysql://localhost:3306/merchant_db
    username: root
    password: root  # Change to your MySQL password
```

## 4. Build Project

```bash
mvn clean install
```

## 5. Run Application

```bash
mvn spring-boot:run
```

Or build JAR and run:

```bash
mvn clean package
java -jar target/merchant-api-wrapper-1.0.0.jar
```

## 6. Verify Application Started

Check console for:
```
Started MerchantApiWrapperApplication in X.XXX seconds
```

Application runs on: `http://localhost:8080`

## 7. Test API

### Generate Hash

For merchant M123 with order ORD1001, amount 1000.00:

```bash
# Using OpenSSL (Windows PowerShell)
$input = "M123ORD10011000.00secret_salt_key_123"
$hash = (([System.Text.Encoding]::UTF8.GetBytes($input) | 
  % { $h = [System.Security.Cryptography.SHA256]::Create(); 
      $h.ComputeHash($_); $h.Dispose() } ) | 
  % { '{0:x2}' -f $_ }) -join ''
Write-Host $hash
```

Or use online tool: https://www.sha256online.com/

### Test API Call

```bash
curl -X POST http://localhost:8080/api/payment/initiate ^
  -H "Content-Type: application/json" ^
  -d "{\"merchantId\":\"M123\",\"orderId\":\"ORD1001\",\"amount\":\"1000.00\",\"hash\":\"YOUR_HASH_HERE\"}"
```

## 8. View Logs

Logs show in console with format:
```
HH:mm:ss.SSS [thread] LEVEL package - message
```

Debug level logs show hash verification steps.

## API Endpoints

| Method | Endpoint | Purpose |
|--------|----------|---------|
| POST | /api/payment/initiate | Initiate payment verification |

## Database Tables

### merchants
- id (Long, PK)
- merchant_id (String, Unique)
- merchant_name (String)
- salt_key (String)
- status (ACTIVE/INACTIVE)
- created_at (Timestamp)
- updated_at (Timestamp)

### transactions
- id (Long, PK)
- order_id (String)
- merchant_id (String)
- amount (Decimal)
- status (INITIATED/PROCESSING/COMPLETED/FAILED/CANCELLED)
- internal_token (String, Unique)
- created_at (Timestamp)
- updated_at (Timestamp)

## Sample Merchants (Pre-loaded)

1. **M123** - Test Merchant (ACTIVE)
   - Salt Key: `secret_salt_key_123`

2. **M456** - Production Merchant (ACTIVE)
   - Salt Key: `prod_salt_key_456`

3. **M789** - Inactive Merchant (INACTIVE)
   - Salt Key: `inactive_salt_key_789`

## Testing

### Unit Tests
```bash
mvn test
```

### Integration Tests
```bash
mvn test -Dtest=PaymentControllerIntegrationTest
```

## Common Issues

### Issue: "Access denied for user 'root'@'localhost'"
**Solution**: Update password in application.yml

### Issue: "Database 'merchant_db' doesn't exist"
**Solution**: Create database using SQL script

### Issue: "Port 8080 already in use"
**Solution**: Change port in application.yml:
```yaml
server:
  port: 8081
```

### Issue: Maven build fails
**Solution**: 
```bash
mvn clean
mvn install
```

## Development Workflow

1. **Modify code** in `src/main/java`
2. **Run tests**: `mvn test`
3. **Build**: `mvn clean install`
4. **Run**: `mvn spring-boot:run`
5. **Test API**: Use curl or Postman

## IDE Setup

### VS Code
1. Install "Extension Pack for Java"
2. Open folder in VS Code
3. Run/Debug from VS Code

### IntelliJ IDEA
1. File → Open → Select project
2. Configure JDK 17
3. Run → Run Application

## Project Structure

```
MerchantAPIWapper/
├── pom.xml
├── README.md
├── API_DOCUMENTATION.md
├── QUICKSTART.md
├── src/
│   ├── main/
│   │   ├── java/com/sabbpe/merchant/
│   │   │   ├── MerchantApiWrapperApplication.java
│   │   │   ├── controller/
│   │   │   ├── service/
│   │   │   ├── repository/
│   │   │   ├── entity/
│   │   │   ├── dto/
│   │   │   ├── util/
│   │   │   ├── exception/
│   │   │   └── config/
│   │   └── resources/
│   │       ├── application.yml
│   │       └── data.sql
│   └── test/
│       ├── java/com/sabbpe/merchant/
│       └── resources/
└── target/
```

## Next Steps

1. Configure your merchant database
2. Generate hash for test transaction
3. Call API endpoint
4. Verify transaction in database
5. Integrate into your application

## Support/Documentation

- Full API Documentation: [API_DOCUMENTATION.md](API_DOCUMENTATION.md)
- Project README: [README.md](README.md)
- Code Comments: Javadoc in source files
- Test Examples: Check `src/test` directory

## Production Deployment

Before production:
- [ ] Change MySQL credentials
- [ ] Use HTTPS
- [ ] Enable proper logging
- [ ] Set up monitoring
- [ ] Configure backup strategy
- [ ] Set up CI/CD pipeline
- [ ] Run security audit
- [ ] Load testing
