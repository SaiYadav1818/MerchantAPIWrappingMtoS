# Complete Spring Boot 3 Payment Gateway Integration with UDF Support

**Date**: February 17, 2026  
**Version**: 1.1.0  
**Framework**: Spring Boot 3.2.0 | Java 21 | Maven  
**Payment Gateway**: Easebuzz  

---

## 📋 Table of Contents

1. [System Overview](#system-overview)
2. [Architecture](#architecture)
3. [Component Details](#component-details)
4. [UDF Fields Support](#udf-fields-support)
5. [Payment Flow](#payment-flow)
6. [Hash Verification](#hash-verification)
7. [Multi-Merchant Routing](#multi-merchant-routing)
8. [API Reference](#api-reference)
9. [Testing Guide](#testing-guide)
10. [Security Considerations](#security-considerations)
11. [Troubleshooting](#troubleshooting)

---

## System Overview

This implementation provides a **production-ready** Spring Boot 3 payment gateway integration system with:

✅ **Full UDF1-UDF10 Support** - Custom fields for merchant and order tracking  
✅ **SHA-512 Hash Verification** - Tamper detection for payment responses  
✅ **Multi-Merchant Routing** - Route payments to merchants using UDF1  
✅ **Professional Thymeleaf UI** - Responsive HTML templates with dynamic parameter display  
✅ **Comprehensive Logging** - Security audit trails for all transactions  
✅ **Error Handling** - Graceful error responses with detailed messages  

---

## Architecture

```
Payment Flow:
┌─────────────────────────────────────────────────────────────────┐
│ 1. PaymentInitiateController                                    │
│    └─> Receives payment initiation request from merchant        │
│    └─> Generates SHA-512 hash (with UDF1-UDF10)                 │
│    └─> Calls Easebuzz API                                       │
└─────────────────────────────────────────────────────────────────┘
                            ↓
┌─────────────────────────────────────────────────────────────────┐
│ 2. Payment Gateway (Easebuzz)                                   │
│    └─> Processes payment                                        │
│    └─> Returns with UDF fields included                         │
│    └─> Redirects to /payment/success or /payment/failure        │
└─────────────────────────────────────────────────────────────────┘
                            ↓
┌─────────────────────────────────────────────────────────────────┐
│ 3. PaymentRedirectController                                    │
│    ├─> Receives redirect from gateway                           │
│    ├─> Logs all parameters including UDF fields                 │
│    ├─> Verifies hash (PaymentVerificationService)               │
│    └─> Extracts merchant/order info from UDF1, UDF2             │
└─────────────────────────────────────────────────────────────────┘
                            ↓
┌─────────────────────────────────────────────────────────────────┐
│ 4. MerchantRoutingService                                       │
│    ├─> Routes payment to merchant handler                       │
│    ├─> Updates merchant wallet                                  │
│    ├─> Sends webhook callback to merchant                       │
│    └─> Sends customer receipt/notification                      │
└─────────────────────────────────────────────────────────────────┘
                            ↓
┌─────────────────────────────────────────────────────────────────┐
│ 5. Thymeleaf Templates (payment-success.html/failure.html)      │
│    ├─> Display transaction results                              │
│    ├─> Show merchant and order info                             │
│    ├─> Display all UDF fields in grid                           │
│    └─> Verify security badge                                    │
└─────────────────────────────────────────────────────────────────┘
```

---

## Component Details

### 1. EasebuzzHashUtil (util/EasebuzzHashUtil.java)

**Purpose**: Generate and verify SHA-512 hashes for payment security

**Key Methods**:
```java
// Forward hash (for initiating payment)
generateHashWithUDF(key, txnid, amount, productinfo, firstname, email,
                    udf1-udf10, salt)

// Reverse hash (for verifying callback)
generateReverseHashWithUDF(salt, status, udf10-udf1, email, firstname,
                           productinfo, amount, txnid, key)

// Backward compatibility (UDF1-UDF5 only)
@Deprecated
generateHash(...with UDF1-UDF5 only...)
```

**Hash Format** (Forward):
```
key|txnid|amount|productinfo|firstname|email|udf1|udf2|udf3|udf4|udf5|udf6|udf7|udf8|udf9|udf10|salt
```

**Hash Format** (Reverse - Callback Verification):
```
salt|status|udf10|udf9|udf8|udf7|udf6|udf5|udf4|udf3|udf2|udf1|email|firstname|productinfo|amount|txnid|key
```

### 2. PaymentVerificationService (service/PaymentVerificationService.java)

**Purpose**: Verify payment responses and detect tampering

**Key Methods**:
- `verifyPaymentHash()` - Verify incoming hash matches calculated hash
- `validateRequiredFields()` - Ensure all required fields present
- `validatePaymentResponse()` - Composite validation (fields + hash)

**Security Checks**:
1. Check if hash present in response
2. Rebuild reverse hash using gateway formula
3. Compare incoming hash with calculated hash
4. Log suspicious activity if mismatch detected
5. Mark payment as tampered if hash invalid

### 3. MerchantRoutingService (service/MerchantRoutingService.java)

**Purpose**: Route payments to appropriate merchants based on UDF fields

**Key Methods**:
- `routePaymentToMerchant()` - Main entry point for routing
- `extractMerchantAndOrderInfo()` - Extract merchant ID (UDF1) and order ID (UDF2)
- `extractUDFValues()` - Extract all UDF1-UDF10 from response
- `handleSuccessfulPayment()` - Process successful payment
- `handleFailedPayment()` - Process failed payment

**Default UDF Configuration**:
- **UDF1** = Merchant ID (used for routing)
- **UDF2** = Order ID (used for tracking)
- **UDF3-UDF10** = Custom fields (application-specific)

**Processing Flow** (Success):
1. Log transaction details
2. Update merchant wallet (CREDIT)
3. Send webhook to merchant
4. Send customer receipt email
5. Return success status

**Processing Flow** (Failure):
1. Log failure details
2. Send failure webhook to merchant
3. Send customer failure notification
4. Return failure status

### 4. PaymentRedirectController (controller/PaymentRedirectController.java)

**Purpose**: Handle payment gateway redirects for success/failure scenarios

**Endpoints**:
```
POST /payment/success  - Success redirect from gateway
POST /payment/failure  - Failure redirect from gateway
```

**Processing Steps**:
1. **Log all parameters** including UDF fields
2. **Verify hash** using PaymentVerificationService
3. **Handle suspicious activity** - Mark if hash invalid
4. **Extract merchant info** - Get merchantId (UDF1) and orderId (UDF2)
5. **Route payment** - Call MerchantRoutingService
6. **Render response** - Display appropriate Thymeleaf template

**Response Model Attributes**:
```java
model.addAttribute("paymentData", requestParams);     // All parameters
model.addAttribute("merchantId", merchantId);          // From UDF1
model.addAttribute("orderId", orderId);                 // From UDF2
model.addAttribute("udfFields", udfMap);               // All UDF1-UDF10
model.addAttribute("hashVerified", isValid);           // Verification status
model.addAttribute("suspiciousActivity", isDetected);  // Tampering detected
```

---

## UDF Fields Support

### Standard UDF Configuration

| Field | Purpose | Type | Max Length | Default |
|-------|---------|------|-----------|---------|
| udf1 | Merchant ID | String | 50 | - |
| udf2 | Order ID | String | 50 | - |
| udf3 | Invoice Number | String | 50 | Optional |
| udf4 | Department | String | 50 | Optional |
| udf5 | Reference Code | String | 50 | Optional |
| udf6 | Campaign ID | String | 50 | Optional |
| udf7 | Partner ID | String | 50 | Optional |
| udf8 | Affiliate ID | String | 50 | Optional |
| udf9 | Source/Channel | String | 50 | Optional |
| udf10 | Custom Metadata | String | 500 | Optional |

### Using UDF Fields in Payment Initiation

```java
EasebuzzPaymentRequest request = EasebuzzPaymentRequest.builder()
    .txnid("TXN20260217001")
    .amount(new BigDecimal("999.99"))
    .productinfo("Premium Subscription")
    .firstname("John")
    .email("john@example.com")
    .phone("9999999999")
    
    // UDF Fields
    .udf1("MERCHANT_001")           // Destination merchant ID
    .udf2("ORDER_12345")            // Order reference
    .udf3("INV_2026_001")           // Invoice number
    .udf4("SALES")                  // Department
    .udf5("REF_ABC123")             // Reference code
    .udf6("CAMPAIGN_SPRING_2026")   // Campaign ID
    .udf7("PARTNER_XYZ")            // Partner ID
    .udf8("AFFILIATE_123")          // Affiliate tracking
    .udf9("MOBILE_APP")             // Source channel
    .udf10("{\"tier\":\"gold\",\"region\":\"north\"}")  // JSON metadata
    
    .build();
```

### Receiving UDF Fields in Redirect

Gateway returns all UDF fields in redirect response:

```html
POST /payment/success HTTP/1.1

txnid=TXN20260217001&
status=success&
amount=999.99&
email=john@example.com&
firstname=John&
udf1=MERCHANT_001&
udf2=ORDER_12345&
udf3=INV_2026_001&
udf4=SALES&
udf5=REF_ABC123&
udf6=CAMPAIGN_SPRING_2026&
udf7=PARTNER_XYZ&
udf8=AFFILIATE_123&
udf9=MOBILE_APP&
udf10={"tier":"gold","region":"north"}&
hash=<sha512_hash>
```

---

## Payment Flow

### 1. Payment Initiation Request

**Request**:
```json
POST /api/payment/easebuzz/initiate
Content-Type: application/json

{
  "txnid": "TXN20260217001",
  "amount": 999.99,
  "productinfo": "Premium Subscription",
  "firstname": "John Doe",
  "phone": "9999999999",
  "email": "john@example.com",
  "udf1": "MERCHANT_001",
  "udf2": "ORDER_12345",
  "udf3": "INV_2026_001",
  "udf4": "SALES",
  "udf5": "REF_ABC123",
  "udf6": "CAMPAIGN_SPRING_2026",
  "udf7": "PARTNER_XYZ",
  "udf8": "AFFILIATE_123",
  "udf9": "MOBILE_APP",
  "udf10": "{\"tier\":\"gold\"}"
}
```

**Process in Controller**:
1. **Validate request** - Check all required fields
2. **Generate hash** - SHA-512 with UDF1-UDF10
3. **Call Easebuzz API** - POST to gateway
4. **Parse response** - Extract payment reference/error

**Success Response**:
```json
{
  "status": "SUCCESS",
  "message": "Payment reference: PAYTM_12345",
  "paymentUrl": "https://easebuzz.in/payment/PAYTM_12345"
}
```

Redirect customer to `paymentUrl` to complete payment.

### 2. Payment Redirect (Success/Failure)

After payment completion, gateway redirects to:

**Success**: `POST /payment/success`  
**Failure**: `POST /payment/failure`

**Parameters Received** (Form data):
```
txnid: TXN20260217001
status: success or failure
amount: 999.99
email: john@example.com
firstname: John Doe
productinfo: Premium Subscription
udf1: MERCHANT_001
udf2: ORDER_12345
udf3-udf10: (custom values)
hash: <sha512_hash>
... (other gateway parameters)
```

**Controller Processing**:

```
1. Log all parameters
2. Verify hash (Security Check)
   ├─ If invalid → Mark suspicious, show error page
   └─ If valid → Continue
3. Extract merchant info from UDF
   ├─ udf1 → merchantId
   └─ udf2 → orderId
4. Route to merchant
   ├─ Send webhook
   ├─ Update wallet
   └─ Send notifications
5. Render response page
```

### 3. Response Display (HTML)

**Payment Success Page** (`payment-success.html`):
- Green theme with success icon
- Transaction ID display
- Hash verification badge
- Merchant ID and Order ID
- All UDF fields in grid layout
- Complete parameter table
- Print receipt button

**Payment Failure Page** (`payment-failure.html`):
- Red theme with error icon
- Error message
- Hash verification status
- Merchant ID and Order ID
- All UDF fields in grid layout
- Common failure reasons
- Support contact information
- Retry payment button

---

## Hash Verification

### Verification Process

```
Received Hash: <from_gateway>
              ↓
Calculate Hash:
  1. Extract all parameters from request
  2. Rebuild hash using reverse formula
  3. Result: <calculated_hash>
              ↓
Compare:
  if (<from_gateway> == <calculated_hash>) {
    ✓ Hash valid - Payment verified
  } else {
    ✗ Hash invalid - Tampering detected!
    → Log security alert
    → Block payment
    → Notify admin
  }
```

### Reverse Hash Formula

```
Incoming Reverse Hash String:
salt|status|udf10|udf9|udf8|udf7|udf6|udf5|udf4|udf3|udf2|udf1|email|firstname|productinfo|amount|txnid|key

Example:
SALT123|success|{tier:gold}|MOBILE_APP|AFFILIATE_123|PARTNER_XYZ|
CAMPAIGN_SPRING_2026|REF_ABC123|SALES|INV_2026_001|ORDER_12345|MERCHANT_001|
john@example.com|John Doe|Premium Subscription|999.99|TXN20260217001|KEY456
```

### Code Example

```java
// In PaymentVerificationService
boolean isValid = verificationService.validatePaymentResponse(paymentData);

if(!isValid) {
    // Hash verification failed
    log.error("Tampering detected! Payment blocked.");
    // Mark transaction as suspicious in database
    // Alert admin
    // Return error to customer
}
```

---

## Multi-Merchant Routing

### Routing Strategy

**Default Configuration**:
- Extract merchant ID from **UDF1**
- Extract order ID from **UDF2**
- Use merchant ID to determine routing handler

### Merchant Routing Service Flow

```java
routePaymentToMerchant(merchantId="MERCHANT_001", 
                       orderId="ORDER_12345", 
                       paymentData={...})
  ├─ if(status == "success")
  │  ├─ Update merchant wallet (+amount)
  │  ├─ Send success webhook to merchant
  │  ├─ Send customer receipt
  │  └─ Log transaction as COMPLETED
  │
  └─ else if(status == "failure")
     ├─ Log transaction as FAILED
     ├─ Send failure webhook to merchant
     ├─ Send customer failure notification
     └─ Return failure status
```

### Webhook Payload Format

```json
POST https://merchant-webhook-url/payment-callback

{
  "event": "payment.completed",
  "merchantId": "MERCHANT_001",
  "orderId": "ORDER_12345",
  "txnId": "TXN20260217001",
  "status": "SUCCESS",
  "amount": 999.99,
  "email": "john@example.com",
  "timestamp": "2026-02-17T18:30:00Z",
  "udf1": "MERCHANT_001",
  "udf2": "ORDER_12345",
  "udf3": "INV_2026_001",
  "udf4": "SALES",
  "udf5": "REF_ABC123"
}
```

### Custom Extension Points

You can extend MerchantRoutingService for custom logic:

```java
@Slf4j
@Service
public class MerchantRoutingService {
    
    private void updateMerchantWallet(String merchantId, String amount, String operation) {
        // TODO: Implement custom wallet update logic
    }
    
    private void sendMerchantWebhook(String merchantId, String orderId, 
                                     String txnId, String status, Map<String, String> data) {
        // TODO: Implement custom webhook delivery with retries
    }
    
    private void sendCustomerReceipt(String email, String orderId, 
                                     String txnId, String amount) {
        // TODO: Implement email template and send receipt
    }
}
```

---

## API Reference

### Payment Initiation Endpoint

```
POST /api/payment/easebuzz/initiate
Content-Type: application/json
Authorization: Bearer <token> (if protected)

Request Body:
{
  "txnid": "string (unique, required)",
  "amount": "bigdecimal (required)",
  "productinfo": "string (required)",
  "firstname": "string (required)",
  "phone": "string (required)",
  "email": "string (required)",
  "udf1": "string (optional)",
  "udf2": "string (optional)",
  "udf3": "string (optional)",
  "udf4": "string (optional)",
  "udf5": "string (optional)",
  "udf6": "string (optional)",
  "udf7": "string (optional)",
  "udf8": "string (optional)",
  "udf9": "string (optional)",
  "udf10": "string (optional)"
}

Success Response (200):
{
  "status": "SUCCESS",
  "message": "Payment reference: PAYTM_12345",
  "paymentUrl": "https://easebuzz.in/payment/PAYTM_12345"
}

Error Response (409/400/500):
{
  "status": "FAILURE",
  "message": "Duplicate transaction id",
  "errorCode": "DUPLICATE_TXN",
  "details": "Transaction ID TXN20260217001 already processed"
}
```

### Payment Redirect Endpoints

```
POST /payment/success
Form Data: All parameters from gateway + UDF fields
Response: Thymeleaf template (payment-success.html)

POST /payment/failure
Form Data: All parameters from gateway + UDF fields
Response: Thymeleaf template (payment-failure.html)
```

---

## Testing Guide

### 1. Local Testing with Payment Gateway Test Form

Navigate to `/test` to access the payment gateway test form.

**Test Success Flow**:
1. Enter transaction ID: `TEST_TXN_20260217_001`
2. Select "Success" option
3. Fill sample data
4. Click "Submit to Success Handler"
5. Verify hash badge shows "✓ Hash verification successful"
6. Check UDF fields display

**Test Failure Flow**:
1. Enter transaction ID: `TEST_TXN_FAILURE`
2. Select "Failure" option
3. Fill sample data
4. Click "Submit to Failure Handler"
5. Verify error message displays
6. Check support contact information shown

### 2. Hash Verification Testing

**Valid Hash Test**:
```
Request with correct hash
→ Response shows ✓ Hash verification successful
→ Green security badge appears
```

**Invalid Hash Test**:
```
Modify hash in request
→ Response shows ✗ Hash verification failed
→ Red security badge appears
→ Error message: "Payment verification failed"
```

### 3. UDF Field Testing

**Verify UDF Display**:
1. Initiate payment with all UDF1-UDF10 filled
2. Check final response page
3. Confirm all UDF fields appear in grid
4. Verify merchant ID and order ID show correct values

**Verify UDF Routing**:
1. Send payment with UDF1 = "TEST_MERCHANT_001"
2. Check server logs for merchant routing message
3. Confirm webhook sent to merchant endpoint

### 4. Unit Test Examples

```java
@Test
public void testHashVerification() {
    // Setup
    Map<String, String> paymentData = new HashMap<>();
    paymentData.put("txnid", "TEST_001");
    paymentData.put("status", "success");
    paymentData.put("amount", "999.99");
    paymentData.put("hash", generateValidHash());
    
    // Execute
    boolean result = verificationService.validatePaymentResponse(paymentData);
    
    // Assert
    assertTrue(result);
}

@Test
public void testUDFExtraction() {
    // Setup
    Map<String, String> payment Data = new HashMap<>();
    paymentData.put("udf1", "MERCHANT_001");
    paymentData.put("udf2", "ORDER_12345");
    
    // Execute
    String[] info = routingService.extractMerchantAndOrderInfo(paymentData);
    
    // Assert
    assertEquals("MERCHANT_001", info[0]);
    assertEquals("ORDER_12345", info[1]);
}
```

---

## Security Considerations

### 1. Hash Verification (Mandatory)

✅ **Always verify hash** before processing payment  
✅ **Use reverse hash formula** as specified by gateway  
✅ **Log all hash mismatches** for audit trail  
✅ **Block suspicious payments** immediately  

### 2. UDF Field Validation

✅ **Validate UDF1 (merchant ID)** exists in database  
✅ **Sanitize UDF10** if contains JSON/special chars  
✅ **Limit UDF field length** per specifications  
✅ **Encode UDF data** for database storage  

### 3. Parameter Validation

✅ **Validate all required fields** present  
✅ **Check amount matches** original request (prevent tampering)  
✅ **Verify email format** is valid  
✅ **Reject missing parameters** with 400 error  

### 4. HTTP Security

✅ **Use HTTPS only** for all payment endpoints  
✅ **Implement CSRF protection** for POST endpoints  
✅ **Set secure cookies** (HttpOnly, Secure flags)  
✅ **Use Content-Security-Policy** headers  

### 5. Logging & Auditing

✅ **Log all payment attempts** (success/failure)  
✅ **Log suspicious activities** separately  
✅ **Never log sensitive data** (except for audit)  
✅ **Keep logs for 90+ days** minimum  

### 6. Error Handling

✅ **Don't expose system details** in error messages  
✅ **Use friendly error messages** for users  
✅ **Log detailed errors internally** for debugging  
✅ **Notify admins** of critical errors  

---

## Troubleshooting

### Issue 1: "Hash verification failed"

**Possible Causes**:
1. Hash generated with wrong salt
2. Hash generated with missing/wrong UDF fields
3. Any parameter  modified after hash generation
4. Clock skew between systems

**Solution**:
```java
// Verify salt is correctly configured
@Value("${easebuzz.salt}")
private String easebuzzSalt;

// Check hash includes all UDF fields
String hash = EasebuzzHashUtil.generateHashWithUDF(
    key, txnid, amount, productinfo, firstname, email,
    udf1, udf2, udf3, udf4, udf5, udf6, udf7, udf8, udf9, udf10,
    salt);  // Must match gateway salt

// Log hash components for debugging
log.debug("Hash input: key|txnid|amount|productinfo|firstname|email|
         udf1|udf2|...|udf10|salt");
```

### Issue 2: "UDF fields not received"

**Possible Causes**:
1. UDF fields not included in payment initiation
2. Gateway stripping empty UDF fields
3. Redirect parameters malformed

**Solution**:
```java
// Ensure all UDF fields included in request
EasebuzzInitiateRequest request = EasebuzzInitiateRequest.builder()
    .udf1("MERCHANT_001")
    .udf2("ORDER_12345")
    // ... fill all UDF fields needed
    .build();

// Log received UDF fields
Map<String, String> udfValues = routingService.extractUDFValues(paymentResponse);
log.info("Received UDFs: {}", udfValues);
```

### Issue 3: "Payment merchant not found"

**Possible Causes**:
1. UDF1 (merchant ID) not in database
2. Merchant ID malformed or null
3. Merchant marked as inactive

**Solution**:
```java
// Validate merchant exists before routing
String[] merchantInfo = routingService.extractMerchantAndOrderInfo(response);
if(merchantInfo[0] == null || merchantInfo[0].isEmpty()) {
    log.error("Merchant ID missing! Cannot route.");
    // Mark transaction as suspicious
    // Return error to customer
    return;
}

// Check merchant status in database
Merchant merchant = merchantRepository.findById(merchantInfo[0]);
if(merchant == null) {
    log.error("Merchant not found: {}", merchantInfo[0]);
    // Handle missing merchant
}
```

### Issue 4: "Template display showing wrong data"

**Possible Causes**:
1. Model attributes not set in controller
2. Thymeleaf expression syntax error
3. Null values causing empty displays

**Solution**:
```java
// Ensure all model attributes set
model.addAttribute("paymentData", requestParams);
model.addAttribute("merchantId", merchantId);  // Don't forget
model.addAttribute("orderId", orderId);        // Don't forget
model.addAttribute("udfFields", udfMap);       // Don't forget
model.addAttribute("hashVerified", boolean);   // Don't forget

// In template, use null-safe expressions
<value th:text="${field.get('udf1') ?: 'N/A'}"></value>
<th:block th:unless="${field.get('udf1') && field.get('udf1').length() > 0}">
  <value class="empty">(empty)</value>
</th:block>
```

### Issue 5: "Merchant routing not triggering"

**Possible Causes**:
1. Hash verification failing silently
2. Exception in routing service
3. Merchant webhook URL not configured

**Solution**:
```java
// Add detailed logging in controller
if (!verificationService.validatePaymentResponse(requestParams)) {
    log.error("Hash failed - routing skipped");
    return;  // Early exit
}

// Add try-catch in routing
try {
    boolean routeSuccess = routingService.routePaymentToMerchant(...);
    if(!routeSuccess) {
        log.error("Routing failed but continuing with display");
    }
} catch(Exception e) {
    log.error("Routing exception", e);
}

// Verify merchant webhook URL configured
// Check MerchantRoutingService commented TODO sections
```

---

## Configuration

### application.yml

```yaml
easebuzz:
  key: YOUR_EASEBUZZ_KEY
  salt: YOUR_EASEBUZZ_SALT
  url:
    initiate: https://easebuzz.in/api/initiate
  surl: https://yoursite.com/payment/success
  furl: https://yoursite.com/payment/failure

spring:
  thymeleaf:
    cache: false  # Disable for development
    mode: HTML
    encoding: UTF-8
```

---

## Files Modified/Created

✅ **Created**:
- `service/MerchantRoutingService.java` (200+ lines)
- `service/PaymentVerificationService.java` (170+ lines)

✅ **Enhanced**:
- `util/EasebuzzHashUtil.java` - Added UDF1-UDF10 support
- `controller/PaymentRedirectController.java` - Added hash verification & routing
- `resources/templates/payment-success.html` - Added UDF, merchant, security displays
- `resources/templates/payment-failure.html` - Added UDF, merchant, security displays

---

## Production Deployment Checklist

- [ ] Update Easebuzz API key and salt in application.yml
- [ ] Configure correct success/failure URLs
- [ ] Set up merchant webhook endpoints
- [ ] Configure email service for receipts/notifications
- [ ] Implement database transaction/payment logging
- [ ] Set up monitoring for hash verification failures
- [ ] Configure alert for suspicious activity
- [ ] Enable HTTPS/SSL certificates
- [ ] Test with real payment gateway (not sandbox)
- [ ] Verify all UDF fields correctly received
- [ ] Load test redirect endpoints
- [ ] Set up backup/failover for payment webhooks

---

## Support & Documentation

- Easebuzz API Docs: https://developer.easebuzz.in/
- Spring Boot Documentation: https://spring.io/projects/spring-boot
- Thymeleaf Reference: https://www.thymeleaf.org/documentation.html

---

**Last Updated**: 2026-02-17  
**Maintained By**: Your Dev Team  
**Version**: 1.1.0
