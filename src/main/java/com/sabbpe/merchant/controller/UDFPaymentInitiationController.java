package com.sabbpe.merchant.controller;

import com.sabbpe.merchant.dto.EasebuzzInitiateRequest;
import com.sabbpe.merchant.dto.PaymentResponse;
import com.sabbpe.merchant.service.payment.PaymentInitiationService;
import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * Controller for UDF-based payment initiation
 * 
 * Endpoint: POST /api/payment/initiate-with-udf
 * 
 * Supports passing UDF1-UDF10 fields for multi-merchant routing:
 * - udf1: Merchant ID (required for routing)
 * - udf2: Order ID (recommended)
 * - udf3-udf10: Custom fields per merchant
 * 
 * Example request:
 * {
 *   "txnid": "TXN123456",
 *   "amount": 1000.00,
 *   "productinfo": "Order #001",
 *   "firstname": "John",
 *   "email": "john@example.com",
 *   "phone": "9876543210",
 *   "udf1": "merchant_123",
 *   "udf2": "order_456",
 *   "udf3": "custom_value_3"
 * }
 */
@Slf4j
@RestController
@RequestMapping("/api/payment")
public class UDFPaymentInitiationController {

    private final PaymentInitiationService paymentInitiationService;

    @Autowired
    public UDFPaymentInitiationController(PaymentInitiationService paymentInitiationService) {
        this.paymentInitiationService = paymentInitiationService;
    }

    /**
     * Initiate payment with full UDF1-UDF10 support
     * 
     * Features:
     * - Auto-generates SHA-512 hash including UDF fields
     * - Validates all required fields
     * - Creates transaction record
     * - Returns payment URL for redirect
     * 
     * @param request Payment initiation request with optional UDF fields
     * @return PaymentResponse with payment URL or error details
     */
    @PostMapping("/initiate-with-udf")
    public ResponseEntity<PaymentResponse> initiatePaymentWithUDF(
            @Valid @RequestBody EasebuzzInitiateRequest request) {
        
        try {
            log.info("Payment initiation with UDF received. txnid: {}, merchantId(udf1): {}, orderId(udf2): {}",
                    request.getTxnid(), request.getUdf1(), request.getUdf2());
            
            log.debug("Payment Details:");
            log.debug("  Amount: {}", request.getAmount());
            log.debug("  Product Info: {}", request.getProductinfo());
            log.debug("  Customer Email: {}", request.getEmail());
            log.debug("  Customer Name: {}", request.getFirstname());
            log.debug("  Phone: {}", request.getPhone());
            
            logUDFFields(request);
            
            // Process payment initiation
            PaymentResponse response = paymentInitiationService.initiatePaymentWithUDF(request);
            
            if (response.isSuccessful()) {
                log.info("Payment initiation successful. txnid: {}, status: {}",
                        request.getTxnid(), response.getStatus());
                return ResponseEntity.ok(response);
            } else {
                log.error("Payment initiation failed. txnid: {}, error: {}",
                        request.getTxnid(), response.getMessage());
                return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
            }
            
        } catch (Exception e) {
            log.error("Error during payment initiation. txnid: {}, error: {}",
                    request.getTxnid(), e.getMessage(), e);
            
            PaymentResponse errorResponse = PaymentResponse.builder()
                    .transactionId(request.getTxnid())
                    .status("FAILURE")
                    .message("Payment initiation error: " + e.getMessage())
                    .errorCode("INIT_ERROR")
                    .build();
            
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
        }
    }

    /**
     * Get payment initiation status
     * 
     * @param txnid Transaction ID
     * @return Payment response with current status
     */
    @GetMapping("/initiation-status/{txnid}")
    public ResponseEntity<PaymentResponse> getInitiationStatus(
            @PathVariable String txnid) {
        
        try {
            log.info("Fetching payment initiation status for txnid: {}", txnid);
            
            PaymentResponse response = paymentInitiationService.getPaymentStatus(txnid);
            
            if (response != null) {
                return ResponseEntity.ok(response);
            } else {
                return ResponseEntity.notFound().build();
            }
            
        } catch (Exception e) {
            log.error("Error fetching payment status for txnid: {}, error: {}",
                    txnid, e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    /**
     * Verify hash for a payment request
     * 
     * Useful for testing hash generation before actual payment
     * 
     * @param request Payment request
     * @return Hash verification result
     */
    @PostMapping("/verify-hash")
    public ResponseEntity<?> verifyHash(
            @Valid @RequestBody EasebuzzInitiateRequest request) {
        
        try {
            log.info("Hash verification requested for txnid: {}", request.getTxnid());
            
            String generatedHash = paymentInitiationService.generatePaymentHash(request);
            
            return ResponseEntity.ok(Map.of(
                    "txnid", request.getTxnid(),
                    "generated_hash", generatedHash,
                    "matches_request_hash", generatedHash.equalsIgnoreCase(request.getHash())
            ));
            
        } catch (Exception e) {
            log.error("Error verifying hash for txnid: {}, error: {}",
                    request.getTxnid(), e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    /**
     * Log all UDF fields for debugging and audit
     * 
     * @param request Payment request
     */
    private void logUDFFields(EasebuzzInitiateRequest request) {
        log.debug("========== UDF FIELDS ==========");
        log.debug("  udf1 (Merchant ID): {}", request.getUdf1());
        log.debug("  udf2 (Order ID): {}", request.getUdf2());
        log.debug("  udf3: {}", request.getUdf3());
        log.debug("  udf4: {}", request.getUdf4());
        log.debug("  udf5: {}", request.getUdf5());
        log.debug("  udf6: {}", request.getUdf6());
        log.debug("  udf7: {}", request.getUdf7());
        log.debug("  udf8: {}", request.getUdf8());
        log.debug("  udf9: {}", request.getUdf9());
        log.debug("  udf10: {}", request.getUdf10());
        log.debug("================================");
    }
}
