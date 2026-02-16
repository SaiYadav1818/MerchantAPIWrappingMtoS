package com.sabbpe.merchant.controller;

import com.sabbpe.merchant.dto.PaymentStatusResponse;
import com.sabbpe.merchant.service.PaymentService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Handles payment status queries
 */
@RestController
@RequestMapping("/api/payment")
public class PaymentStatusController {

    private static final Logger logger = LoggerFactory.getLogger(PaymentStatusController.class);

    @Autowired
    private PaymentService paymentService;

    /**
     * Retrieves payment status for a transaction
     * 
     * GET /api/payment/status/{txnId}
     * 
     * Response:
     * {
     *   "txnId": "TXN...",
     *   "amount": 1000,
     *   "status": "SUCCESS",
     *   "gatewayStatus": "success"
     * }
     * 
     * @param txnId Transaction ID
     * @return PaymentStatusResponse with payment details and status
     */
    @GetMapping("/status/{txnId}")
    public ResponseEntity<?> getPaymentStatus(@PathVariable String txnId) {
        logger.info("Fetching payment status for txnId: {}", txnId);

        try {
            PaymentStatusResponse response = paymentService.getPaymentStatus(txnId);
            logger.info("Payment status retrieved successfully for txnId: {}", txnId);
            return ResponseEntity.ok(response);
            
        } catch (com.sabbpe.merchant.exception.ValidationException e) {
            logger.warn("Payment not found: {}", e.getMessage());
            PaymentStatusResponse errorResponse = new PaymentStatusResponse(
                "FAILURE", e.getMessage(), "PAYMENT_NOT_FOUND");
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(errorResponse);
        } catch (Exception e) {
            logger.error("Error fetching payment status: {}", e.getMessage(), e);
            PaymentStatusResponse errorResponse = new PaymentStatusResponse(
                "FAILURE", "Error fetching payment status", "INTERNAL_ERROR");
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
        }
    }
}
