package com.sabbpe.merchant.exception;

import com.sabbpe.merchant.dto.PaymentResponse;
import com.sabbpe.merchant.dto.EasebuzzPaymentResponse;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.WebRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.Map;

@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger logger = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @ExceptionHandler(UnauthorizedException.class)
    public ResponseEntity<EasebuzzPaymentResponse> handleUnauthorizedException(
            UnauthorizedException ex, WebRequest request) {

        logger.error("Unauthorized exception: {}", ex.getMessage());

        EasebuzzPaymentResponse response =
                EasebuzzPaymentResponse.error("FAILURE", ex.getMessage(), "UNAUTHORIZED");

        return ResponseEntity
                .status(HttpStatus.UNAUTHORIZED)
                .body(response);
    }

    @ExceptionHandler(ValidationException.class)
    public ResponseEntity<EasebuzzPaymentResponse> handleValidationException(
            ValidationException ex, WebRequest request) {

        logger.error("Validation exception: {}", ex.getMessage());

        EasebuzzPaymentResponse response =
                EasebuzzPaymentResponse.error("FAILURE", ex.getMessage(), "VALIDATION_ERROR");

        return ResponseEntity
                .status(HttpStatus.BAD_REQUEST)
                .body(response);
    }

    @ExceptionHandler(GatewayException.class)
    public ResponseEntity<EasebuzzPaymentResponse> handleGatewayException(
            GatewayException ex, WebRequest request) {

        logger.error("Gateway exception: {}", ex.getMessage(), ex);

        EasebuzzPaymentResponse response =
                EasebuzzPaymentResponse.error("FAILURE", ex.getMessage(), "GATEWAY_ERROR");

        return ResponseEntity
                .status(HttpStatus.BAD_GATEWAY)
                .body(response);
    }

    @ExceptionHandler(HashMismatchException.class)
    public ResponseEntity<PaymentResponse> handleHashMismatchException(
            HashMismatchException ex, WebRequest request) {

        logger.error("Hash mismatch exception: {}", ex.getMessage());

        PaymentResponse response = PaymentResponse.builder()
                .status("FAILURE")
                .message(ex.getMessage())
                .errorCode("HASH_MISMATCH")
                .build();

        return ResponseEntity
                .status(HttpStatus.BAD_REQUEST)
                .body(response);
    }

    @ExceptionHandler(MerchantNotFoundException.class)
    public ResponseEntity<PaymentResponse> handleMerchantNotFoundException(
            MerchantNotFoundException ex, WebRequest request) {

        logger.error("Merchant not found exception: {}", ex.getMessage());

        PaymentResponse response = PaymentResponse.builder()
                .status("FAILURE")
                .message(ex.getMessage())
                .errorCode("MERCHANT_NOT_FOUND")
                .build();

        return ResponseEntity
                .status(HttpStatus.UNAUTHORIZED)
                .body(response);
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<Map<String, Object>> handleMethodArgumentNotValidException(
            MethodArgumentNotValidException ex, WebRequest request) {

        logger.error("Validation exception: {}", ex.getMessage());

        Map<String, String> errors = new HashMap<>();
        ex.getBindingResult().getAllErrors().forEach((error) -> {
            String fieldName = ((FieldError) error).getField();
            String errorMessage = error.getDefaultMessage();
            errors.put(fieldName, errorMessage);
        });

        Map<String, Object> response = new HashMap<>();
        response.put("status", "FAILURE");
        response.put("errorCode", "VALIDATION_FAILED");
        response.put("message", "Request validation failed");
        response.put("errors", errors);

        return ResponseEntity
                .status(HttpStatus.BAD_REQUEST)
                .body(response);
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<Map<String, Object>> handleIllegalArgumentException(
            IllegalArgumentException ex, WebRequest request) {

        logger.error("Illegal argument exception: {}", ex.getMessage());

        Map<String, Object> response = new HashMap<>();
        response.put("status", "FAILURE");
        response.put("errorCode", "INVALID_ARGUMENT");
        response.put("message", ex.getMessage());
        response.put("timestamp", System.currentTimeMillis());

        return ResponseEntity
                .status(HttpStatus.BAD_REQUEST)
                .body(response);
    }

    @ExceptionHandler(RuntimeException.class)
    public ResponseEntity<Map<String, Object>> handleRuntimeException(
            RuntimeException ex, WebRequest request) {

        logger.error("Runtime exception: {}", ex.getMessage(), ex);

        Map<String, Object> response = new HashMap<>();
        response.put("status", "FAILURE");
        response.put("errorCode", "RUNTIME_ERROR");
        response.put("message", ex.getMessage());
        response.put("timestamp", System.currentTimeMillis());

        return ResponseEntity
                .status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(response);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<EasebuzzPaymentResponse> handleGenericException(
            Exception ex, WebRequest request) {

        logger.error("Unexpected exception: {}", ex.getMessage(), ex);

        EasebuzzPaymentResponse response =
                EasebuzzPaymentResponse.error("FAILURE",
                        "An unexpected error occurred",
                        "INTERNAL_SERVER_ERROR");

        return ResponseEntity
                .status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(response);
    }
}
