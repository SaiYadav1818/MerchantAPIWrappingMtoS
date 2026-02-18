package com.sabbpe.merchant.controller;

import com.sabbpe.merchant.service.MerchantRoutingService;
import com.sabbpe.merchant.service.PaymentVerificationService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.Map;

/**
 * Controller to handle payment gateway redirect URLs.
 * 
 * Responsibilities:
 * 1. Receive payment response from Easebuzz (success/failure redirects)
 * 2. Verify hash to detect tampering
 * 3. Extract merchant and order info from UDF fields
 * 4. Route payment to appropriate merchant
 * 5. Display response on HTML pages
 * 
 * Security:
 * - All responses verified using SHA-512 hash
 * - Missing/invalid hash triggers security alert
 * - All parameters logged for audit trail
 * - Suspicious activity reported
 */
@Slf4j
@Controller
@RequestMapping("/payment")
public class PaymentRedirectController {

    private final PaymentVerificationService verificationService;
    private final MerchantRoutingService routingService;

    @Autowired
    public PaymentRedirectController(
            PaymentVerificationService verificationService,
            MerchantRoutingService routingService) {
        this.verificationService = verificationService;
        this.routingService = routingService;
    }

    /**
     * Handle successful payment redirect from Easebuzz.
     * 
     * Flow:
     * 1. Log all parameters received
     * 2. Verify hash (security check)
     * 3. Extract merchant ID and order ID from UDF fields
     * 4. Route to merchant handler
     * 5. Display response on success page
     * 
     * @param requestParams All request parameters from payment gateway
     * @param model Model to pass data to view
     * @return payment-success view name
     */
    @PostMapping("/success")
    public String handlePaymentSuccess(
            @RequestParam Map<String, String> requestParams,
            Model model) {
        
        try {
            log.info("========== PAYMENT SUCCESS REDIRECT RECEIVED ==========");
            log.info("Transaction ID: {}", requestParams.get("txnid"));
            log.info("Status: {}", requestParams.get("status"));
            log.info("Amount: {}", requestParams.get("amount"));
            log.info("Email: {}", requestParams.get("email"));
            log.info("Parameters received: {}", requestParams.keySet());
            
            // Log all parameters for debugging
            requestParams.forEach((key, value) -> 
                log.debug("  {} = {}", key, value));
            
            logUDFFields(requestParams);

            // ============================================================
            // SECURITY CHECK: Verify hash
            // ============================================================
            log.info("Verifying payment hash...");
            if (!verificationService.validatePaymentResponse(requestParams)) {
                log.error("HASH VERIFICATION FAILED - SUSPICIOUS ACTIVITY!");
                model.addAttribute("paymentData", requestParams);
                model.addAttribute("title", "Payment Suspicious");
                model.addAttribute("suspiciousActivity", true);
                model.addAttribute("message", "Payment verification failed. Please contact support.");
                return "payment-failure";
            }
            log.info("Hash verification PASSED");

            // ============================================================
            // MERCHANT ROUTING: Extract merchant info from UDF fields
            // ============================================================
            String[] merchantInfo = routingService.extractMerchantAndOrderInfo(requestParams);
            String merchantId = merchantInfo[0];
            String orderId = merchantInfo[1];
            
            log.info("Extracted Merchant Info - merchantId: {}, orderId: {}", merchantId, orderId);
            
            if (merchantId == null || merchantId.isEmpty()) {
                log.error("Merchant ID (UDF1) is missing! Cannot route payment.");
                model.addAttribute("paymentData", requestParams);
                model.addAttribute("title", "Payment Suspicious");
                model.addAttribute("message", "Merchant information missing. Please contact support.");
                return "payment-failure";
            }

            // ============================================================
            // ROUTE TO MERCHANT
            // ============================================================
            log.info("Routing payment to merchant: {}", merchantId);
            boolean routeSuccess = routingService.routePaymentToMerchant(
                    merchantId, orderId, requestParams);
            
            if (!routeSuccess) {
                log.error("Failed to route payment to merchant: {}", merchantId);
                // Don't fail user experience, but log the routing error
                // User will see success message, but background routing failed
            } else {
                log.info("Payment successfully routed to merchant: {}", merchantId);
            }

            // ============================================================
            // DISPLAY SUCCESS PAGE
            // ============================================================
            model.addAttribute("paymentData", requestParams);
            model.addAttribute("title", "Payment Successful");
            model.addAttribute("merchantId", merchantId);
            model.addAttribute("orderId", orderId);
            model.addAttribute("udfFields", routingService.extractUDFValues(requestParams));
            model.addAttribute("hashVerified", true);

            log.info("========== PAYMENT SUCCESS REDIRECT HANDLED ==========\n");
            return "payment-success";

        } catch (Exception e) {
            log.error("ERROR handling payment success redirect", e);
            model.addAttribute("paymentData", requestParams);
            model.addAttribute("title", "Payment Error");
            model.addAttribute("message", "An error occurred processing your payment. Please contact support.");
            return "payment-failure";
        }
    }

    /**
     * Handle failed payment redirect from Easebuzz.
     * 
     * Flow:
     * 1. Log all parameters received
     * 2. Verify hash (security check)
     * 3. Extract merchant ID and order ID from UDF fields
     * 4. Route to merchant handler
     * 5. Display response on failure page
     * 
     * @param requestParams All request parameters from payment gateway
     * @param model Model to pass data to view
     * @return payment-failure view name
     */
    @PostMapping("/failure")
    public String handlePaymentFailure(
            @RequestParam Map<String, String> requestParams,
            Model model) {
        
        try {
            log.warn("========== PAYMENT FAILURE REDIRECT RECEIVED ==========");
            log.warn("Transaction ID: {}", requestParams.get("txnid"));
            log.warn("Status: {}", requestParams.get("status"));
            log.warn("Amount: {}", requestParams.get("amount"));
            log.warn("Email: {}", requestParams.get("email"));
            log.warn("Parameters received: {}", requestParams.keySet());
            
            // Log all parameters for debugging
            requestParams.forEach((key, value) ->
                log.debug("  {} = {}", key, value));
            
            logUDFFields(requestParams);

            // ============================================================
            // SECURITY CHECK: Verify hash
            // ============================================================
            log.warn("Verifying payment hash for failed transaction...");
            boolean hashValid = verificationService.validatePaymentResponse(requestParams);
            if (!hashValid) {
                log.error("HASH VERIFICATION FAILED FOR FAILED PAYMENT - SUSPICIOUS ACTIVITY!");
                model.addAttribute("paymentData", requestParams);
                model.addAttribute("title", "Payment Suspicious");
                model.addAttribute("suspiciousActivity", true);
                model.addAttribute("message", "Payment verification failed. Please contact support.");
                return "payment-failure";
            }
            log.warn("Hash verification PASSED for failed payment");

            // ============================================================
            // MERCHANT ROUTING: Extract merchant info from UDF fields
            // ============================================================
            String[] merchantInfo = routingService.extractMerchantAndOrderInfo(requestParams);
            String merchantId = merchantInfo[0];
            String orderId = merchantInfo[1];
            
            log.warn("Extracted Merchant Info - merchantId: {}, orderId: {}", merchantId, orderId);
            
            if (merchantId != null && !merchantId.isEmpty()) {
                // ============================================================
                // ROUTE TO MERCHANT
                // ============================================================
                log.warn("Routing failed payment to merchant: {}", merchantId);
                boolean routeSuccess = routingService.routePaymentToMerchant(
                        merchantId, orderId, requestParams);
                
                if (!routeSuccess) {
                    log.error("Failed to route failure notification to merchant: {}", merchantId);
                    // Continue to display page even if routing fails
                }
            } else {
                log.warn("Merchant ID (UDF1) is missing for failed payment");
            }

            // ============================================================
            // DISPLAY FAILURE PAGE
            // ============================================================
            model.addAttribute("paymentData", requestParams);
            model.addAttribute("title", "Payment Failed");
            model.addAttribute("merchantId", merchantId);
            model.addAttribute("orderId", orderId);
            model.addAttribute("udfFields", routingService.extractUDFValues(requestParams));
            model.addAttribute("hashVerified", hashValid);

            log.warn("========== PAYMENT FAILURE REDIRECT HANDLED ==========\n");
            return "payment-failure";

        } catch (Exception e) {
            log.error("ERROR handling payment failure redirect", e);
            model.addAttribute("paymentData", requestParams);
            model.addAttribute("title", "Payment Error");
            model.addAttribute("message", "An error occurred processing your payment. Please contact support.");
            return "payment-failure";
        }
    }

    /**
     * Log all UDF fields received in payment response
     * Useful for debugging and auditing multi-merchant transactions
     * 
     * @param paymentData Payment response parameters
     */
    private void logUDFFields(Map<String, String> paymentData) {
        log.debug("========== UDF FIELDS ==========");
        for (int i = 1; i <= 10; i++) {
            String udfKey = "udf" + i;
            String udfValue = paymentData.getOrDefault(udfKey, "");
            log.debug("  {} = '{}'", udfKey, udfValue);
        }
        log.debug("================================");
    }
}
