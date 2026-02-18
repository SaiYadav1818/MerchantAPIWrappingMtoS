package com.sabbpe.merchant.service;

import com.sabbpe.merchant.util.EasebuzzHashUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.Map;

/**
 * Service for payment verification and hash validation
 * 
 * Responsibilities:
 * - Verify payment hash from callback/redirect responses
 * - Compare incoming hash with generated hash
 * - Detect tampering or replay attacks
 * - Log suspicious activities
 */
@Slf4j
@Service
public class PaymentVerificationService {

    @Value("${easebuzz.salt}")
    private String easebuzzSalt;

    @Value("${easebuzz.key}")
    private String easebuzzKey;

    private final EasebuzzHashUtil hashUtil;

    @Autowired
    public PaymentVerificationService(EasebuzzHashUtil hashUtil) {
        this.hashUtil = hashUtil;
    }

    /**
     * Verify payment response hash (callback/redirect from Easebuzz)
     * 
     * Verification Steps:
     * 1. Extract hash from response
     * 2. Rebuild hash using gateway reverse formula
     * 3. Compare both hashes
     * 4. Log security events
     * 
     * Reverse Hash Format:
     * salt|status|udf10|udf9|udf8|udf7|udf6|udf5|udf4|udf3|udf2|udf1|email|firstname|productinfo|amount|txnid|key
     * 
     * @param paymentData Response data from gateway containing all parameters
     * @return true if hash is valid, false if tampering detected
     */
    public boolean verifyPaymentHash(Map<String, String> paymentData) {
        try {
            // Step 1: Extract incoming hash from response
            String incomingHash = paymentData.get("hash");
            if (incomingHash == null || incomingHash.isEmpty()) {
                log.error("Hash missing in payment response. This is a security concern!");
                return false;
            }

            log.debug("Verifying payment hash. Incoming hash: {}...", incomingHash.substring(0, Math.min(10, incomingHash.length())));

            // Step 2: Rebuild reverse hash using gateway formula
            String calculatedHash = generateReverseHashFromResponse(paymentData);

            // Step 3: Compare hashes
            boolean isHashValid = incomingHash.equalsIgnoreCase(calculatedHash);

            if (isHashValid) {
                log.info("Payment hash verification SUCCESS. txnid: {}, status: {}",
                        paymentData.get("txnid"), paymentData.get("status"));
                return true;
            } else {
                log.error("Payment hash verification FAILED - TAMPERING DETECTED!");
                log.error("Expected hash: {}", calculatedHash);
                log.error("Received hash: {}", incomingHash);
                log.error("Transaction details - txnid: {}, status: {}, amount: {}",
                        paymentData.get("txnid"), paymentData.get("status"), paymentData.get("amount"));
                
                // Log all parameters for forensics
                logSuspiciousActivity(paymentData, calculatedHash, incomingHash);
                return false;
            }

        } catch (Exception e) {
            log.error("Error verifying payment hash: {}", e.getMessage(), e);
            return false;
        }
    }

    /**
     * Generate reverse hash using Easebuzz formula
     * This matches what the gateway sends back
     * 
     * @param paymentData Payment response data
     * @return Generated reverse hash
     */
    private String generateReverseHashFromResponse(Map<String, String> paymentData) {
        String status = paymentData.get("status");
        String email = paymentData.get("email");
        String firstname = paymentData.get("firstname");
        String productinfo = paymentData.get("productinfo");
        String amount = paymentData.get("amount");
        String txnid = paymentData.get("txnid");
        
        String udf1 = paymentData.getOrDefault("udf1", "");
        String udf2 = paymentData.getOrDefault("udf2", "");
        String udf3 = paymentData.getOrDefault("udf3", "");
        String udf4 = paymentData.getOrDefault("udf4", "");
        String udf5 = paymentData.getOrDefault("udf5", "");
        String udf6 = paymentData.getOrDefault("udf6", "");
        String udf7 = paymentData.getOrDefault("udf7", "");
        String udf8 = paymentData.getOrDefault("udf8", "");
        String udf9 = paymentData.getOrDefault("udf9", "");
        String udf10 = paymentData.getOrDefault("udf10", "");

        log.debug("Generating reverse hash with parameters. status: {}, txnid: {}, amount: {}",
                status, txnid, amount);

        return EasebuzzHashUtil.generateReverseHashWithUDF(
                easebuzzSalt,
                status,
                udf10, udf9, udf8, udf7, udf6, udf5, udf4, udf3, udf2, udf1,
                email, firstname, productinfo, amount, txnid, easebuzzKey
        );
    }

    /**
     * Log suspicious activity for security audit
     * 
     * @param paymentData All payment parameters
     * @param expectedHash The hash we calculated
     * @param receivedHash The hash received from gateway
     */
    private void logSuspiciousActivity(Map<String, String> paymentData, String expectedHash, String receivedHash) {
        try {
            log.warn("=== SECURITY ALERT: SUSPICIOUS PAYMENT ACTIVITY ===");
            log.warn("Transaction ID: {}", paymentData.get("txnid"));
            log.warn("Status: {}", paymentData.get("status"));
            log.warn("Amount: {}", paymentData.get("amount"));
            log.warn("Email: {}", paymentData.get("email"));
            log.warn("Merchant ID (UDF1): {}", paymentData.get("udf1"));
            log.warn("Hash Mismatch - Expected: {}", expectedHash);
            log.warn("Hash Mismatch - Received: {}", receivedHash);
            log.warn("All Parameters: {}", paymentData);
            log.warn("=================================================");
            
            // TODO: Send alert to security team / administrator
            // TODO: Block this transaction
            // TODO: Save to security_audit table
            
        } catch (Exception e) {
            log.error("Error logging suspicious activity: {}", e.getMessage(), e);
        }
    }

    /**
     * Validate payment response contains all required fields
     * 
     * @param paymentData Response data
     * @return true if all required fields present
     */
    public boolean validateRequiredFields(Map<String, String> paymentData) {
        String[] requiredFields = {"txnid", "status", "email", "firstname", "amount", "hash"};
        
        for (String field : requiredFields) {
            if (!paymentData.containsKey(field) || paymentData.get(field).isEmpty()) {
                log.error("Required field missing in payment response: {}", field);
                return false;
            }
        }
        
        log.debug("All required fields present in payment response");
        return true;
    }

    /**
     * Verify hash AND required fields
     * Composite check for complete validation
     * 
     * @param paymentData Payment response
     * @return true if all validations pass
     */
    public boolean validatePaymentResponse(Map<String, String> paymentData) {
        // Check 1: Required fields
        if (!validateRequiredFields(paymentData)) {
            log.error("Payment response failed required fields validation");
            return false;
        }

        // Check 2: Hash verification
        if (!verifyPaymentHash(paymentData)) {
            log.error("Payment response failed hash verification");
            return false;
        }

        log.info("Payment response validation PASSED. txnid: {}, status: {}",
                paymentData.get("txnid"), paymentData.get("status"));
        return true;
    }
}
