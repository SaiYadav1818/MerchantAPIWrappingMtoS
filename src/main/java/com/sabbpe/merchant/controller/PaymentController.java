package com.sabbpe.merchant.controller;

import com.sabbpe.merchant.dto.*;
import com.sabbpe.merchant.service.PaymentService;
import com.sabbpe.merchant.util.JwtUtil;
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
    private JwtUtil jwtUtil;

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

    @PostMapping("/easebuzz/initiate")
    public ResponseEntity<EasebuzzPaymentResponse> initiateEasebuzzPayment(
            @Valid @RequestBody EasebuzzPaymentInitiateRequest request,
            @RequestHeader(value = "Authorization", required = false) String authHeader) {

        logger.info("Received Easebuzz payment initiation request");

        try {

            String token = jwtUtil.extractTokenFromAuthHeader(authHeader);
            String merchantId = jwtUtil.extractMerchantIdFromToken(token);

            logger.debug("Extracted merchantId from JWT: {}", merchantId);

            EasebuzzPaymentResponse response =
                    paymentService.initiateEasebuzzPayment(request, merchantId);

            logger.info("Easebuzz payment initiated successfully with txnId: {}",
                    response.getTxnId());

            return ResponseEntity.ok(response);

        } catch (com.sabbpe.merchant.exception.UnauthorizedException e) {

            logger.warn("Unauthorized request: {}", e.getMessage());

            return ResponseEntity
                    .status(HttpStatus.UNAUTHORIZED)
                    .body(EasebuzzPaymentResponse.error(
                            "FAILURE",
                            e.getMessage(),
                            "UNAUTHORIZED"));

        } catch (com.sabbpe.merchant.exception.MerchantNotFoundException e) {

            logger.warn("Merchant error: {}", e.getMessage());

            return ResponseEntity
                    .status(HttpStatus.NOT_FOUND)
                    .body(EasebuzzPaymentResponse.error(
                            "FAILURE",
                            e.getMessage(),
                            "MERCHANT_NOT_FOUND"));

        } catch (com.sabbpe.merchant.exception.GatewayException e) {

            logger.error("Gateway error: {}", e.getMessage(), e);

            return ResponseEntity
                    .status(HttpStatus.BAD_GATEWAY)
                    .body(EasebuzzPaymentResponse.error(
                            "FAILURE",
                            e.getMessage(),
                            "GATEWAY_ERROR"));
        }
    }
}
