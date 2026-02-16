package com.sabbpe.merchant.controller;

import com.sabbpe.merchant.dto.*;
import com.sabbpe.merchant.service.PaymentService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
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

    @PostMapping("/initiate")
    public ResponseEntity<PaymentResponse> initiatePayment(
            @Valid @RequestBody PaymentInitiateRequest request) {

        logger.info("Received payment initiation request for merchantId: {}, orderId: {}",
                request.getMerchantId(), request.getOrderId());

        PaymentResponse response = paymentService.initiatePayment(request);

        logger.info("Payment initiation successful with token: {}", response.getInternalToken());

        return ResponseEntity.ok(response);
    }

    @PostMapping("/generate-hash")
    public ResponseEntity<HashResponse> generateHash(
            @Valid @RequestBody HashGenerateRequest request) {

        logger.info("Received hash generation request for merchantId: {}, orderId: {}",
                request.getMerchantId(), request.getOrderId());

        HashResponse response = paymentService.generateHash(request);

        logger.info("Hash generated successfully for orderId: {}", response.getOrderId());

        return ResponseEntity.ok(response);
    }

    @PostMapping("/payment-url")
    public ResponseEntity<PaymentUrlResponse> getPaymentUrl(
            @Valid @RequestBody PaymentUrlRequest request) {

        logger.info("Received payment URL request for internal token: {}", request.getInternalToken());

        PaymentUrlResponse response = paymentService.getPaymentUrl(request);

        if ("SUCCESS".equals(response.getStatus())) {
            logger.info("Payment URL generated successfully for orderId: {}", response.getOrderId());
            return ResponseEntity.ok(response);
        } else {
            logger.error("Failed to generate payment URL: {}", response.getMessage());
            return ResponseEntity.status(500).body(response);
        }
    }
}
