package com.sabbpe.merchant.util;

import com.sabbpe.merchant.dto.ErrorResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.Map;

/**
 * Utility to parse Easebuzz error responses and extract meaningful error information
 * Specifically handles duplicate transaction ID detection
 * 
 * Easebuzz error format:
 * {
 *   "status": 0,
 *   "error_desc": "Kindly retry your transaction.",
 *   "data": "Transaction cannot be processed. Duplicate transaction id."
 * }
 */
@Slf4j
@Component
public class EasebuzzErrorHandler {

    // Duplicate transaction error indicators
    private static final String[] DUPLICATE_INDICATORS = {
            "duplicate",
            "already processed",
            "already exists",
            "already initiated"
    };

    /**
     * Parse Easebuzz error response and determine if it's a duplicate transaction
     * 
     * @param responseBody The full response body from Easebuzz API
     * @param transactionId The transaction ID (for reference in logs)
     * @return ErrorResponse with appropriate status and message
     */
    public ErrorResponse handleEasebuzzError(Map<String, Object> responseBody, String transactionId) {
        String errorDesc = extractErrorDescription(responseBody);
        String data = extractData(responseBody);

        // Log the full error for debugging
        log.error("Easebuzz API Error - TxnId: {}, Error: {}, Data: {}", 
                transactionId, errorDesc, data);

        // Check if this is a duplicate transaction error
        if (isDuplicateTransaction(errorDesc, data)) {
            log.warn("Duplicate transaction detected for TxnId: {}", transactionId);
            return ErrorResponse.duplicateTransaction(transactionId);
        }

        // Check if it's a retryable error
        if (isRetryableError(errorDesc)) {
            log.info("Retryable error detected for TxnId: {}", transactionId);
            return ErrorResponse.gatewayError(
                    "Payment gateway temporary error. Please retry your transaction.",
                    "GATEWAY_RETRY"
            );
        }

        // Generic gateway error
        String userFriendlyMessage = formatUserMessage(errorDesc);
        return ErrorResponse.gatewayError(userFriendlyMessage, "GATEWAY_ERROR");
    }

    /**
     * Detect if the error is a duplicate transaction.
     * Checks both error_desc and data fields for duplicate indicators
     */
    private boolean isDuplicateTransaction(String errorDesc, String data) {
        String combinedError = (errorDesc + " " + data).toLowerCase();

        for (String indicator : DUPLICATE_INDICATORS) {
            if (combinedError.contains(indicator)) {
                log.debug("Duplicate transaction pattern matched: {}", indicator);
                return true;
            }
        }
        return false;
    }

    /**
     * Detect if the error is retryable (temporary gateway issue)
     */
    private boolean isRetryableError(String errorDesc) {
        if (errorDesc == null) return false;

        String lowerErr = errorDesc.toLowerCase();
        return lowerErr.contains("retry") || 
               lowerErr.contains("timeout") || 
               lowerErr.contains("temporarily") ||
               lowerErr.contains("service unavailable");
    }

    /**
     * Extract error description field from Easebuzz response
     */
    private String extractErrorDescription(Map<String, Object> responseBody) {
        if (responseBody == null) return "";

        Object errorDesc = responseBody.get("error_desc");
        if (errorDesc != null) {
            return errorDesc.toString().trim();
        }

        // Fallback to "error" field if error_desc not present
        Object error = responseBody.get("error");
        if (error != null) {
            return error.toString().trim();
        }

        // Fallback to "message" field
        Object message = responseBody.get("message");
        if (message != null) {
            return message.toString().trim();
        }

        return "";
    }

    /**
     * Extract data field from Easebuzz response (often contains detailed error info)
     */
    private String extractData(Map<String, Object> responseBody) {
        if (responseBody == null) return "";

        Object data = responseBody.get("data");
        if (data != null) {
            return data.toString().trim();
        }
        return "";
    }

    /**
     * Convert Easebuzz error message to user-friendly format
     * Removes technical jargon and provides actionable guidance
     */
    private String formatUserMessage(String errorDesc) {
        if (errorDesc == null || errorDesc.isBlank()) {
            return "Payment initiation failed. Please try again later.";
        }

        // Remove common technical prefixes
        String message = errorDesc
                .replaceAll("(?i)^kindly ", "Please ")
                .replaceAll("(?i)^please ", "Please ")
                .replaceAll("your transaction", "your payment");

        // Capitalize first letter and ensure proper punctuation
        if (message.length() > 0) {
            message = message.substring(0, 1).toUpperCase() + message.substring(1);
            if (!message.endsWith(".")) {
                message += ".";
            }
        }

        return message;
    }

    /**
     * Validate response body format before processing
     */
    public boolean isValidEasebuzzResponse(Map<String, Object> responseBody) {
        return responseBody != null && responseBody.containsKey("status");
    }

    /**
     * Extract status code from Easebuzz response
     */
    public int extractStatus(Object statusObj) {
        if (statusObj instanceof Integer) {
            return (Integer) statusObj;
        }
        if (statusObj instanceof String) {
            try {
                return Integer.parseInt((String) statusObj);
            } catch (NumberFormatException ignored) {
                log.warn("Could not parse status from: {}", statusObj);
            }
        }
        return 0;  // Default to failure if unparseable
    }
}
