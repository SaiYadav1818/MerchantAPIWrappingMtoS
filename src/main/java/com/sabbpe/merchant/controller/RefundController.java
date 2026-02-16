package com.sabbpe.merchant.controller;

import com.sabbpe.merchant.dto.RefundRequest;
import com.sabbpe.merchant.dto.PaymentStatusResponse;
import com.sabbpe.merchant.service.PaymentService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Handles refund operations
 */
@RestController
@RequestMapping("/api/payment")
public class RefundController {

    private static final Logger logger = LoggerFactory.getLogger(RefundController.class);

    @Autowired
    private PaymentService paymentService;

    /**
     * Processes refund for a payment
     * 
     * POST /api/payment/refund
     * 
     * Request Body:
     * {
     *   "txnId": "TXN...",
     *   "refundAmount": 1000
     * }
     * 
     * Response:
     * {
     *   "txnId": "TXN...",
     *   "amount": 1000,
     *   "status": "REFUNDED",
     *   "gatewayStatus": "refunded"
     * }
     * 
     * @param request RefundRequest with txnId and refund amount
     * @return Response indicating refund status
     */
    @PostMapping("/refund")
    public ResponseEntity<?> refundPayment(@Valid @RequestBody RefundRequest request) {
        logger.info("Processing refund for txnId: {}, amount: {}", request.getTxnId(), request.getRefundAmount());

        try {
            boolean refunded = paymentService.processRefund(request.getTxnId(), request.getRefundAmount());
            
            if (refunded) {
                PaymentStatusResponse response = paymentService.getPaymentStatus(request.getTxnId());
                logger.info("Refund processed successfully for txnId: {}", request.getTxnId());
                return ResponseEntity.ok(response);
            } else {
                logger.warn("Refund processing failed for txnId: {}", request.getTxnId());
                PaymentStatusResponse errorResponse = new PaymentStatusResponse(
                    "FAILURE", "Refund processing failed", "REFUND_FAILED");
                return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
            }
            
        } catch (com.sabbpe.merchant.exception.ValidationException e) {
            logger.warn("Validation error during refund: {}", e.getMessage());
            PaymentStatusResponse errorResponse = new PaymentStatusResponse(
                "FAILURE", e.getMessage(), "VALIDATION_ERROR");
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(errorResponse);
        } catch (Exception e) {
            logger.error("Error processing refund: {}", e.getMessage(), e);
            PaymentStatusResponse errorResponse = new PaymentStatusResponse(
                "FAILURE", "Error processing refund", "INTERNAL_ERROR");
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
        }
    }
}
