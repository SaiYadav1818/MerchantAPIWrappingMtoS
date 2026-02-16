package com.sabbpe.merchant.util;

import org.springframework.stereotype.Component;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

/**
 * Utility class for Easebuzz hash generation
 * Handles SHA-512 hashing for Easebuzz payment gateway
 * 
 * Hash Format:
 * key|txnid|amount|productinfo|firstname|email|udf1|udf2|udf3|udf4|udf5|||||salt
 */
@Component
public class EasebuzzHashUtil {

    private static final String SHA_512 = "SHA-512";
    private static final String HEX_DIGITS = "0123456789abcdef";

    /**
     * Generate Easebuzz hash with exact format
     * 
     * Hash String Format:
     * key|txnid|amount|productinfo|firstname|email|udf1|udf2|udf3|udf4|udf5|||||salt
     * 
     * @param key Merchant key from Easebuzz
     * @param txnid Transaction ID
     * @param amount Payment amount
     * @param productinfo Product information
     * @param firstname First name
     * @param email Email address
     * @param salt Merchant salt from Easebuzz
     * @return SHA-512 hash as hexadecimal string
     */
    public static String generateHash(
            String key,
            String txnid,
            String amount,
            String productinfo,
            String firstname,
            String email,
            String salt) {
        
        return generateHash(key, txnid, amount, productinfo, firstname, email, "", "", "", "", "", salt);
    }

    /**
     * Generate Easebuzz hash with UDF fields
     * 
     * @param key Merchant key
     * @param txnid Transaction ID
     * @param amount Amount
     * @param productinfo Product info
     * @param firstname First name
     * @param email Email
     * @param udf1 UDF field 1 (typically internal_token)
     * @param udf2 UDF field 2
     * @param udf3 UDF field 3
     * @param udf4 UDF field 4
     * @param udf5 UDF field 5
     * @param salt Salt key
     * @return SHA-512 hash
     */
    public static String generateHash(
            String key,
            String txnid,
            String amount,
            String productinfo,
            String firstname,
            String email,
            String udf1,
            String udf2,
            String udf3,
            String udf4,
            String udf5,
            String salt) {
        
        // Build hash string with exact format
        // key|txnid|amount|productinfo|firstname|email|udf1|udf2|udf3|udf4|udf5|||||salt
        StringBuilder hashString = new StringBuilder();
        hashString.append(key != null ? key : "").append("|")
                  .append(txnid != null ? txnid : "").append("|")
                  .append(amount != null ? amount : "").append("|")
                  .append(productinfo != null ? productinfo : "").append("|")
                  .append(firstname != null ? firstname : "").append("|")
                  .append(email != null ? email : "").append("|")
                  .append(udf1 != null ? udf1 : "").append("|")
                  .append(udf2 != null ? udf2 : "").append("|")
                  .append(udf3 != null ? udf3 : "").append("|")
                  .append(udf4 != null ? udf4 : "").append("|")
                  .append(udf5 != null ? udf5 : "").append("|")
                  .append("||||") // Four empty fields (total 5 pipes between udf5 and salt)
                  .append(salt != null ? salt : "");
        
        return generateSHA512(hashString.toString());
    }

    /**
     * Generate reverse hash for callback verification
     * 
     * Format for reverse hash (callback):
     * salt|status||||||||email|firstname|productinfo|amount|txnid|key
     * 
     * @param salt Salt key
     * @param status Payment status from gateway
     * @param email Email
     * @param firstname First name
     * @param productinfo Product info
     * @param amount Amount
     * @param txnid Transaction ID
     * @param key Merchant key
     * @return SHA-512 hash
     */
    public static String generateReverseHash(
            String salt,
            String status,
            String email,
            String firstname,
            String productinfo,
            String amount,
            String txnid,
            String key) {
        
        // Build reverse hash string
        StringBuilder reverseHashString = new StringBuilder();
        reverseHashString.append(salt != null ? salt : "").append("|")
                         .append(status != null ? status : "").append("|")
                         .append("||||||||") // Eight empty fields
                         .append(email != null ? email : "").append("|")
                         .append(firstname != null ? firstname : "").append("|")
                         .append(productinfo != null ? productinfo : "").append("|")
                         .append(amount != null ? amount : "").append("|")
                         .append(txnid != null ? txnid : "").append("|")
                         .append(key != null ? key : "");
        
        return generateSHA512(reverseHashString.toString());
    }

    /**
     * Generates SHA-512 hash for input string
     * 
     * @param input The input string to hash
     * @return SHA-512 hash as hexadecimal string
     * @throws RuntimeException if SHA-512 algorithm not available
     */
    private static String generateSHA512(String input) {
        try {
            MessageDigest digest = MessageDigest.getInstance(SHA_512);
            byte[] encodedhash = digest.digest(input.getBytes());
            return bytesToHexString(encodedhash);
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("SHA-512 algorithm not available", e);
        }
    }

    /**
     * Converts byte array to hexadecimal string
     * 
     * @param bytes The byte array to convert
     * @return Hexadecimal string representation
     */
    private static String bytesToHexString(byte[] bytes) {
        StringBuilder sb = new StringBuilder();
        for (byte b : bytes) {
            int index = (b & 0xff) >> 4;
            sb.append(HEX_DIGITS.charAt(index));
            index = b & 0xf;
            sb.append(HEX_DIGITS.charAt(index));
        }
        return sb.toString();
    }
}
