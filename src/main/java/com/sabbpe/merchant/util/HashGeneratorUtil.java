package com.sabbpe.merchant.util;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

/**
 * Utility class for generating SHA-512 hash for Easebuzz payment gateway
 * Hash sequence: key|txnid|amount|productinfo|firstname|email|udf1|udf2|udf3|udf4|udf5|udf6|udf7|udf8|udf9|udf10|salt
 */
@Slf4j
@Component
public class HashGeneratorUtil {

    private static final String HASH_ALGORITHM = "SHA-512";
    private static final String SEPARATOR = "|";

    /**
     * Generate SHA-512 hash using the Easebuzz hash sequence
     *
     * @param key         Easebuzz API key
     * @param txnid       Transaction ID
     * @param amount      Payment amount
     * @param productinfo Product information
     * @param firstname   Customer first name
     * @param email       Customer email
     * @param udf1        User defined field 1
     * @param udf2        User defined field 2
     * @param udf3        User defined field 3
     * @param udf4        User defined field 4
     * @param udf5        User defined field 5
     * @param udf6        User defined field 6
     * @param udf7        User defined field 7
     * @param udf8        User defined field 8
     * @param udf9        User defined field 9
     * @param udf10       User defined field 10
     * @param salt        Easebuzz salt
     * @return SHA-512 hash string
     * @throws RuntimeException if SHA-512 algorithm is not available
     */
    public String generateHash(String key, String txnid, BigDecimal amount, String productinfo,
                               String firstname, String email, String udf1, String udf2,
                               String udf3, String udf4, String udf5, String udf6,
                               String udf7, String udf8, String udf9, String udf10, String salt) {

        try {
            // Build hash string in the required sequence
            String hashString = buildHashString(key, txnid, amount, productinfo, firstname,
                    email, udf1, udf2, udf3, udf4, udf5, udf6, udf7, udf8, udf9, udf10, salt);

            log.debug("Hash input string: {}", hashString);

            // Generate SHA-512 hash
            MessageDigest messageDigest = MessageDigest.getInstance(HASH_ALGORITHM);
            byte[] hashBytes = messageDigest.digest(hashString.getBytes(StandardCharsets.UTF_8));

            // Convert to hex string
            String hash = bytesToHex(hashBytes);

            log.debug("Generated hash: {}", hash);
            return hash;

        } catch (NoSuchAlgorithmException e) {
            log.error("SHA-512 algorithm not available", e);
            throw new RuntimeException("Failed to generate SHA-512 hash: " + e.getMessage(), e);
        }
    }

    /**
     * Build the hash string in the required sequence
     */
    private String buildHashString(String key, String txnid, BigDecimal amount, String productinfo,
                                   String firstname, String email, String udf1, String udf2,
                                   String udf3, String udf4, String udf5, String udf6,
                                   String udf7, String udf8, String udf9, String udf10, String salt) {

        return key + SEPARATOR +
                txnid + SEPARATOR +
                amount + SEPARATOR +
                productinfo + SEPARATOR +
                firstname + SEPARATOR +
                email + SEPARATOR +
                (udf1 != null ? udf1 : "") + SEPARATOR +
                (udf2 != null ? udf2 : "") + SEPARATOR +
                (udf3 != null ? udf3 : "") + SEPARATOR +
                (udf4 != null ? udf4 : "") + SEPARATOR +
                (udf5 != null ? udf5 : "") + SEPARATOR +
                (udf6 != null ? udf6 : "") + SEPARATOR +
                (udf7 != null ? udf7 : "") + SEPARATOR +
                (udf8 != null ? udf8 : "") + SEPARATOR +
                (udf9 != null ? udf9 : "") + SEPARATOR +
                (udf10 != null ? udf10 : "") + SEPARATOR +
                salt;
    }

    /**
     * Convert byte array to hexadecimal string
     */
    private String bytesToHex(byte[] bytes) {
        StringBuilder hexString = new StringBuilder();
        for (byte b : bytes) {
            String hex = Integer.toHexString(0xff & b);
            if (hex.length() == 1) {
                hexString.append('0');
            }
            hexString.append(hex);
        }
        return hexString.toString();
    }
}
