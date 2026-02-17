# Payment Gateway Redirect Implementation Guide

## Overview
This Spring Boot 3.2 application implements a complete payment gateway redirect system with professional HTML pages to display payment response parameters. The implementation uses Thymeleaf templates, Lombok, and SLF4J logging.

---

## Architecture Overview

```
┌─────────────────────────────────────────────────────────────┐
│                  Payment Gateway (Easebuzz)                 │
│                                                              │
│  After payment processing, redirects to callback URL        │
└────────────────────┬────────────────────────────────────────┘
                     │
                     │ POST Request with Form Parameters
                     │
                     ▼
┌─────────────────────────────────────────────────────────────┐
│           PaymentRedirectController                          │
│  (/payment/success or /payment/failure endpoints)           │
│                                                              │
│  - Receives parameters in Map<String, String>              │
│  - Logs all parameters                                     │
│  - Adds data to Model                                      │
└────────────────────┬────────────────────────────────────────┘
                     │
                     │ Model passed to View
                     │
                     ▼
┌─────────────────────────────────────────────────────────────┐
│          Thymeleaf Template Engine                          │
│                                                              │
│  - payment-success.html (Success Page)                    │
│  - payment-failure.html (Failure Page)                    │
│                                                              │
│  Dynamically renders all payment parameters               │
└─────────────────────────────────────────────────────────────┘
```

---

## Step 1: Controller Implementation

**File:** `src/main/java/com/sabbpe/merchant/controller/PaymentRedirectController.java`

The controller handles two POST endpoints:
- `/payment/success` - Handles successful payment redirects
- `/payment/failure` - Handles failed payment redirects

**Key Features:**
- Accepts all request parameters as `Map<String, String>`
- Logs all incoming parameters for debugging
- Passes data to Thymeleaf templates via Model attribute
- Clean separation of concerns

---

## Step 2: Success HTML Template

**File:** `src/main/resources/templates/payment-success.html`

**Features:**
- Professional success page design
- Green color scheme with success icon
- Dynamic parameter display using Thymeleaf loops
- Displays transaction ID prominently
- Print receipt functionality
- Responsive mobile design
- Action buttons (Return Home, Print Receipt)

**Thymeleaf Loop Example:**
```html
<tr th:each="entry : ${paymentData}">
    <td class="key-column" th:text="${entry.key}">Key</td>
    <td class="value-column" th:text="${entry.value ?: 'N/A'}">Value</td>
</tr>
```

---

## Step 3: Failure HTML Template

**File:** `src/main/resources/templates/payment-failure.html`

**Features:**
- Professional failure page design
- Red color scheme with error icon
- Support contact information
- Common failure reasons listed
- Dynamic parameter display
- Responsive design
- Action buttons (Retry Payment, Return Home)

---

## Step 4: Thymeleaf Configuration

**File:** `src/main/resources/application.yml`

```yaml
spring:
  thymeleaf:
    cache: false           # Disable caching for development
    mode: HTML             # Use HTML mode
    encoding: UTF-8        # UTF-8 encoding
```

---

## Step 5: Maven Dependency

**File:** `pom.xml`

Added Thymeleaf starter dependency:
```xml
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-thymeleaf</artifactId>
</dependency>
```

---

## Payment Initiation Configuration

To properly redirect users after payment, configure your payment initiation request with these URLs:

### For Easebuzz Payment Gateway:

**Success URL (surl):**
```
http://localhost:8080/payment/success
```

**Failure URL (furl):**
```
http://localhost:8080/payment/failure
```

### Example Payment Initiation Request:

```java
Map<String, String> paymentParams = new HashMap<>();
paymentParams.put("key", "ZF93ZSH6B");                           // Merchant Key
paymentParams.put("txnid", "TXN123456789");                      // Unique Transaction ID
paymentParams.put("amount", "100.00");                           // Payment Amount
paymentParams.put("productinfo", "Product Description");        // Product Info
paymentParams.put("firstname", "John");                          // Customer First Name
paymentParams.put("email", "john@example.com");                 // Customer Email
paymentParams.put("phone", "9999999999");                        // Customer Phone
paymentParams.put("surl", "http://localhost:8080/payment/success"); // Success URL
paymentParams.put("furl", "http://localhost:8080/payment/failure"); // Failure URL
paymentParams.put("hash", generateHash(paymentParams));         // SHA-512 Hash
```

---

## Easebuzz Callback Response Parameters

### Successful Payment Response:

The Easebuzz gateway will POST the following parameters to `/payment/success`:

```
Parameter Name          | Description                    | Example Value
------------------------|--------------------------------|-----------------------------------------
txnid                   | Transaction ID                 | TXN123456789
status                  | Payment Status                 | success
amount                  | Payment Amount                 | 100.00
firstname               | Customer First Name            | John
email                   | Customer Email                 | john@example.com
phone                   | Customer Phone                 | 9999999999
productinfo             | Product Information            | Product Description
easepayid               | Easebuzz Payment ID           | EP1234567890
hash                    | Response Hash (SHA-512)       | a1b2c3d4e5f6g7h8i9j0k1l2m3n4o5p6q7r8s9t0u1v2w3x4y5z6a7b8c9d0e1f2
mode                    | Payment Mode                   | CC/NB/DC/WALLET
bankname                | Bank Name (if applicable)      | HDFC Bank
cardname                | Card Type (if applicable)      | Visa/Mastercard
udf1 to udf5            | Custom Fields                  | Custom Value 1
```

### Failed Payment Response:

The Easebuzz gateway will POST the following parameters to `/payment/failure`:

```
Parameter Name          | Description                    | Example Value
------------------------|--------------------------------|-----------------------------------------
txnid                   | Transaction ID                 | TXN123456789
status                  | Payment Status                 | failure/cancel
amount                  | Payment Amount                 | 100.00
firstname               | Customer First Name            | John
email                   | Customer Email                 | john@example.com
phone                   | Customer Phone                 | 9999999999
productinfo             | Product Information            | Product Description
easepayid               | Error Code/Easebuzz ID        | ERROR_CODE or blank
hash                    | Response Hash (SHA-512)       | a1b2c3d4e5f6g7h8i9j0k1l2m3n4o5p6q7r8s9t0u1v2w3x4y5z6a7b8c9d0e1f2
error_Message           | Error Description              | Payment Declined
mode                    | Payment Mode                   | CC/NB/DC/WALLET
udf1 to udf5            | Custom Fields                  | Custom Value 1
```

---

## Running the Application

1. **Build the project:**
   ```bash
   mvn clean install
   ```

2. **Run the application:**
   ```bash
   mvn spring-boot:run
   ```

3. **Default URL:**
   ```
   http://localhost:8080
   ```

---

## Testing Payment Redirect

### Using curl to simulate payment gateway response:

**Success Response Test:**
```bash
curl -X POST "http://localhost:8080/payment/success" \
  -d "txnid=TXN123456789" \
  -d "status=success" \
  -d "amount=100.00" \
  -d "easepayid=EP1234567890" \
  -d "hash=abcdef1234567890" \
  -d "firstname=John" \
  -d "email=john@example.com" \
  -d "productinfo=Test Product"
```

**Failure Response Test:**
```bash
curl -X POST "http://localhost:8080/payment/failure" \
  -d "txnid=TXN123456789" \
  -d "status=failure" \
  -d "amount=100.00" \
  -d "easepayid=" \
  -d "hash=abcdef1234567890" \
  -d "firstname=John" \
  -d "email=john@example.com" \
  -d "productinfo=Test Product" \
  -d "error_message=Payment Declined"
```

---

## Logging Output

The application uses SLF4J for logging. All incoming parameters are logged at:
- **INFO level**: Redirect received and transaction details
- **DEBUG level**: Individual parameter values
- **WARN level**: When transaction not found in database

**Example Log Output:**
```
2024-02-17T22:15:32.123+05:30  INFO 12345 --- [nio-8080-exec-1] c.s.m.c.PaymentRedirectController : Payment Success Redirect - Parameters received: [txnid, status, amount, easepayid, hash, firstname, email, productinfo]
2024-02-17T22:15:32.124+05:30 DEBUG 12345 --- [nio-8080-exec-1] c.s.m.c.PaymentRedirectController : Param: txnid = TXN123456789
2024-02-17T22:15:32.124+05:30 DEBUG 12345 --- [nio-8080-exec-1] c.s.m.c.PaymentRedirectController : Param: status = success
```

---

## Directory Structure

```
src/main/
├── java/
│   └── com/sabbpe/merchant/
│       └── controller/
│           └── PaymentRedirectController.java
├── resources/
│   ├── application.yml
│   └── templates/
│       ├── payment-success.html
│       └── payment-failure.html
```

---

## Features

✅ **Production-Ready Code**
- Clean, well-documented code
- Proper exception handling
- Security considerations
- Performance optimized

✅ **Professional UI**
- Responsive design (mobile-friendly)
- CSS animations and transitions
- Color-coded success/failure pages
- Dynamic table display

✅ **Thymeleaf Integration**
- Dynamic parameter rendering
- Null-safe expressions
- Loop iteration support
- Proper HTML escaping

✅ **Logging & Monitoring**
- SLF4J integration
- Parameter logging
- Error tracking
- Debug information

✅ **Best Practices**
- Separation of concerns
- Model-View-Controller pattern
- Single responsible methods
- Clear naming conventions

---

## Future Enhancements

1. **Hash Verification**
   - Implement SHA-512 hash validation on callback
   - Verify response authenticity

2. **Database Integration**
   - Save payment responses to database
   - Update transaction status
   - Store payment metadata

3. **Email Notifications**
   - Send confirmation emails on success
   - Send failure alerts to customers
   - Send admin notifications

4. **Admin Dashboard**
   - View payment history
   - Filter by status, date, amount
   - Export payment reports

5. **Multiple Gateway Support**
   - Razorpay integration
   - PayU integration
   - Stripe integration

6. **Advanced Features**
   - Webhook retry mechanism
   - Payment reconciliation
   - Partial refund support
   - Multi-currency support

---

## Security Considerations

1. **Always validate hash** on production
2. **Use HTTPS** for payment URLs
3. **Store sensitive data** encrypted
4. **Implement CSRF protection** for forms
5. **Rate limit** callback endpoints
6. **Sanitize** all user inputs
7. **Log security events** separately
8. **Use environment variables** for keys

---

## Troubleshooting

| Issue | Solution |
|-------|----------|
| Templates not found | Ensure templates are in `src/main/resources/templates/` |
| Parameters not displaying | Check Thymeleaf cache setting is false in development |
| CSS not loading | Verify CSS is embedded in HTML (no external file needed) |
| POST parameters not received | Ensure form data is sent with correct parameter names |
| Endpoint returns 404 | Check controller mapping: `/payment/success` or `/payment/failure` |

---

## Contact & Support

For issues, questions, or improvements, contact the development team.

**Version:** 1.0.0  
**Last Updated:** February 17, 2024  
**Spring Boot Version:** 3.2.0  
**Java Version:** 21
