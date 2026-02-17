package com.sabbpe.merchant.controller;

import com.sabbpe.merchant.dto.EasebuzzInitiateRequest;
import com.sabbpe.merchant.dto.EasebuzzPaymentResponse;
import com.sabbpe.merchant.dto.ErrorResponse;
import com.sabbpe.merchant.exception.GatewayException;
import com.sabbpe.merchant.service.EasebuzzService;
import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * REST Controller for Easebuzz payment gateway integration
 * Handles payment initiation requests and communicates with Easebuzz
 * 
 * Error Handling Strategy:
 * - Duplicate Transaction: Returns 409 (Conflict) with ErrorResponse
 * - Validation Error: Returns 400 (Bad Request)
 * - Gateway Error: Returns 502 (Bad Gateway)
 * - Generic Error: Returns 500 (Internal Server Error)
 */
@Slf4j
@RestController
@RequestMapping("/api/payment")
public class EasebuzzPaymentController {

    private final EasebuzzService easebuzzService;

    @Value("${easebuzz.key}")
    private String easebuzzKey;

    @Value("${easebuzz.surl}")
    private String successUrl;

    @Value("${easebuzz.furl}")
    private String failureUrl;

    @Autowired
    public EasebuzzPaymentController(EasebuzzService easebuzzService) {
        this.easebuzzService = easebuzzService;
    }

    /**
     * Initiate payment with Easebuzz gateway
     *
     * @param initiateRequest Request body containing payment details (txnid, amount, productinfo, firstname, phone, email)
     * @return EasebuzzPaymentResponse with payment URL for successful initiation
     *         or ErrorResponse with detailed error information for failures
     */
    @PostMapping("/easebuzz/initiate")
    public ResponseEntity<?> initiatePayment(
            @Valid @RequestBody EasebuzzInitiateRequest initiateRequest) {

        try {
            log.info("Payment initiation request received for transaction: {}", initiateRequest.getTxnid());

            // Auto-fill Easebuzz key
            initiateRequest.setKey(easebuzzKey);

            // Set success and failure URLs
            if (initiateRequest.getSurl() == null || initiateRequest.getSurl().isEmpty()) {
                initiateRequest.setSurl(successUrl);
            }
            if (initiateRequest.getFurl() == null || initiateRequest.getFurl().isEmpty()) {
                initiateRequest.setFurl(failureUrl);
            }

            // Set placeholder hash (will be generated in service)
            initiateRequest.setHash("");

            log.debug("Calling EasebuzzService for transaction: {}", initiateRequest.getTxnid());

            // Call service to initiate payment
            EasebuzzPaymentResponse response = easebuzzService.initiatePayment(initiateRequest);

            log.info("Payment initiated successfully for transaction: {}", initiateRequest.getTxnid());

            return ResponseEntity.ok(response);

        } catch (GatewayException ex) {
            // Handle Easebuzz gateway errors with detailed error response
            return handleGatewayException(ex, initiateRequest.getTxnid());

        } catch (IllegalArgumentException e) {
            log.error("Invalid request parameter for transaction initiation: {}", e.getMessage());
            ErrorResponse errorResponse = ErrorResponse.validationError(e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(errorResponse);

        } catch (Exception e) {
            log.error("Unexpected error initiating payment for transaction: {}", 
                    initiateRequest.getTxnid(), e);
            ErrorResponse errorResponse = ErrorResponse.builder()
                    .status("FAILURE")
                    .message("Payment initiation failed due to an unexpected error")
                    .errorCode("INTERNAL_ERROR")
                    .errorType("UNEXPECTED_ERROR")
                    .details(e.getMessage())
                    .timestamp(System.currentTimeMillis())
                    .build();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
        }
    }

    /**
     * Handle GatewayException and return appropriate HTTP status and error response
     * 
     * Status Code Selection:
     * - 409 Conflict: For duplicate transaction IDs
     * - 502 Bad Gateway: For gateway communication errors
     * - 500 Internal Server Error: For other errors
     * 
     * @param ex The GatewayException thrown by service
     * @param transactionId The transaction ID for logging
     * @return ResponseEntity with appropriate HTTP status and error details
     */
    private ResponseEntity<?> handleGatewayException(GatewayException ex, String transactionId) {
        ErrorResponse errorResponse = ex.getErrorResponse();

        // If no error response attached, create one from exception message
        if (errorResponse == null) {
            errorResponse = ErrorResponse.gatewayError(
                    ex.getMessage(),
                    "GATEWAY_ERROR"
            );
        }

        // Determine HTTP status code based on error type
        HttpStatus httpStatus = determineHttpStatus(errorResponse);

        log.warn("Gateway exception handled for TxnId: {}, Status: {}, Message: {}",
                transactionId, httpStatus.value(), errorResponse.getMessage());

        return ResponseEntity.status(httpStatus).body(errorResponse);
    }

    /**
     * Determine appropriate HTTP status code based on error response type
     * 
     * @param errorResponse The error response containing error type
     * @return HttpStatus to return to client
     */
    private HttpStatus determineHttpStatus(ErrorResponse errorResponse) {
        if (errorResponse == null || errorResponse.getErrorType() == null) {
            return HttpStatus.INTERNAL_SERVER_ERROR;
        }

        return switch (errorResponse.getErrorType()) {
            case "DUPLICATE_TRANSACTION" -> HttpStatus.CONFLICT;  // 409
            case "VALIDATION_ERROR" -> HttpStatus.BAD_REQUEST;     // 400
            case "GATEWAY_ERROR" -> HttpStatus.BAD_GATEWAY;        // 502
            case "GATEWAY_RETRY" -> HttpStatus.SERVICE_UNAVAILABLE; // 503
            default -> HttpStatus.INTERNAL_SERVER_ERROR;           // 500
        };
    }
}
