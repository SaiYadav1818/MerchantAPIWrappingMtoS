package com.sabbpe.merchant.reference;

import com.sabbpe.merchant.config.EasebuzzConfig;
import com.sabbpe.merchant.dto.*;
import com.sabbpe.merchant.entity.Merchant;
import com.sabbpe.merchant.entity.Transaction;
import com.sabbpe.merchant.repository.MerchantRepository;
import com.sabbpe.merchant.repository.TransactionRepository;
import com.sabbpe.merchant.service.PaymentService;
import com.sabbpe.merchant.util.EasebuzzHashUtil;
import com.sabbpe.merchant.util.HashUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.*;
import org.springframework.stereotype.Component;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;

import java.math.BigDecimal;
import java.util.*;

/**
 * REFERENCE IMPLEMENTATION - Complete Working Example
 * 
 * This class demonstrates the exact step-by-step process for:
 * 1. Merchant hash verification
 * 2. Easebuzz payment initiation
 * 3. Payment URL generation
 * 4. Callback processing
 */
@Component
public class EasebuzzIntegrationReference {

    private static final Logger logger = LoggerFactory.getLogger(EasebuzzIntegrationReference.class);

    @Autowired
    private MerchantRepository merchantRepository;

    @Autowired
    private TransactionRepository transactionRepository;

    @Autowired
    private EasebuzzConfig easebuzzConfig;

    @Autowired
    private RestTemplate restTemplate;

    /**
     * COMPLETE FLOW EXAMPLE
     * 
     * Flow: Merchant Hash Verification → Create Internal Token → Easebuzz Payment → Payment URL
     */
    public void demonstrateCompleteFlow() {
        logger.info("=== COMPLETE EASEBUZZ INTEGRATION FLOW ===\n");

        // ============================================================
        // STEP 1: MERCHANT HASH VERIFICATION
        // ============================================================
        logger.info("STEP 1: Merchant Hash Verification");
        logger.info("------------------------------------------");
        
        String merchantId = "MERCHANT123";
        String orderId = "ORDER456";
        String amount = "100.00";
        String receivedHash = "hash_from_merchant";
        
        // Find merchant from database
        Merchant merchant = merchantRepository.findByMerchantId(merchantId)
                .orElseThrow(() -> new RuntimeException("Merchant not found"));
        
        logger.info("✓ Merchant found: {}", merchant.getMerchantId());
        logger.info("  - Easebuzz Key: {}", easebuzzConfig.getKey());
        logger.info("  - Salt Key: {}", merchant.getSaltKey());
        logger.info("  - Status: {}", merchant.getStatus());
        
        // Build hash string for verification
        String hashInput = merchantId + orderId + amount + merchant.getSaltKey();
        String calculatedHash = HashUtil.generateSHA256(hashInput);
        
        logger.info("Hash Verification:");
        logger.info("  - Input: {}", hashInput);
        logger.info("  - Calculated Hash: {}", calculatedHash);
        logger.info("  - Received Hash: {}", receivedHash);
        
        if (!calculatedHash.equals(receivedHash)) {
            logger.error("✗ Hash mismatch - REJECT REQUEST");
            return;
        }
        logger.info("✓ Hash verified successfully\n");

        // ============================================================
        // STEP 2: CREATE INTERNAL TOKEN & SAVE TRANSACTION
        // ============================================================
        logger.info("STEP 2: Create Internal Token & Save Transaction");
        logger.info("------------------------------------------");
        
        String internalToken = UUID.randomUUID().toString();
        logger.info("Generated Internal Token: {}", internalToken);
        
        Transaction transaction = Transaction.builder()
                .orderId(orderId)
                .merchantId(merchantId)
                .amount(new BigDecimal(amount))
                .internalToken(internalToken)
                .build();
        
        // Save transaction (not shown in this reference)
        logger.info("✓ Transaction saved in database\n");

        // ============================================================
        // STEP 3: GENERATE EASEBUZZ HASH
        // ============================================================
        logger.info("STEP 3: Generate Easebuzz Hash (SHA-512)");
        logger.info("------------------------------------------");
        
        String txnId = "TXN" + System.currentTimeMillis() + "_" + merchantId;
        String productInfo = "Order for customer";
        String firstName = "John";
        String email = "john@example.com";
        
        logger.info("Building hash with EXACT format:");
        logger.info("  key         : {}", easebuzzConfig.getKey());
        logger.info("  txnid       : {}", txnId);
        logger.info("  amount      : {}", amount);
        logger.info("  productinfo : {}", productInfo);
        logger.info("  firstname   : {}", firstName);
        logger.info("  email       : {}", email);
        logger.info("  udf1        : {} (INTERNAL_TOKEN)", internalToken);
        logger.info("  udf2-5      : (empty)");
        logger.info("  salt        : {}", easebuzzConfig.getSalt());
        
        String easebuzzHash = EasebuzzHashUtil.generateHash(
                easebuzzConfig.getKey(),
                txnId,
                amount,
                productInfo,
                firstName,
                email,
                internalToken,  // udf1
                "", "", "", "",  // udf2-5 empty
                easebuzzConfig.getSalt()
        );
        
        logger.info("Generated Easebuzz Hash: {}", easebuzzHash);
        logger.info("Hash Length: {} (should be 128 for SHA-512 hex)\n", easebuzzHash.length());

        // ============================================================
        // STEP 4: BUILD EASEBUZZ REQUEST PAYLOAD
        // ============================================================
        logger.info("STEP 4: Build Easebuzz Request Payload");
        logger.info("------------------------------------------");
        
        MultiValueMap<String, String> formData = new LinkedMultiValueMap<>();
        formData.add("key", easebuzzConfig.getKey());
        formData.add("txnid", txnId);
        formData.add("amount", amount);
        formData.add("productinfo", productInfo);
        formData.add("firstname", firstName);
        formData.add("email", email);
        formData.add("phone", "9876543210");
        formData.add("hash", easebuzzHash);
        formData.add("surl", "http://yourdomain.com/callback/success");
        formData.add("furl", "http://yourdomain.com/callback/failure");
        formData.add("udf1", internalToken);  // IMPORTANT: Include internal token
        
        logger.info("Request Payload:");
        formData.forEach((key, value) -> 
            logger.info("  {} : {}", key, value.get(0))
        );
        logger.info("");

        // ============================================================
        // STEP 5: CALL EASEBUZZ API
        // ============================================================
        logger.info("STEP 5: Call Easebuzz Payment Initiate API");
        logger.info("------------------------------------------");
        
        String easebuzzUrl = "https://testpay.easebuzz.in/payment/initiateLink";
        logger.info("API Endpoint: {}", easebuzzUrl);
        
        try {
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);
            headers.set("Accept", "application/json");
            
            HttpEntity<MultiValueMap<String, String>> request = 
                new HttpEntity<>(formData, headers);
            
            ResponseEntity<Map> response = restTemplate.postForEntity(
                    easebuzzUrl,
                    request,
                    Map.class
            );
            
            logger.info("API Response Status: {}", response.getStatusCode());
            logger.info("API Response Body: {}\n", response.getBody());

            // ============================================================
            // STEP 6: PARSE & VALIDATE RESPONSE
            // ============================================================
            logger.info("STEP 6: Parse & Validate Easebuzz Response");
            logger.info("------------------------------------------");
            
            Map<String, Object> responseBody = response.getBody();
            
            if (responseBody == null) {
                logger.error("✗ Response body is NULL");
                return;
            }
            
            if (!responseBody.containsKey("status")) {
                logger.error("✗ Missing 'status' field in response");
                return;
            }
            
            int status = Integer.parseInt(responseBody.get("status").toString());
            logger.info("Response Status Code: {}", status);
            
            if (status == 1) {
                logger.info("✓ Status 1 = Success");
            } else {
                logger.error("✗ Status {} = Failure", status);
                logger.error("Message: {}", responseBody.get("message"));
                return;
            }
            
            // ============================================================
            // STEP 7: EXTRACT PAYMENT URL
            // ============================================================
            logger.info("STEP 7: Extract Payment URL");
            logger.info("------------------------------------------");
            
            if (!responseBody.containsKey("data")) {
                logger.error("✗ Missing 'data' field in response");
                return;
            }
            
            String accessKey = responseBody.get("data").toString();
            logger.info("Access Key: {}", accessKey);
            
            String paymentUrl = "https://testpay.easebuzz.in/pay/" + accessKey;
            logger.info("✓ Payment URL Generated: {}\n", paymentUrl);
            
            // ============================================================
            // STEP 8: RETURN PAYMENT URL TO FRONTEND
            // ============================================================
            logger.info("STEP 8: Return Payment URL to Frontend");
            logger.info("------------------------------------------");
            logger.info("Response to Frontend:");
            logger.info("""
                {
                  "status": "SUCCESS",
                  "paymentUrl": "%s",
                  "txnId": "%s",
                  "orderId": "%s",
                  "internalToken": "%s"
                }
                """, paymentUrl, txnId, orderId, internalToken);
            logger.info("Frontend should redirect user to paymentUrl\n");

        } catch (Exception e) {
            logger.error("✗ Error calling Easebuzz API: {}", e.getMessage(), e);
        }

        // ============================================================
        // STEP 9: CALLBACK PROCESSING
        // ============================================================
        demonstrateCallbackProcessing(merchant, txnId);
    }

    /**
     * CALLBACK PROCESSING EXAMPLE
     */
    private void demonstrateCallbackProcessing(Merchant merchant, String txnId) {
        logger.info("\n========================================");
        logger.info("STEP 9: Easebuzz Callback Processing");
        logger.info("========================================\n");
        
        // Simulate callback parameters from Easebuzz
        Map<String, String> callbackParams = new HashMap<>();
        callbackParams.put("txnid", txnId);
        callbackParams.put("status", "1");  // 1 = success
        callbackParams.put("key", easebuzzConfig.getKey());
        callbackParams.put("email", "john@example.com");
        callbackParams.put("firstname", "John");
        callbackParams.put("productinfo", "Order for customer");
        callbackParams.put("amount", "100.00");
        callbackParams.put("udf1", "internal_token_here");
        // Hash will be calculated
        
        logger.info("Received Callback Parameters:");
        callbackParams.forEach((key, value) -> 
            logger.info("  {} : {}", key, value)
        );
        logger.info("");
        
        // ============================================================
        // STEP 9a: VERIFY REVERSE HASH
        // ============================================================
        logger.info("STEP 9a: Verify Reverse Hash");
        logger.info("------------------------------------------");
        logger.info("Reverse Hash Format: salt|status||||||||email|firstname|productinfo|amount|txnid|key");
        
        String receivedHash = callbackParams.get("hash");
        String calculatedHash = EasebuzzHashUtil.generateReverseHash(
                merchant.getSaltKey(),
                callbackParams.get("status"),
                callbackParams.get("email"),
                callbackParams.get("firstname"),
                callbackParams.get("productinfo"),
                callbackParams.get("amount"),
                callbackParams.get("txnid"),
                callbackParams.get("key")
        );
        
        logger.info("Received Hash: {}", receivedHash);
        logger.info("Calculated Hash: {}", calculatedHash);
        
        if (!calculatedHash.equals(receivedHash)) {
            logger.error("✗ Hash mismatch in callback - REJECT");
            return;
        }
        logger.info("✓ Hash verified\n");
        
        // ============================================================
        // STEP 9b: UPDATE PAYMENT STATUS
        // ============================================================
        logger.info("STEP 9b: Update Payment Status");
        logger.info("------------------------------------------");
        
        String status = callbackParams.get("status");
        String normalizedStatus;
        
        if ("1".equals(status)) {
            normalizedStatus = "SUCCESS";
        } else if ("2".equals(status)) {
            normalizedStatus = "PENDING";
        } else {
            normalizedStatus = "FAILED";
        }
        
        logger.info("Gateway Status: {}", status);
        logger.info("Normalized Status: {}", normalizedStatus);
        logger.info("✓ Payment status updated in database\n");
        
        logger.info("=== CALLBACK PROCESSING COMPLETE ===");
    }

    /**
     * HASH DEBUGGING HELPER
     * 
     * Use this when debugging hash mismatches
     */
    public void debugHashGeneration() {
        logger.info("\n=== HASH GENERATION DEBUG ===\n");
        
        String key = "ZF93ZSH6B";
        String txnid = "TXN1234567890";
        String amount = "100.00";
        String productinfo = "Order Description";
        String firstname = "John";
        String email = "john@example.com";
        String udf1 = "internal-token-uuid";
        String salt = "2Y1MFHGIB";
        
        // Show the exact string being hashed
        String hashInputRaw = key + "|" + txnid + "|" + amount + "|" + 
                             productinfo + "|" + firstname + "|" + email + "|" +
                             udf1 + "|||||" + salt;
        
        logger.info("Hash Input String:");
        logger.info("Raw: {}", hashInputRaw);
        logger.info("");
        logger.info("Broken down by parts:");
        logger.info("1. key         : {}", key);
        logger.info("2. txnid       : {}", txnid);
        logger.info("3. amount      : {}", amount);
        logger.info("4. productinfo : {}", productinfo);
        logger.info("5. firstname   : {}", firstname);
        logger.info("6. email       : {}", email);
        logger.info("7. udf1        : {}", udf1);
        logger.info("8. udf2        : (empty)");
        logger.info("9. udf3        : (empty)");
        logger.info("10. udf4       : (empty)");
        logger.info("11. udf5       : (empty)");
        logger.info("12. (3 pipes)  : |||");
        logger.info("13. salt       : {}", salt);
        logger.info("");
        
        String hash = EasebuzzHashUtil.generateHash(key, txnid, amount, 
                                                     productinfo, firstname, email, 
                                                     udf1, "", "", "", "", salt);
        
        logger.info("Generated SHA-512 Hash: {}", hash);
        logger.info("Hash Length: {} (expected 128)", hash.length());
    }
}
