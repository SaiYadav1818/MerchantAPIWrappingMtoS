package com.sabbpe.merchant.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.sabbpe.merchant.entity.PaymentTransaction;
import com.sabbpe.merchant.repository.PaymentTransactionRepository;
import com.sabbpe.merchant.util.EasebuzzHashUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.Map;
import java.util.Optional;

/**
 * Payment Processing Service
 * 
 * Handles complete payment processing workflow:
 * 1. Receive payment gateway response
 * 2. Verify hash signature
 * 3. Extract and map fields
 * 4. Store in database
 * 5. Support idempotent operations (duplicate handling)
 * 
 * Thread-safe and transaction-aware
 */
@Slf4j
@Service
@Transactional
public class PaymentProcessingService {

    @Autowired
    private PaymentTransactionRepository paymentTransactionRepository;

    @Autowired
    private ObjectMapper objectMapper;

    @Value("${easebuzz.key:}")
    private String easebuzzKey;

    @Value("${easebuzz.salt:}")
    private String easebuzzSalt;

    // ========================================
    // PAYMENT PROCESSING METHODS
    // ========================================

    /**
     * Process payment success response from gateway
     * 
     * Workflow:
     * 1. Extract transaction ID
     * 2. Verify hash signature
     * 3. Check for duplicate/existing transaction
     * 4. Extract all fields from response
     * 5. Store in database
     * 
     * @param paymentResponse Map of all gateway response parameters
     * @return PaymentTransaction entity from database
     * @throws IllegalArgumentException if txnid missing or invalid
     */
    public PaymentTransaction processSuccessResponse(Map<String, String> paymentResponse) {
        log.info("========== PROCESSING SUCCESS RESPONSE ==========");
        
        // Extract transaction ID
        String txnid = paymentResponse.get("txnid");
        if (txnid == null || txnid.trim().isEmpty()) {
            log.error("ERROR: txnid missing in payment response");
            throw new IllegalArgumentException("Transaction ID (txnid) is required");
        }

        log.info("Processing success for txnid: {}", txnid);

        try {
            // Verify hash
            boolean hashValid = verifyPaymentHash(paymentResponse);
            log.info("Hash verification result: {}", hashValid ? "VALID" : "INVALID");

            // Check for existing transaction (idempotent operation)
            Optional<PaymentTransaction> existingTxn = paymentTransactionRepository.findByTxnid(txnid);
            
            PaymentTransaction transaction;
            if (existingTxn.isPresent()) {
                log.info("Updating existing transaction for txnid: {}", txnid);
                transaction = existingTxn.get();
            } else {
                log.info("Creating new transaction for txnid: {}", txnid);
                transaction = PaymentTransaction.builder().build();
                transaction.setTxnid(txnid);
            }

            // Extract and set all fields
            enrichTransactionFromResponse(transaction, paymentResponse, "SUCCESS", hashValid);

            // Store raw response as JSON
            storeRawResponse(transaction, paymentResponse);

            // Save to database
            PaymentTransaction savedTxn = paymentTransactionRepository.save(transaction);
            log.info("Transaction successfully saved. ID: {}, Status: SUCCESS", savedTxn.getId());

            return savedTxn;

        } catch (Exception e) {
            log.error("ERROR processing success response for txnid: {}", txnid, e);
            throw e;
        }
    }

    /**
     * Process payment failure response from gateway
     * 
     * @param paymentResponse Map of all gateway response parameters
     * @return PaymentTransaction entity from database
     * @throws IllegalArgumentException if txnid missing
     */
    public PaymentTransaction processFailureResponse(Map<String, String> paymentResponse) {
        log.info("========== PROCESSING FAILURE RESPONSE ==========");
        
        String txnid = paymentResponse.get("txnid");
        if (txnid == null || txnid.trim().isEmpty()) {
            log.error("ERROR: txnid missing in payment response");
            throw new IllegalArgumentException("Transaction ID (txnid) is required");
        }

        log.warn("Processing failure for txnid: {}", txnid);

        try {
            // Verify hash
            boolean hashValid = verifyPaymentHash(paymentResponse);
            log.warn("Hash verification result for failed payment: {}", hashValid ? "VALID" : "INVALID");

            // Check for existing transaction
            Optional<PaymentTransaction> existingTxn = paymentTransactionRepository.findByTxnid(txnid);
            
            PaymentTransaction transaction;
            if (existingTxn.isPresent()) {
                log.info("Updating existing failed transaction for txnid: {}", txnid);
                transaction = existingTxn.get();
            } else {
                log.info("Creating new failed transaction for txnid: {}", txnid);
                transaction = PaymentTransaction.builder().build();
                transaction.setTxnid(txnid);
            }

            // Extract and set all fields
            enrichTransactionFromResponse(transaction, paymentResponse, "FAILED", hashValid);

            // Store raw response as JSON
            storeRawResponse(transaction, paymentResponse);

            // Save to database
            PaymentTransaction savedTxn = paymentTransactionRepository.save(transaction);
            log.warn("Failed transaction successfully saved. ID: {}, Status: FAILED", savedTxn.getId());

            return savedTxn;

        } catch (Exception e) {
            log.error("ERROR processing failure response for txnid: {}", txnid, e);
            throw e;
        }
    }

    // ========================================
    // HASH VERIFICATION
    // ========================================

    /**
     * Verify payment hash signature
     * 
     * Reverse hash formula (for verification):
     * hash = SHA512(salt|status|udf10|udf9|...|udf1|email|firstname|productinfo|amount|txnid|key)
     * 
     * @param paymentResponse Payment response map
     * @return true if hash is valid, false if tampered
     */
    private boolean verifyPaymentHash(Map<String, String> paymentResponse) {
        try {
            String receivedHash = paymentResponse.get("hash");
            if (receivedHash == null || receivedHash.isEmpty()) {
                log.warn("Hash field missing in payment response");
                return false;
            }

            // Extract fields for hash verification
            String salt = easebuzzSalt;
            String status = paymentResponse.getOrDefault("status", "");
            String udf10 = paymentResponse.getOrDefault("udf10", "");
            String udf9 = paymentResponse.getOrDefault("udf9", "");
            String udf8 = paymentResponse.getOrDefault("udf8", "");
            String udf7 = paymentResponse.getOrDefault("udf7", "");
            String udf6 = paymentResponse.getOrDefault("udf6", "");
            String udf5 = paymentResponse.getOrDefault("udf5", "");
            String udf4 = paymentResponse.getOrDefault("udf4", "");
            String udf3 = paymentResponse.getOrDefault("udf3", "");
            String udf2 = paymentResponse.getOrDefault("udf2", "");
            String udf1 = paymentResponse.getOrDefault("udf1", "");
            String email = paymentResponse.getOrDefault("email", "");
            String firstname = paymentResponse.getOrDefault("firstname", "");
            String productinfo = paymentResponse.getOrDefault("productinfo", "");
            String amount = paymentResponse.getOrDefault("amount", "");
            String txnid = paymentResponse.getOrDefault("txnid", "");
            String key = easebuzzKey;

            // Generate hash using utility
            String calculatedHash = EasebuzzHashUtil.generateReverseHashWithUDF(
                salt, status, udf10, udf9, udf8, udf7, udf6, udf5, udf4, udf3, udf2, udf1,
                email, firstname, productinfo, amount, txnid, key
            );

            boolean isValid = calculatedHash.equalsIgnoreCase(receivedHash);
            log.debug("Hash verification - Received: {}, Calculated: {}, Valid: {}",
                receivedHash.substring(0, Math.min(10, receivedHash.length())) + "...",
                calculatedHash.substring(0, Math.min(10, calculatedHash.length())) + "...",
                isValid);

            return isValid;

        } catch (Exception e) {
            log.error("ERROR verifying hash", e);
            return false;
        }
    }

    // ========================================
    // FIELD EXTRACTION AND MAPPING
    // ========================================

    /**
     * Extract all fields from gateway response and populate transaction entity
     * 
     * @param transaction Entity to populate
     * @param response Gateway response map
     * @param statusValue Status to set (SUCCESS/FAILED)
     * @param hashValid Is hash verified
     */
    private void enrichTransactionFromResponse(
            PaymentTransaction transaction,
            Map<String, String> response,
            String statusValue,
            boolean hashValid) {

        log.debug("Enriching transaction with response fields");

        // Gateway transaction fields
        transaction.setStatus(statusValue);
        transaction.setHashVerified(hashValid);
        transaction.setHash(response.get("hash"));

        // Amount
        String amountStr = response.get("amount");
        if (amountStr != null && !amountStr.isEmpty()) {
            try {
                transaction.setAmount(new BigDecimal(amountStr));
            } catch (NumberFormatException e) {
                log.warn("Invalid amount format: {}", amountStr);
            }
        }

        // Payment details
        transaction.setEasepayid(response.get("easepayid"));
        transaction.setBankRefNum(response.get("bank_ref_num"));
        transaction.setBankcode(response.get("bankcode"));
        transaction.setMode(response.get("mode"));
        transaction.setPaymentSource(response.get("payment_source"));

        // Customer information
        transaction.setEmail(response.get("email"));
        transaction.setPhone(response.get("phone"));
        transaction.setFirstname(response.get("firstname"));

        // Product info
        transaction.setProductinfo(response.get("productinfo"));

        // Bank details
        transaction.setBankName(response.get("bank_name"));
        transaction.setIssuingBank(response.get("issuing_bank"));
        transaction.setCardType(response.get("card_type"));
        transaction.setAuthCode(response.get("auth_code"));

        // Error message (if any)
        transaction.setErrorMessage(response.get("error_Message"));

        // UDF fields (1-10)
        for (int i = 1; i <= 10; i++) {
            String udfKey = "udf" + i;
            String udfValue = response.get(udfKey);
            if (udfValue != null) {
                transaction.setUDFValue(i, udfValue);
                log.debug("Set {} = {}", udfKey, udfValue);
            }
        }
    }

    /**
     * Store complete gateway response as JSON string for audit
     * 
     * @param transaction Entity to populate raw_response field
     * @param response Gateway response map
     */
    private void storeRawResponse(PaymentTransaction transaction, Map<String, String> response) {
        try {
            // Filter out null/empty values
            Map<String, String> filteredResponse = response.entrySet().stream()
                .filter(e -> e.getValue() != null && !e.getValue().isEmpty())
                .collect(java.util.stream.Collectors.toMap(
                    Map.Entry::getKey,
                    Map.Entry::getValue
                ));

            String rawJson = objectMapper.writeValueAsString(filteredResponse);
            transaction.setRawResponse(rawJson);
            log.debug("Raw response stored as JSON. Length: {} bytes", rawJson.length());
        } catch (Exception e) {
            log.error("ERROR converting response to JSON", e);
            // Don't fail the transaction save if JSON conversion fails
            transaction.setRawResponse("{}");
        }
    }

    // ========================================
    // QUERY METHODS
    // ========================================

    /**
     * Get payment transaction by ID
     * @param txnid Transaction ID
     * @return Optional containing transaction
     */
    @Transactional(readOnly = true)
    public Optional<PaymentTransaction> getTransaction(String txnid) {
        return paymentTransactionRepository.findByTxnid(txnid);
    }

    /**
     * Get payment transactions by status
     * @param status Payment status
     * @return List of transactions
     */
    @Transactional(readOnly = true)
    public java.util.List<PaymentTransaction> getTransactionsByStatus(String status) {
        return paymentTransactionRepository.findByStatus(status);
    }

    /**
     * Get merchant transactions
     * @param merchantId Merchant ID (UDF1)
     * @return List of successful transactions for merchant
     */
    @Transactional(readOnly = true)
    public java.util.List<PaymentTransaction> getMerchantTransactions(String merchantId) {
        return paymentTransactionRepository.findSuccessfulTransactionsByMerchant(merchantId, "SUCCESS");
    }

    /**
     * Get hash verification failures
     * @return List of transactions with hash verification failures
     */
    @Transactional(readOnly = true)
    public java.util.List<PaymentTransaction> getHashVerificationFailures() {
        return paymentTransactionRepository.findHashVerificationFailures();
    }

    /**
     * Count transactions by status
     * @param status Payment status
     * @return Count
     */
    @Transactional(readOnly = true)
    public long countTransactionsByStatus(String status) {
        return paymentTransactionRepository.countByStatus(status);
    }
}
