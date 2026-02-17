package com.sabbpe.merchant.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Unified error response DTO for API failures
 * Used by all endpoints to return consistent error messages
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ErrorResponse {

    @JsonProperty("status")
    private String status;  // "FAILURE" | "ERROR"

    @JsonProperty("message")
    private String message;  // User-friendly error message

    @JsonProperty("error_code")
    private String errorCode;  // Machine-readable error code

    @JsonProperty("error_type")
    private String errorType;  // Type of error (e.g., "DUPLICATE_TRANSACTION", "GATEWAY_ERROR")

    @JsonProperty("details")
    private String details;  // Additional technical details

    @JsonProperty("request_id")
    private String requestId;  // For correlation/debugging

    @JsonProperty("timestamp")
    private Long timestamp;

    // Factory methods for common error scenarios
    public static ErrorResponse duplicateTransaction(String transactionId) {
        return ErrorResponse.builder()
                .status("FAILURE")
                .message("Duplicate transaction id")
                .errorCode("DUPLICATE_TXN")
                .errorType("DUPLICATE_TRANSACTION")
                .details("Transaction " + transactionId + " has already been processed")
                .timestamp(System.currentTimeMillis())
                .build();
    }

    public static ErrorResponse gatewayError(String message, String errorCode) {
        return ErrorResponse.builder()
                .status("FAILURE")
                .message(message)
                .errorCode(errorCode)
                .errorType("GATEWAY_ERROR")
                .timestamp(System.currentTimeMillis())
                .build();
    }

    public static ErrorResponse validationError(String message) {
        return ErrorResponse.builder()
                .status("FAILURE")
                .message(message)
                .errorCode("VALIDATION_ERROR")
                .errorType("VALIDATION_ERROR")
                .timestamp(System.currentTimeMillis())
                .build();
    }
}
