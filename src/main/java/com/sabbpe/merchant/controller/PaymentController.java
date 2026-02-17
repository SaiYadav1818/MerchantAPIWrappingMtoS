package com.sabbpe.merchant.controller;

import com.sabbpe.merchant.dto.*;
import com.sabbpe.merchant.security.SecurityUtil;
import com.sabbpe.merchant.service.PaymentService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@RestController
@RequestMapping("/api/payment")
public class PaymentController {

    private static final Logger logger = LoggerFactory.getLogger(PaymentController.class);

    @Autowired
    private PaymentService paymentService;

    @Autowired
    private SecurityUtil securityUtil;

    @PostMapping("/initiate")
    public ResponseEntity<?> initiatePayment(
            @Valid @RequestBody PaymentInitiateRequest request) {

        logger.info("Received payment initiation request for merchantId: {}, orderId: {}",
                request.getMerchantId(), request.getOrderId());

        try {
            // Validate merchant ownership - JWT token must match request
            securityUtil.validateMerchantOwnership(request.getMerchantId());

            PaymentResponse response = paymentService.initiatePayment(request);

            logger.info("Payment initiation successful with token: {}", response.getInternalToken());

            return ResponseEntity.ok(response);

        } catch (IllegalArgumentException e) {
            logger.warn("Merchant ownership validation failed for merchantId: {}", request.getMerchantId());
            return securityUtil.createAuthorizationErrorResponse(e.getMessage());
        } catch (Exception e) {
            logger.error("Payment initiation error: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new ErrorResponse("Payment initiation failed: " + e.getMessage()));
        }
    }

    @PostMapping("/generate-hash")
    public ResponseEntity<?> generateHash(
            @Valid @RequestBody HashGenerateRequest request) {

        logger.info("Received hash generation request for merchantId: {}, orderId: {}",
                request.getMerchantId(), request.getOrderId());

        try {
            // Validate merchant ownership - JWT token must match request
            securityUtil.validateMerchantOwnership(request.getMerchantId());

            HashResponse response = paymentService.generateHash(request);

            logger.info("Hash generated successfully for orderId: {}", response.getOrderId());

            return ResponseEntity.ok(response);

        } catch (IllegalArgumentException e) {
            logger.warn("Merchant ownership validation failed for merchantId: {}", request.getMerchantId());
            return securityUtil.createAuthorizationErrorResponse(e.getMessage());
        } catch (Exception e) {
            logger.error("Hash generation error: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new ErrorResponse("Hash generation failed: " + e.getMessage()));
        }
    }

    @PostMapping("/payment-url")
    public ResponseEntity<?> getPaymentUrl(
            @Valid @RequestBody PaymentUrlRequest request) {

        logger.info("Received payment URL request for internal token: {}", request.getInternalToken());

        try {
            PaymentUrlResponse response = paymentService.getPaymentUrl(request);

            if ("SUCCESS".equals(response.getStatus())) {
                logger.info("Payment URL generated successfully for orderId: {}", response.getOrderId());
                return ResponseEntity.ok(response);
            } else {
                logger.error("Failed to generate payment URL: {}", response.getMessage());
                return ResponseEntity.status(500).body(response);
            }

        } catch (Exception e) {
            logger.error("Payment URL generation error: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new ErrorResponse("Failed to generate payment URL: " + e.getMessage()));
        }
    }
}
