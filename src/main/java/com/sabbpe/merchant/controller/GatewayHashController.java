package com.sabbpe.merchant.controller;

import com.sabbpe.merchant.dto.GatewayHashRequest;
import com.sabbpe.merchant.dto.GatewayHashResponse;
import com.sabbpe.merchant.service.GatewayHashService;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * REST Controller for Easebuzz payment gateway hash generation
 * 
 * Provides endpoints for generating Easebuzz payment gateway hashes
 * Used during payment initiation flow
 */
@RestController
@RequestMapping("/api/gateway")
public class GatewayHashController {

    private static final Logger logger = LoggerFactory.getLogger(GatewayHashController.class);

    @Autowired
    private GatewayHashService gatewayHashService;

    /**
     * Generate Easebuzz payment gateway hash
     * 
     * Endpoint: POST /api/gateway/generate-hash
     * 
     * This endpoint generates a SHA-512 hash used by Easebuzz payment gateway.
     * The hash is required for the payment initiation request.
     * 
     * Hash Format: key|txnid|amount|productinfo|firstname|email|||||||||||salt
     * 
     * @param request GatewayHashRequest containing transaction details
     * @return ResponseEntity with GatewayHashResponse containing generated hash
     * 
     * @throws IllegalArgumentException if Easebuzz configuration is missing
     * @throws RuntimeException if hash generation fails
     * 
     * Example Request:
     * POST /api/gateway/generate-hash
     * {
     *   "txnId": "TXN1234567890",
     *   "amount": "100.00",
     *   "productInfo": "Order #12345",
     *   "firstName": "John",
     *   "email": "john@example.com"
     * }
     * 
     * Example Response:
     * {
     *   "status": "SUCCESS",
     *   "hash": "a1b2c3d4e5f6...",
     *   "timestamp": 1676543210000
     * }
     */
    @PostMapping("/generate-hash")
    public ResponseEntity<GatewayHashResponse> generateHash(
            @Valid @RequestBody GatewayHashRequest request) {

        logger.info("Received hash generation request for txnId: {}", request.getTxnId());

        GatewayHashResponse response = gatewayHashService.generateHash(request);

        logger.info("Hash generated successfully for txnId: {}", request.getTxnId());

        return ResponseEntity.ok(response);
    }
}
