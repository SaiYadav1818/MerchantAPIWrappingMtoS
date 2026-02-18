package com.sabbpe.merchant.controller;

import com.sabbpe.merchant.dto.PaymentStatusResponse;
import com.sabbpe.merchant.exception.TransactionNotFoundException;
import com.sabbpe.merchant.service.PaymentStatusService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * REST Controller for Payment Status Queries
 * 
 * Provides REST endpoints to query payment transaction status with proper
 * exception handling and response serialization.
 * 
 * Endpoints:
 * GET /api/payment/status/{txnid} - Get complete transaction details
 * 
 * All LocalDateTime fields are serialized as "yyyy-MM-dd HH:mm:ss" format
 * Exception handling returns standard error response format
 */
@RestController
@RequestMapping("/api/payment")
public class PaymentStatusController {

    private static final Logger logger = LoggerFactory.getLogger(PaymentStatusController.class);

    private final PaymentStatusService paymentStatusService;

    @Autowired
    public PaymentStatusController(PaymentStatusService paymentStatusService) {
        this.paymentStatusService = paymentStatusService;
    }

    /**
     * Get complete payment transaction details by transaction ID
     * 
     * Endpoint: GET /api/payment/status/{txnid}
     * 
     * Response includes:
     * - txnid (transaction ID)
     * - status (payment status - SUCCESS, FAILED, etc.)
     * - amount (payment amount)
     * - bank details (bank_ref_num, bank_name, bankcode, mode)
     * - gateway details (easepayid)
     * - merchant routing (udf1, udf2, udf3 - merchant_id, order_id, internal_reference)
     * - customer information (firstname, email, phone)
     * - card details (card_type, issuing_bank)
     * - verification status (hash_verified)
     * - timestamps (created_at, updated_at in "yyyy-MM-dd HH:mm:ss" format)
     * - raw_response (complete gateway JSON)
     * 
     * Success Response (200):
     * {
     *   "txnid": "TXN123456",
     *   "status": "SUCCESS",
     *   "amount": 1000.00,
     *   "bank_ref_num": "BANK12345",
     *   "easepayid": "EASE789",
     *   "bankcode": "HDFC",
     *   "mode": "NET_BANKING",
     *   "email": "customer@example.com",
     *   "phone": "9876543210",
     *   "firstname": "John Doe",
     *   "bank_name": "HDFC Bank",
     *   "issuing_bank": "HDFC Bank Ltd",
     *   "card_type": null,
     *   "hash_verified": true,
     *   "error_message": null,
     *   "udf1": "MERCHANT_001",
     *   "udf2": "ORDER_789",
     *   "udf3": "INT_REF_456",
     *   "udf4": null,
     *   ... (udf5-10)
     *   "raw_response": "{...}",
     *   "created_at": "2024-01-15 14:30:45",
     *   "updated_at": "2024-01-15 14:30:45"
     * }
     * 
     * Error Response (404):
     * {
     *   "status": "FAILURE",
     *   "errorCode": "TRANSACTION_NOT_FOUND",
     *   "message": "Transaction not found with txnid: TXN123456",
     *   "timestamp": 1705329045000
     * }
     * 
     * @param txnid Transaction ID from payment gateway (required, non-empty)
     * @return ResponseEntity with TransactionStatusResponse DTO on success
     * @throws TransactionNotFoundException if transaction not found (404)
     * @throws IllegalArgumentException if txnid is null or empty (400)
     */
    @GetMapping("/status/{txnid}")
    public ResponseEntity<PaymentStatusResponse> getPaymentStatus(
            @PathVariable String txnid) {
        
        logger.info("GET /api/payment/status/{} - Fetching payment status", txnid);

        // Validate input
        if (txnid == null || txnid.trim().isEmpty()) {
            logger.warn("Invalid txnid provided - null or empty");
            throw new IllegalArgumentException("Transaction ID (txnid) is required and must not be empty");
        }

        // Fetch payment status from service
        // Service will throw TransactionNotFoundException if not found (404)
        PaymentStatusResponse response = paymentStatusService.getPaymentStatus(txnid);
        
        logger.info("Successfully returned status for txnid: {}", txnid);
        return ResponseEntity.ok(response);
    }

    /**
     * Check if transaction exists in database (lightweight query)
     * 
     * Endpoint: GET /api/payment/exists/{txnid}
     * 
     * Response: {
     *   "exists": true,
     *   "txnid": "TXN123456"
     * }
     * 
     * @param txnid Transaction ID
     * @return ResponseEntity with existence check result
     */
    @GetMapping("/exists/{txnid}")
    public ResponseEntity<?> checkTransactionExists(@PathVariable String txnid) {
        logger.info("GET /api/payment/exists/{} - Checking if transaction exists", txnid);

        if (txnid == null || txnid.trim().isEmpty()) {
            logger.warn("Invalid txnid provided for exists check");
            throw new IllegalArgumentException("Transaction ID is required");
        }

        try {
            boolean exists = paymentStatusService.transactionExists(txnid);
            logger.info("Transaction exists check for {}: {}", txnid, exists);
            
            return ResponseEntity.ok(java.util.Map.of(
                "txnid", txnid,
                "exists", exists
            ));
            
        } catch (Exception e) {
            logger.error("ERROR checking transaction existence", e);
            throw new RuntimeException("Error checking transaction existence", e);
        }
    }
}

