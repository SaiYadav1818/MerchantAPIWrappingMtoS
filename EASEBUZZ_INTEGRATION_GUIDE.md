# Easebuzz Payment Integration Guide

## 1. Correct Hash Generation - SHA-512

### Forward Hash (Payment Initiation)
**Format:**
```
key|txnid|amount|productinfo|firstname|email|udf1|udf2|udf3|udf4|udf5|||||salt
```

**Java Implementation:**
```java
String hash = EasebuzzHashUtil.generateHash(
    merchant.getMerchantKey(),              // key
    txnId,                                  // txnid
    amount.toString(),                      // amount
    productInfo,                            // productinfo
    firstName,                              // firstname
    email,                                  // email
    internalToken,                          // udf1 (IMPORTANT: internal_token for tracking)
    "",                                     // udf2
    "",                                     // udf3
    "",                                     // udf4
    "",                                     // udf5
    merchant.getSaltKey()                   // salt
);
```

**Key Points:**
- `key`: Merchant key from Easebuzz (NOT global config key)
- `txnid`: Transaction ID (must be unique)
- `amount`: In decimal format (e.g., "100.00")
- `productinfo`: Description of the product/service
- `firstname`: Customer first name
- `email`: Customer email
- `udf1`: **Internal_token** for tracking in callback
- Empty pipes `|||`: Three empty fields before salt
- `salt`: Merchant salt from Easebuzz (NOT global config salt)

### Reverse Hash (Callback Verification)
**Format:**
```
salt|status||||||||email|firstname|productinfo|amount|txnid|key
```

**Java Implementation:**
```java
String reverseHash = EasebuzzHashUtil.generateReverseHash(
    merchant.getSaltKey(),              // salt
    status,                             // status from callback
    email,                              // email from callback
    firstName,                          // firstname from callback
    productInfo,                        // productinfo from callback
    amount,                             // amount from callback
    txnId,                              // txnid from callback
    merchantKey                         // key from callback
);
```

---

## 2. Correct Easebuzz Payload Structure

### Request Format (Form URL-Encoded)
```
key=merchant_key&
txnid=UNIQUE_TXN_ID&
amount=100.00&
productinfo=Order Description&
firstname=John&
email=john@example.com&
phone=9876543210&
hash=SHA512_HASH&
surl=http://yourserver.com/callback/success&
furl=http://yourserver.com/callback/failure&
udf1=internal_token
```

### Response Format (JSON)
```json
{
  "status": 1,
  "data": "payment_page_access_key",
  "message": "Request Processed Successfully"
}
```

**OR (Failure):**
```json
{
  "status": 0,
  "message": "Merchant key is not valid or not found"
}
```

---

## 3. Complete Service Layer Implementation

### Step 1: Merchant Hash Verification
```java
@Transactional
public PaymentResponse initiatePayment(PaymentInitiateRequest request) {
    // 1. Find merchant
    Merchant merchant = merchantRepository
        .findByMerchantId(request.getMerchantId())
        .orElseThrow(() -> new MerchantNotFoundException("Merchant not found"));
    
    // 2. Verify merchant is active
    if (!merchant.getStatus().equals(MerchantStatus.ACTIVE)) {
        throw new MerchantNotFoundException("Merchant is not active");
    }
    
    // 3. Verify hash (sha256: merchantId + orderId + amount + salt)
    String hashInput = request.getMerchantId() + request.getOrderId() + 
                      request.getAmount() + merchant.getSaltKey();
    String generatedHash = HashUtil.generateSHA256(hashInput);
    
    if (!generatedHash.equals(request.getHash())) {
        throw new HashMismatchException("Hash verification failed");
    }
    
    // 4. Create internal token
    String internalToken = UUID.randomUUID().toString();
    
    // 5. Save transaction
    Transaction transaction = Transaction.builder()
        .orderId(request.getOrderId())
        .merchantId(request.getMerchantId())
        .amount(request.getAmount())
        .status(TransactionStatus.INITIATED)
        .internalToken(internalToken)
        .build();
    transactionRepository.save(transaction);
    
    return PaymentResponse.builder()
        .status("SUCCESS")
        .internalToken(internalToken)
        .orderId(request.getOrderId())
        .build();
}
```

### Step 2: Easebuzz Payment Initiation
```java
@Transactional
public EasebuzzPaymentResponse initiateEasebuzzPayment(
        EasebuzzPaymentInitiateRequest request,
        String merchantId,
        String internalToken) {
    
    // 1. Fetch merchant from DB
    Merchant merchant = merchantRepository
        .findByMerchantId(merchantId)
        .orElseThrow(() -> new MerchantNotFoundException("Merchant not found"));
    
    // 2. Generate unique txnId
    String txnId = "TXN" + System.currentTimeMillis() + "_" + merchantId;
    
    // 3. Generate Easebuzz hash with CORRECT format
    String hash = EasebuzzHashUtil.generateHash(
        merchant.getMerchantKey(),      // From DB, NOT config
        txnId,
        request.getAmount().toString(),
        request.getProductInfo(),
        request.getFirstName(),
        request.getEmail(),
        internalToken,                  // AS UDF1
        "", "", "", "",
        merchant.getSaltKey()           // From DB, NOT config
    );
    
    // 4. Prepare form data
    MultiValueMap<String, String> formData = new LinkedMultiValueMap<>();
    formData.add("key", merchant.getMerchantKey());
    formData.add("txnid", txnId);
    formData.add("amount", request.getAmount().toString());
    formData.add("productinfo", request.getProductInfo());
    formData.add("firstname", request.getFirstName());
    formData.add("email", request.getEmail());
    formData.add("phone", request.getPhone());
    formData.add("hash", hash);
    formData.add("surl", easebuzzConfig.getSurl());
    formData.add("furl", easebuzzConfig.getFurl());
    formData.add("udf1", internalToken);  // IMPORTANT
    
    // 5. Call Easebuzz API
    HttpHeaders headers = new HttpHeaders();
    headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);
    HttpEntity<MultiValueMap<String, String>> requestEntity = 
        new HttpEntity<>(formData, headers);
    
    ResponseEntity<Map> response = restTemplate.postForEntity(
        easebuzzConfig.getUrl().getInitiate(),
        requestEntity,
        Map.class
    );
    
    // 6. Validate response
    Map<String, Object> responseBody = response.getBody();
    if (responseBody == null || responseBody.get("status") == null) {
        throw new GatewayException("Invalid response from Easebuzz");
    }
    
    int status = Integer.parseInt(responseBody.get("status").toString());
    if (status != 1) {
        String error = responseBody.get("message") != null ? 
                      responseBody.get("message").toString() : "Unknown error";
        throw new GatewayException("Easebuzz returned error: " + error);
    }
    
    // 7. Extract payment URL
    String accessKey = responseBody.get("data").toString();
    String paymentUrl = easebuzzConfig.getUrl().getPayment() + accessKey;
    
    // 8. Return response
    return EasebuzzPaymentResponse.builder()
        .status("SUCCESS")
        .paymentUrl(paymentUrl)
        .txnId(txnId)
        .message("Payment URL generated successfully")
        .build();
}
```

### Step 3: Callback Processing
```java
@Transactional
public boolean processEasebuzzCallback(Map<String, String> callbackParams) {
    // 1. Extract callback parameters
    String txnId = callbackParams.get("txnid");
    String status = callbackParams.get("status");
    String hash = callbackParams.get("hash");
    String email = callbackParams.get("email");
    String firstname = callbackParams.get("firstname");
    String productinfo = callbackParams.get("productinfo");
    String amount = callbackParams.get("amount");
    String key = callbackParams.get("key");
    String udf1 = callbackParams.get("udf1");  // internal_token
    
    // 2. Fetch merchant using key
    Merchant merchant = merchantRepository
        .findByMerchantKey(key)
        .orElseThrow(() -> new MerchantNotFoundException("Merchant not found"));
    
    // 3. Verify reverse hash
    String calculatedHash = EasebuzzHashUtil.generateReverseHash(
        merchant.getSaltKey(),
        status,
        email,
        firstname,
        productinfo,
        amount,
        txnId,
        key
    );
    
    boolean hashValid = hash != null && hash.equals(calculatedHash);
    
    if (!hashValid) {
        logger.error("Hash mismatch in callback for txnId: {}", txnId);
        return false;
    }
    
    // 4. Process payment status
    EasebuzzPayment payment = easebuzzPaymentRepository.findByTxnId(txnId)
        .orElseGet(() -> {
            EasebuzzPayment newPayment = new EasebuzzPayment();
            newPayment.setTxnId(txnId);
            newPayment.setMerchantId(merchant.getMerchantId());
            return newPayment;
        });
    
    payment.setHashValidated(true);
    payment.setGatewayStatus(status);
    payment.setNormalizedStatus(mapStatus(status));
    
    easebuzzPaymentRepository.save(payment);
    
    return true;
}
```

---

## 4. Best Practices for Returning Payment URL

### Frontend Flow
```javascript
// 1. Client calls `/api/payment/initiate`
POST /api/payment/initiate
{
  "merchantId": "MERCHANT123",
  "orderId": "ORDER456",
  "amount": "100.00",
  "hash": "sha256_hash_here"
}

Response:
{
  "status": "SUCCESS",
  "internalToken": "uuid-here",
  "orderId": "ORDER456"
}

// 2. Client calls `/api/payment/payment-url` with internal token
POST /api/payment/payment-url
{
  "internalToken": "uuid-here",
  "productInfo": "Order Description",
  "firstName": "John",
  "email": "john@example.com",
  "phone": "9876543210"
}

Response:
{
  "status": "SUCCESS",
  "paymentUrl": "https://testpay.easebuzz.in/pay/ACCESS_KEY",
  "txnId": "TXN1234567890",
  "orderId": "ORDER456",
  "message": "Payment URL generated successfully"
}

// 3. Frontend redirects user to paymentUrl
window.location.href = response.paymentUrl;
```

### Return Format (JSON Response)
```json
{
  "status": "SUCCESS",
  "paymentUrl": "https://testpay.easebuzz.in/pay/eGJSEWNfSVCxQ5Jz",
  "txnId": "TXN1234567890_MERCHANT123",
  "orderId": "ORDER456",
  "message": "Payment URL generated successfully",
  "timestamp": 1676543210000
}
```

---

## 5. Common Mistakes That Cause FAILURE Response

### ❌ Mistake 1: Using Global Config Keys Instead of Merchant DB Keys
```java
// WRONG ❌
String hash = HashUtil.generateSHA512(
    easebuzzConfig.getKey() +     // Global key from config
    "|" + txnId + "|" + amount + "|" + 
    productinfo + "|" + firstName + "|" + email +
    "|||||||" + easebuzzConfig.getSalt()  // Global salt from config
);

// CORRECT ✅
String hash = EasebuzzHashUtil.generateHash(
    merchant.getMerchantKey(),    // From database
    txnId,
    amount.toString(),
    productinfo,
    firstName,
    email,
    internalToken,
    "", "", "", "",
    merchant.getSaltKey()         // From database
);
```

### ❌ Mistake 2: Wrong Hash String Format
```java
// WRONG ❌ - Missing pipes before salt
String hashString = key + "|" + txnid + "|" + amount + "|" + 
                   productinfo + "|" + firstname + "|" + email + 
                   "|" + salt;  // Only one pipe!

// CORRECT ✅ - Exactly 5 pipes for UDF fields + 3 empty pipes
String hashString = key + "|" + txnid + "|" + amount + "|" + 
                   productinfo + "|" + firstname + "|" + email + 
                   "|udf1|udf2|udf3|udf4|udf5|||||" + salt;
```

### ❌ Mistake 3: Not Including internal_token in Payload
```java
// WRONG ❌
formData.add("key", merchantKey);
formData.add("txnid", txnId);
formData.add("amount", amount);
formData.add("hash", hash);
// Missing udf1!

// CORRECT ✅
formData.add("key", merchantKey);
formData.add("txnid", txnId);
formData.add("amount", amount);
formData.add("hash", hash);
formData.add("udf1", internalToken);  // Add this!
```

### ❌ Mistake 4: Wrong Response Parsing
```java
// WRONG ❌ - Checking for string "1" instead of integer
if ("1".equals(responseBody.get("status"))) {  // String comparison
    accessKey = responseBody.get("data").toString();
}

// CORRECT ✅
int status = Integer.parseInt(responseBody.get("status").toString());
if (status == 1) {  // Integer comparison
    accessKey = responseBody.get("data").toString();
}
```

### ❌ Mistake 5: Not Handling Null Response
```java
// WRONG ❌
Map<String, Object> responseBody = response.getBody();
String accessKey = responseBody.get("data").toString();  // NPE risk

// CORRECT ✅
Map<String, Object> responseBody = response.getBody();
if (responseBody == null || !responseBody.containsKey("data")) {
    throw new GatewayException("Invalid response from Easebuzz");
}
String accessKey = responseBody.get("data").toString();
```

### ❌ Mistake 6: Wrong Callback Hash Verification
```java
// WRONG ❌ - Using forward hash format instead of reverse
String calculatedHash = HashUtil.generateSHA512(
    key + "|" + txnid + "|" + amount + "|" + 
    productinfo + "|" + firstname + "|" + email + 
    "|||||||" + salt  // Forward hash!
);

// CORRECT ✅ - Reverse hash format
String calculatedHash = EasebuzzHashUtil.generateReverseHash(
    salt,
    status,
    email,
    firstname,
    productinfo,
    amount,
    txnid,
    key
);
```

### ❌ Mistake 7: Not Using Application Config Values
```java
// WRONG ❌ - Hardcoded URLs
String surl = "http://localhost:8080/callback/success";
String furl = "http://localhost:8080/callback/failure";

// CORRECT ✅ - Use application config
formData.add("surl", easebuzzConfig.getSurl());
formData.add("furl", easebuzzConfig.getFurl());
```

### ❌ Mistake 8: Amount Format Issues
```java
// WRONG ❌
formData.add("amount", request.getAmount());  // Could be BigDecimal

// CORRECT ✅
formData.add("amount", request.getAmount().toString());  // Convert to String
```

---

## 6. Debug Checklist

When getting FAILURE response, check:

✓ **Merchant Key & Salt**
```java
Merchant merchant = merchantRepository.findByMerchantId(merchantId);
System.out.println("Key: " + merchant.getMerchantKey());      // Should NOT be null
System.out.println("Salt: " + merchant.getSaltKey());        // Should NOT be null
```

✓ **Hash Generation**
```java
String hash = EasebuzzHashUtil.generateHash(...);
logger.info("Generated hash: {}", hash);  // Should be 128 char hex string
```

✓ **Payload Content**
```java
logger.debug("Request payload: {}", formData);  // Log all fields
```

✓ **Response Treatment**
```java
logger.debug("Easebuzz response: {}", responseBody);  // Log raw response
```

✓ **Network Issues**
```java
// Add timeout to RestTemplate
RestTemplate restTemplate = new RestTemplate();
restTemplate.setRequestFactory(new HttpComponentsClientHttpRequestFactory() {{
    getHttpClient().getParams().setParameter(
        "http.socket.timeout", 5000
    );
}});
```

---

## 7. Configuration Example

**application.yml:**
```yaml
easebuzz:
  # These are GLOBAL defaults if merchant not found
  key: ZF93ZSH6B
  salt: 2Y1MFHGIB
  
  url:
    initiate: https://testpay.easebuzz.in/payment/initiateLink
    payment: https://testpay.easebuzz.in/pay/
  
  timeout: 5000
  surl: http://yourdomain.com/api/payment/easebuzz/callback/success
  furl: http://yourdomain.com/api/payment/easebuzz/callback/failure
```

**Database Merchant Table:**
```sql
INSERT INTO merchants (merchant_id, merchant_name, merchant_key, salt_key, status) 
VALUES (
    'MERCHANT123',
    'My Business',
    'ACTUAL_KEY_FROM_EASEBUZZ',      -- Use actual key from Easebuzz dashboard
    'ACTUAL_SALT_FROM_EASEBUZZ',     -- Use actual salt from Easebuzz dashboard
    'ACTIVE'
);
```

---

## 8. Testing the Flow

### cURL Command:
```bash
# 1. Initiate Payment
curl -X POST http://localhost:8080/api/payment/initiate \
  -H "Content-Type: application/json" \
  -d '{
    "merchantId": "MERCHANT123",
    "orderId": "ORDER456",
    "amount": "100.00",
    "hash": "sha256_hash_computed_on_client"
  }'

# Response includes: internalToken

# 2. Get Payment URL
curl -X POST http://localhost:8080/api/payment/payment-url \
  -H "Content-Type: application/json" \
  -d '{
    "internalToken": "uuid-from-step1",
    "productInfo": "Order #456",
    "firstName": "John",
    "email": "john@example.com",
    "phone": "9876543210"
  }'

# Response includes: paymentUrl - redirect user to this URL
```

---

## Summary

| Aspect | Details |
|--------|---------|
| **Hash Algorithm** | SHA-512 (hexadecimal) |
| **Forward Hash Format** | `key\|txnid\|amount\|productinfo\|firstname\|email\|udf1\|udf2\|udf3\|udf4\|udf5\|\|\|\|salt` |
| **Reverse Hash Format** | `salt\|status\|\|\|\|\|\|\|\|email\|firstname\|productinfo\|amount\|txnid\|key` |
| **Payment Flow** | Merchant → Sabbpe (verify hash) → Easebuzz API → Get payment URL |
| **Internal Token** | Use as `udf1` in Easebuzz payload for tracking |
| **Keys/Salt Source** | Always from Merchant table in DB, never from global config |
| **Response Format** | JSON with `status: 1` (success) or `status: 0` (failure) |
| **Callback Verification** | Always validate reverse hash before updating payment status |
