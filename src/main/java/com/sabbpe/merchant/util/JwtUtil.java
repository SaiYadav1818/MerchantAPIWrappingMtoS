package com.sabbpe.merchant.util;

import com.sabbpe.merchant.exception.UnauthorizedException;
import org.springframework.stereotype.Component;

import java.util.Base64;

/**
 * Utility class for JWT token processing
 */
@Component
public class JwtUtil {

    /**
     * Extracts merchantId from JWT token
     * Expects JWT format: Header.Payload.Signature
     * Payload contains merchantId claim
     * 
     * @param token JWT token from Authorization header
     * @return merchantId extracted from token
     * @throws UnauthorizedException if token is invalid or merchantId not found
     */
    public String extractMerchantIdFromToken(String token) {
        try {
            if (token == null || token.isEmpty()) {
                throw new UnauthorizedException("Missing JWT token");
            }

            // Remove "Bearer " prefix if present
            String cleanToken = token.startsWith("Bearer ") ? token.substring(7) : token;

            // Split token into parts
            String[] parts = cleanToken.split("\\.");
            if (parts.length != 3) {
                throw new UnauthorizedException("Invalid JWT format");
            }

            // Decode payload (second part)
            String payload = new String(Base64.getUrlDecoder().decode(parts[1]));

            // Extract merchantId from payload JSON
            // Assumes payload contains: {"merchantId":"...", ...}
            String merchantId = extractJsonValue(payload, "merchantId");
            
            if (merchantId == null || merchantId.isEmpty()) {
                throw new UnauthorizedException("merchantId not found in JWT token");
            }

            return merchantId;

        } catch (UnauthorizedException e) {
            throw e;
        } catch (Exception e) {
            throw new UnauthorizedException("Invalid JWT token: " + e.getMessage(), e);
        }
    }

    /**
     * Extracts a JSON field value from a simple JSON string
     * This is a basic implementation - use a proper JSON parser for production
     */
    private String extractJsonValue(String json, String key) {
        String searchKey = "\"" + key + "\":\"";
        int startIndex = json.indexOf(searchKey);
        
        if (startIndex == -1) {
            return null;
        }

        startIndex += searchKey.length();
        int endIndex = json.indexOf("\"", startIndex);

        if (endIndex == -1) {
            return null;
        }

        return json.substring(startIndex, endIndex);
    }

    /**
     * Validates Authorization header format
     */
    public String extractTokenFromAuthHeader(String authHeader) {
        if (authHeader == null || authHeader.isEmpty()) {
            throw new UnauthorizedException("Missing Authorization header");
        }

        if (!authHeader.startsWith("Bearer ")) {
            throw new UnauthorizedException("Invalid Authorization header format. Expected: Bearer <token>");
        }

        return authHeader.substring(7);
    }
}
