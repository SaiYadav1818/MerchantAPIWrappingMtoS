package com.sabbpe.merchant.controller;

import com.sabbpe.merchant.service.PaymentService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.Map;

/**
 * Handles callbacks from Easebuzz payment gateway
 */
@RestController
@RequestMapping("/payment")
public class CallbackController {

    private static final Logger logger = LoggerFactory.getLogger(CallbackController.class);

    @Autowired
    private PaymentService paymentService;

    /**
     * Receives callback from Easebuzz payment gateway
     * 
     * POST /payment/easebuzz/callback
     * 
     * Accepts all request parameters dynamically
     * Always returns HTTP 200 with body "OK"
     * 
     * @param params All request parameters
     * @return HTTP 200 with "OK"
     */
    @PostMapping("/easebuzz/callback")
    public ResponseEntity<String> handleEasebuzzCallback(@RequestParam Map<String, String> params) {
        logger.info("Received callback from Easebuzz with {} parameters", params.size());
        logger.debug("Callback parameters: {}", params);

        try {
            // Process callback asynchronously in service
            boolean processed = paymentService.processEasebuzzCallback(params);
            
            if (processed) {
                logger.info("Easebuzz callback processed successfully");
            } else {
                logger.warn("Easebuzz callback processing returned false");
            }
            
        } catch (Exception e) {
            logger.error("Error processing Easebuzz callback: {}", e.getMessage(), e);
            // Still return 200 OK as per Easebuzz requirements
        }

        // IMPORTANT: Always return 200 OK to Easebuzz
        return ResponseEntity.ok("OK");
    }

    /**
     * Webhook endpoint for Easebuzz callbacks (alternative endpoint)
     * This is an alias for the main callback endpoint
     */
    @PostMapping("/webhook/easebuzz")
    public ResponseEntity<String> handleEasebuzzWebhook(@RequestParam Map<String, String> params) {
        logger.info("Received webhook from Easebuzz");
        return handleEasebuzzCallback(params);
    }
}
