package com.sabbpe.merchant.service;

import com.sabbpe.merchant.dto.GatewayHashRequest;
import com.sabbpe.merchant.dto.GatewayHashResponse;
import com.sabbpe.merchant.util.HashUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.text.DecimalFormat;

/**
 * Service for generating Easebuzz payment gateway hash
 * 
 * Handles the generation of SHA-512 hashes for Easebuzz payment gateway integration
 * Reads merchant key and salt from application properties
 */
@Service
public class GatewayHashService {

    private static final Logger logger = LoggerFactory.getLogger(GatewayHashService.class);

    @Value("${easebuzz.key:#{null}}")
    private String easebuzzKey;

    @Value("${easebuzz.salt:#{null}}")
    private String easebuzzSalt;

    /**
     * Generate Easebuzz payment gateway hash
     * 
     * Hash Format:
     * key|txnid|amount|productinfo|firstname|email|||||||||||salt
     * 
     * @param request GatewayHashRequest containing payment details
     * @return GatewayHashResponse with generated hash
     * @throws IllegalArgumentException if configuration is missing or request data is invalid
     */
    public GatewayHashResponse generateHash(GatewayHashRequest request) {
        logger.info("Generating Easebuzz payment gateway hash for txnId: {}", request.getTxnId());

        // Validate configuration
        if (easebuzzKey == null || easebuzzKey.trim().isEmpty()) {
            logger.error("Easebuzz key not configured in application.properties");
            throw new IllegalArgumentException("Easebuzz gateway key is not configured");
        }

        if (easebuzzSalt == null || easebuzzSalt.trim().isEmpty()) {
            logger.error("Easebuzz salt not configured in application.properties");
            throw new IllegalArgumentException("Easebuzz gateway salt is not configured");
        }

        try {
            // Format amount to 2 decimal places
            String formattedAmount = formatAmount(request.getAmount());
            logger.debug("Amount formatted to: {}", formattedAmount);

            // Build hash string with EXACT format
            // key|txnid|amount|productinfo|firstname|email|||||||||||salt
            // Where ||| after email represents 11 empty fields
            String hashString = buildHashString(
                    easebuzzKey,
                    request.getTxnId(),
                    formattedAmount,
                    request.getProductInfo(),
                    request.getFirstName(),
                    request.getEmail(),
                    easebuzzSalt
            );

            logger.debug("Hash input constructed (salt not logged for security)");

            // Generate SHA-512 hash
            String hash = HashUtil.generateSHA512(hashString);

            logger.info("Hash generated successfully for txnId: {}", request.getTxnId());
            logger.debug("Hash length: {} characters (SHA-512 hex = 128)", hash.length());

            return GatewayHashResponse.builder()
                    .status("SUCCESS")
                    .hash(hash)
                    .timestamp(System.currentTimeMillis())
                    .build();

        } catch (Exception e) {
            logger.error("Error generating hash for txnId: {}, error: {}", request.getTxnId(), e.getMessage(), e);
            throw new RuntimeException("Hash generation failed: " + e.getMessage(), e);
        }
    }

    /**
     * Build hash string with exact Easebuzz format
     * 
     * Format: key|txnid|amount|productinfo|firstname|email|||||||||||salt
     * 
     * @param key Easebuzz merchant key
     * @param txnId Transaction ID
     * @param amount Formatted amount (2 decimal places)
     * @param productInfo Product information
     * @param firstName Customer first name
     * @param email Customer email
     * @param salt Easebuzz merchant salt
     * @return Complete hash string
     */
    private String buildHashString(
            String key,
            String txnId,
            String amount,
            String productInfo,
            String firstName,
            String email,
            String salt) {

        // Build string with exact format: key|txnid|amount|productinfo|firstname|email|||||||||||salt
        // The 11 pipes represent 11 empty fields after email
        StringBuilder hashString = new StringBuilder();
        hashString.append(key).append("|")
                  .append(txnId).append("|")
                  .append(amount).append("|")
                  .append(productInfo).append("|")
                  .append(firstName).append("|")
                  .append(email)
                  .append("|||||||||||")  // 11 empty fields
                  .append(salt);

        return hashString.toString();
    }

    /**
     * Format amount to 2 decimal places
     * 
     * @param amount BigDecimal amount
     * @return Formatted amount string with exactly 2 decimal places
     */
    private String formatAmount(BigDecimal amount) {
        if (amount == null) {
            throw new IllegalArgumentException("Amount cannot be null");
        }

        DecimalFormat df = new DecimalFormat("0.00");
        df.setMaximumFractionDigits(2);
        df.setMinimumFractionDigits(2);
        df.setGroupingUsed(false);

        return df.format(amount);
    }
}
