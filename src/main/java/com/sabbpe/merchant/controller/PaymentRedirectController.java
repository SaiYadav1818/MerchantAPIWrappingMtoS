package com.sabbpe.merchant.controller;

import com.sabbpe.merchant.entity.PaymentTransaction;
import com.sabbpe.merchant.service.PaymentProcessingService;
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
 * Workflow:
 * 1. Receive all parameters from Easebuzz gateway redirect
 * 2. Process response (verify hash, extract fields)
 * 3. Store in H2 database via PaymentProcessingService
 * 4. Display on HTML page with complete transaction details
 * 
 * Idempotent: Duplicate callbacks update existing record instead of creating duplicate
 */
@Slf4j
@Controller
@RequestMapping("/payment")
public class PaymentRedirectController {

    @Autowired
    private PaymentProcessingService paymentProcessingService;

    /**
     * Handle successful payment redirect from Easebuzz.
     * 
     * Workflow:
     * 1. Log all parameters received
     * 2. Call PaymentProcessingService to:
     *    - Verify hash signature
     *    - Extract and map all fields
     *    - Check for duplicate transaction (idempotent)
     *    - Store in database
     * 3. Pass data to Thymeleaf for HTML display
     * 
     * @param requestParams All request parameters from payment gateway
     * @param model Model to pass data to view
     * @return payment-success view
     */
    @PostMapping("/success")
    public String handlePaymentSuccess(@RequestParam Map<String, String> requestParams, Model model) {
        log.info("========== PAYMENT SUCCESS REDIRECT RECEIVED ==========");
        log.info("Parameters count: {}", requestParams.size());
        requestParams.forEach((key, value) -> log.debug("  {} = {}", key, value));

        try {
            // Process and store payment response in database
            PaymentTransaction transaction = paymentProcessingService.processSuccessResponse(requestParams);
            log.info("SUCCESS: Transaction saved to database. ID: {}, txnid: {}", 
                transaction.getId(), transaction.getTxnid());

            // Add data to model for HTML display
            model.addAttribute("paymentData", requestParams);
            model.addAttribute("title", "Payment Successful");
            model.addAttribute("transaction", transaction);
            model.addAttribute("hashVerified", transaction.isHashValid());

            // Check if hash verification failed
            if (!transaction.isHashValid()) {
                log.warn("WARNING: Hash verification failed for txnid: {}", transaction.getTxnid());
                model.addAttribute("suspiciousActivity", true);
                model.addAttribute("message", "Payment received but hash verification failed. Please contact support.");
            }

            log.info("========== SUCCESS PAGE RENDERED ==========\n");
            return "payment-success";

        } catch (Exception e) {
            log.error("ERROR handling payment success redirect", e);
            model.addAttribute("paymentData", requestParams);
            model.addAttribute("title", "Payment Processing Error");
            model.addAttribute("message", "An error occurred while processing your payment: " + e.getMessage());
            return "payment-failure";
        }
    }

    /**
     * Handle failed payment redirect from Easebuzz.
     * 
     * Workflow:
     * 1. Log all parameters received
     * 2. Call PaymentProcessingService to:
     *    - Verify hash signature
     *    - Extract and map all fields
     *    - Check for duplicate transaction (idempotent)
     *    - Store in database with FAILED status
     * 3. Pass data to Thymeleaf for HTML display
     * 
     * @param requestParams All request parameters from payment gateway
     * @param model Model to pass data to view
     * @return payment-failure view
     */
    @PostMapping("/failure")
    public String handlePaymentFailure(@RequestParam Map<String, String> requestParams, Model model) {
        log.warn("========== PAYMENT FAILURE REDIRECT RECEIVED ==========");
        log.warn("Parameters count: {}", requestParams.size());
        requestParams.forEach((key, value) -> log.debug("  {} = {}", key, value));

        try {
            // Process and store payment response in database
            PaymentTransaction transaction = paymentProcessingService.processFailureResponse(requestParams);
            log.warn("FAILED: Transaction saved to database. ID: {}, txnid: {}", 
                transaction.getId(), transaction.getTxnid());

            // Add data to model for HTML display
            model.addAttribute("paymentData", requestParams);
            model.addAttribute("title", "Payment Failed");
            model.addAttribute("transaction", transaction);
            model.addAttribute("hashVerified", transaction.isHashValid());

            // Check if hash verification failed
            if (!transaction.isHashValid()) {
                log.error("ERROR: Hash verification failed for failed transaction. txnid: {}", transaction.getTxnid());
                model.addAttribute("suspiciousActivity", true);
                model.addAttribute("message", "Payment failed and hash verification also failed. Possible tampering detected. Please contact support.");
            }

            log.warn("========== FAILURE PAGE RENDERED ==========\n");
            return "payment-failure";

        } catch (Exception e) {
            log.error("ERROR handling payment failure redirect", e);
            model.addAttribute("paymentData", requestParams);
            model.addAttribute("title", "Payment Processing Error");
            model.addAttribute("message", "An error occurred while processing your failed payment: " + e.getMessage());
            return "payment-failure";
        }
    }
}
