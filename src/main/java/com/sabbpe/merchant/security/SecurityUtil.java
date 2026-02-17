package com.sabbpe.merchant.security;

import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;

/**
 * Security Utility Component
 * Provides helper methods for extracting authenticated merchant ID and validating ownership
 */
@Slf4j
@Component
public class SecurityUtil {

    /**
     * Extract merchant ID from JWT token (from SecurityContext)
     * @return merchant ID if authenticated, null otherwise
     */
    public String getAuthenticatedMerchantId() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        
        if (authentication != null && authentication.isAuthenticated()) {
            String merchantId = (String) authentication.getPrincipal();
            log.debug("Extracted merchantId from JWT token: {}", merchantId);
            return merchantId;
        }
        
        return null;
    }

    /**
     * Validate merchant ownership - checks if JWT token merchant matches request merchant
     * @param requestMerchantId merchant ID from request payload
     * @throws IllegalArgumentException if merchant IDs don't match or not authenticated
     */
    public void validateMerchantOwnership(String requestMerchantId) {
        String tokenMerchantId = getAuthenticatedMerchantId();
        
        if (tokenMerchantId == null) {
            log.warn("No authenticated merchant found in SecurityContext");
            throw new IllegalArgumentException("No authenticated merchant found");
        }
        
        if (!tokenMerchantId.equals(requestMerchantId)) {
            log.warn("Merchant ownership validation failed. Token merchant: {}, Request merchant: {}",
                    tokenMerchantId, requestMerchantId);
            throw new IllegalArgumentException(
                    "Access denied: Merchant ID in token does not match request"
            );
        }
        
        log.debug("Merchant ownership validation successful for merchantId: {}", tokenMerchantId);
    }

    /**
     * Create error response for authorization failures
     * @param message error message
     * @return ResponseEntity with error response
     */
    public ResponseEntity<?> createAuthorizationErrorResponse(String message) {
        Map<String, Object> errorResponse = new HashMap<>();
        errorResponse.put("success", false);
        errorResponse.put("message", message);
        errorResponse.put("status", HttpStatus.FORBIDDEN.value());
        
        return ResponseEntity.status(HttpStatus.FORBIDDEN).body(errorResponse);
    }

    /**
     * Create error response for authentication failures
     * @param message error message
     * @return ResponseEntity with error response
     */
    public ResponseEntity<?> createAuthenticationErrorResponse(String message) {
        Map<String, Object> errorResponse = new HashMap<>();
        errorResponse.put("success", false);
        errorResponse.put("message", message);
        errorResponse.put("status", HttpStatus.UNAUTHORIZED.value());
        
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(errorResponse);
    }
}
