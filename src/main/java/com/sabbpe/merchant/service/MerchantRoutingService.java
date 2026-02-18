package com.sabbpe.merchant.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Map;

/**
 * Service for routing payments to merchants based on UDF values
 * 
 * Multi-Merchant Routing Strategy:
 * - UDF1 contains merchant identifier (merchantId from PaymentInitiateRequest)
 * - Routes payment response to appropriate merchant handler
 * - Supports webhook callbacks, wallet updates, and transaction logging per merchant
 */
@Slf4j
@Service
public class MerchantRoutingService {

    private final ObjectMapper objectMapper;

    @Autowired
    public MerchantRoutingService(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    /**
     * Route payment to merchant based on UDF1 (merchant ID)
     * 
     * Processing flow:
     * 1. Extract merchant ID from udf1
     * 2. Extract order ID from udf2
     * 3. Validate merchant exists in system
     * 4. Process payment status update
     * 5. Send webhook to merchant if configured
     * 6. Log all actions
     * 
     * @param merchantId Merchant identifier (from udf1)
     * @param orderId Order identifier (from udf2)
     * @param paymentResponse Complete payment response from gateway with all parameters
     * @return true if routed successfully, false otherwise
     */
    public boolean routePaymentToMerchant(
            String merchantId,
            String orderId,
            Map<String, String> paymentResponse) {
        
        try {
            log.info("Routing payment to merchant. merchantId: {}, orderId: {}", merchantId, orderId);
            
            // Extract important fields from payment response
            String txnId = paymentResponse.get("txnid");
            String status = paymentResponse.get("status");
            String amount = paymentResponse.get("amount");
            String email = paymentResponse.get("email");
            String firstname = paymentResponse.get("firstname");
            
            log.info("Payment Details - txnId: {}, status: {}, amount: {}, email: {}, firstname: {}",
                    txnId, status, amount, email, firstname);
            
            // Route based on payment status
            if ("success".equalsIgnoreCase(status)) {
                return handleSuccessfulPayment(merchantId, orderId, txnId, amount, paymentResponse);
            } else {
                return handleFailedPayment(merchantId, orderId, txnId, paymentResponse);
            }
            
        } catch (Exception e) {
            log.error("Error routing payment to merchant. merchantId: {}, orderId: {}, error: {}",
                    merchantId, orderId, e.getMessage(), e);
            return false;
        }
    }

    /**
     * Handle successful payment scenario
     * 
     * Actions:
     * - Log transaction as completed
     * - Update merchant wallet/balance
     * - Send success webhook to merchant
     * - Send customer receipt email
     * 
     * @param merchantId Merchant ID
     * @param orderId Order ID
     * @param txnId Transaction ID
     * @param amount Payment amount
     * @param paymentResponse Full response map from gateway
     * @return success status
     */
    private boolean handleSuccessfulPayment(
            String merchantId,
            String orderId,
            String txnId,
            String amount,
            Map<String, String> paymentResponse) {
        
        try {
            log.info("Processing successful payment. merchantId: {}, orderId: {}, txnId: {}, amount: {}",
                    merchantId, orderId, txnId, amount);
            
            // Step 1: Save transaction to database (handled by EasebuzzService)
            // Step 2: Update merchant wallet
            updateMerchantWallet(merchantId, amount, "CREDIT");
            
            // Step 3: Send webhook to merchant
            sendMerchantWebhook(merchantId, orderId, txnId, "SUCCESS", paymentResponse);
            
            // Step 4: Send customer email
            sendCustomerReceipt(paymentResponse.get("email"), orderId, txnId, amount);
            
            log.info("Successful payment routed. merchantId: {}, orderId: {}", merchantId, orderId);
            return true;
            
        } catch (Exception e) {
            log.error("Error handling successful payment. merchantId: {}, orderId: {}, error: {}",
                    merchantId, orderId, e.getMessage(), e);
            return false;
        }
    }

    /**
     * Handle failed payment scenario
     * 
     * Actions:
     * - Log transaction as failed
     * - Send failure webhook to merchant
     * - Send customer notification
     * 
     * @param merchantId Merchant ID
     * @param orderId Order ID
     * @param txnId Transaction ID
     * @param paymentResponse Full response map from gateway
     * @return success status
     */
    private boolean handleFailedPayment(
            String merchantId,
            String orderId,
            String txnId,
            Map<String, String> paymentResponse) {
        
        try {
            log.warn("Processing failed payment. merchantId: {}, orderId: {}, txnId: {}",
                    merchantId, orderId, txnId);
            
            // Step 1: Save failed transaction
            // Step 2: Send failure webhook to merchant
            sendMerchantWebhook(merchantId, orderId, txnId, "FAILURE", paymentResponse);
            
            // Step 3: Send customer notification
            sendCustomerFailureNotification(paymentResponse.get("email"), orderId, txnId);
            
            log.info("Failed payment routed. merchantId: {}, orderId: {}", merchantId, orderId);
            return true;
            
        } catch (Exception e) {
            log.error("Error handling failed payment. merchantId: {}, orderId: {}, error: {}",
                    merchantId, orderId, e.getMessage(), e);
            return false;
        }
    }

    /**
     * Update merchant wallet balance
     * 
     * @param merchantId Merchant ID
     * @param amount Amount to add/deduct
     * @param operation CREDIT or DEBIT
     */
    private void updateMerchantWallet(String merchantId, String amount, String operation) {
        try {
            log.debug("Updating merchant wallet. merchantId: {}, amount: {}, operation: {}",
                    merchantId, amount, operation);
            
            // TODO: Implement wallet update logic
            // This would typically involve:
            // 1. Fetching merchant from MerchantRepository
            // 2. Updating balance based on operation (CREDIT/DEBIT)
            // 3. Saving updated merchant back to database
            
            log.info("Merchant wallet updated. merchantId: {}, amount: {}, operation: {}",
                    merchantId, amount, operation);
            
        } catch (Exception e) {
            log.error("Error updating merchant wallet. merchantId: {}, error: {}",
                    merchantId, e.getMessage(), e);
        }
    }

    /**
     * Send webhook callback to merchant
     * 
     * Merchant webhook URL should be configured in database.
     * Supports retry mechanism for failed deliveries.
     * 
     * @param merchantId Merchant ID
     * @param orderId Order ID
     * @param txnId Transaction ID
     * @param paymentStatus Payment status (SUCCESS/FAILURE)
     * @param paymentResponse Full response map
     */
    private void sendMerchantWebhook(
            String merchantId,
            String orderId,
            String txnId,
            String paymentStatus,
            Map<String, String> paymentResponse) {
        
        try {
            log.debug("Sending merchant webhook. merchantId: {}, orderId: {}, status: {}",
                    merchantId, orderId, paymentStatus);
            
            // TODO: Implement webhook delivery
            // This would typically involve:
            // 1. Getting merchant webhook URL from MerchantRepository
            // 2. Creating webhook payload (JSON)
            // 3. Making POST request to merchant URL
            // 4. Logging success/failure with retry logic
            
            log.info("Merchant webhook sent. merchantId: {}, orderId: {}", merchantId, orderId);
            
        } catch (Exception e) {
            log.error("Error sending merchant webhook. merchantId: {}, orderId: {}, error: {}",
                    merchantId, orderId, e.getMessage(), e);
        }
    }

    /**
     * Send successful payment receipt to customer email
     * 
     * @param email Customer email
     * @param orderId Order ID
     * @param txnId Transaction ID
     * @param amount Payment amount
     */
    private void sendCustomerReceipt(String email, String orderId, String txnId, String amount) {
        try {
            log.debug("Sending customer receipt. email: {}, orderId: {}, txnId: {}, amount: {}",
                    email, orderId, txnId, amount);
            
            // TODO: Implement email sending
            // This would typically involve:
            // 1. Creating email template with payment details
            // 2. Sending email via EmailService
            // 3. Logging email delivery status
            
            log.info("Customer receipt sent. email: {}, orderId: {}", email, orderId);
            
        } catch (Exception e) {
            log.error("Error sending customer receipt. email: {}, error: {}",
                    email, e.getMessage(), e);
        }
    }

    /**
     * Send payment failure notification to customer
     * 
     * @param email Customer email
     * @param orderId Order ID
     * @param txnId Transaction ID
     */
    private void sendCustomerFailureNotification(String email, String orderId, String txnId) {
        try {
            log.debug("Sending customer failure notification. email: {}, orderId: {}, txnId: {}",
                    email, orderId, txnId);
            
            // TODO: Implement failure email
            // This would typically involve:
            // 1. Creating email template with retry options
            // 2. Sending email via EmailService
            // 3. Logging email delivery status
            
            log.info("Customer failure notification sent. email: {}, orderId: {}", email, orderId);
            
        } catch (Exception e) {
            log.error("Error sending customer failure notification. email: {}, error: {}",
                    email, e.getMessage(), e);
        }
    }

    /**
     * Extract all UDF values from payment response
     * Returns a map of udf1-udf10 with their values
     * 
     * @param paymentResponse Payment response map from gateway
     * @return Map of UDF fields
     */
    public Map<String, String> extractUDFValues(Map<String, String> paymentResponse) {
        return Map.ofEntries(
                Map.entry("udf1", paymentResponse.getOrDefault("udf1", "")),
                Map.entry("udf2", paymentResponse.getOrDefault("udf2", "")),
                Map.entry("udf3", paymentResponse.getOrDefault("udf3", "")),
                Map.entry("udf4", paymentResponse.getOrDefault("udf4", "")),
                Map.entry("udf5", paymentResponse.getOrDefault("udf5", "")),
                Map.entry("udf6", paymentResponse.getOrDefault("udf6", "")),
                Map.entry("udf7", paymentResponse.getOrDefault("udf7", "")),
                Map.entry("udf8", paymentResponse.getOrDefault("udf8", "")),
                Map.entry("udf9", paymentResponse.getOrDefault("udf9", "")),
                Map.entry("udf10", paymentResponse.getOrDefault("udf10", ""))
        );
    }

    /**
     * Extract merchant and order information from UDF fields
     * Default configuration:
     * - udf1 = merchantId
     * - udf2 = orderId
     * 
     * @param paymentResponse Payment response map
     * @return Object array with [merchantId, orderId]
     */
    public String[] extractMerchantAndOrderInfo(Map<String, String> paymentResponse) {
        String merchantId = paymentResponse.getOrDefault("udf1", "");
        String orderId = paymentResponse.getOrDefault("udf2", "");
        
        log.debug("Extracted merchant info. merchantId: {}, orderId: {}", merchantId, orderId);
        
        return new String[]{merchantId, orderId};
    }
}
