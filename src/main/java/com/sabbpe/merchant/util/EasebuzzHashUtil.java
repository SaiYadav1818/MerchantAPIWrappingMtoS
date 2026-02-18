package com.sabbpe.merchant.util;

import org.springframework.stereotype.Component;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

/**
 * Utility class for Easebuzz hash generation
 * Handles SHA-512 hashing for Easebuzz payment gateway with full UDF1-UDF10 support
 * 
 * Hash Format (Forward):
 * key|txnid|amount|productinfo|firstname|email|udf1|udf2|udf3|udf4|udf5|udf6|udf7|udf8|udf9|udf10|salt
 * 
 * Hash Format (Reverse/Callback):
 * salt|status|udf10|udf9|udf8|udf7|udf6|udf5|udf4|udf3|udf2|udf1|email|firstname|productinfo|amount|txnid|key
 */
@Component
public class EasebuzzHashUtil {

    private static final String SHA_512 = "SHA-512";
    private static final String HEX_DIGITS = "0123456789abcdef";

    /**
     * Generate Easebuzz hash with exact format (no UDF fields)
     * key|txnid|amount|productinfo|firstname|email|salt
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
        
        return generateHashWithUDF(key, txnid, amount, productinfo, firstname, email, 
                null, null, null, null, null, null, null, null, null, null, salt);
    }

    /**
     * Generate Easebuzz hash with UDF fields (UDF1-UDF10)
     * 
     * Hash Format:
     * key|txnid|amount|productinfo|firstname|email|udf1|udf2|udf3|udf4|udf5|udf6|udf7|udf8|udf9|udf10|salt
     * 
     * @param key Merchant key
     * @param txnid Transaction ID
     * @param amount Amount
     * @param productinfo Product info
     * @param firstname First name
     * @param email Email
     * @param udf1 UDF field 1 (typically merchant ID)
     * @param udf2 UDF field 2 (typically order ID)
     * @param udf3 UDF field 3
     * @param udf4 UDF field 4
     * @param udf5 UDF field 5
     * @param udf6 UDF field 6
     * @param udf7 UDF field 7
     * @param udf8 UDF field 8
     * @param udf9 UDF field 9
     * @param udf10 UDF field 10
     * @param salt Salt key
     * @return SHA-512 hash
     */
    public static String generateHashWithUDF(
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
            String udf6,
            String udf7,
            String udf8,
            String udf9,
            String udf10,
            String salt) {
        
        // Build hash string with exact format (nulls become empty strings)
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
                  .append(udf6 != null ? udf6 : "").append("|")
                  .append(udf7 != null ? udf7 : "").append("|")
                  .append(udf8 != null ? udf8 : "").append("|")
                  .append(udf9 != null ? udf9 : "").append("|")
                  .append(udf10 != null ? udf10 : "").append("|")
                  .append(salt != null ? salt : "");
        
        return generateSHA512(hashString.toString());
    }

    /**
     * Generate Easebuzz hash with UDF fields (backward compatible - UDF1-UDF5 only)
     * Deprecated: Use generateHashWithUDF() for full UDF1-UDF10 support
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
     * @deprecated Use generateHashWithUDF() for full UDF1-UDF10 support
     */
    @Deprecated(since = "1.1", forRemoval = true)
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
        
        return generateHashWithUDF(key, txnid, amount, productinfo, firstname, email,
                udf1, udf2, udf3, udf4, udf5, null, null, null, null, null, salt);
    }

    /**
     * Generate reverse hash for callback verification with full UDF support
     * 
     * Format for reverse hash (callback):
     * salt|status|udf10|udf9|udf8|udf7|udf6|udf5|udf4|udf3|udf2|udf1|email|firstname|productinfo|amount|txnid|key
     * 
     * @param salt Salt key
     * @param status Payment status from gateway
     * @param udf10 UDF 10
     * @param udf9 UDF 9
     * @param udf8 UDF 8
     * @param udf7 UDF 7
     * @param udf6 UDF 6
     * @param udf5 UDF 5
     * @param udf4 UDF 4
     * @param udf3 UDF 3
     * @param udf2 UDF 2
     * @param udf1 UDF 1
     * @param email Email
     * @param firstname First name
     * @param productinfo Product info
     * @param amount Amount
     * @param txnid Transaction ID
     * @param key Merchant key
     * @return SHA-512 hash for verification
     */
    public static String generateReverseHashWithUDF(
            String salt,
            String status,
            String udf10,
            String udf9,
            String udf8,
            String udf7,
            String udf6,
            String udf5,
            String udf4,
            String udf3,
            String udf2,
            String udf1,
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
                         .append(udf10 != null ? udf10 : "").append("|")
                         .append(udf9 != null ? udf9 : "").append("|")
                         .append(udf8 != null ? udf8 : "").append("|")
                         .append(udf7 != null ? udf7 : "").append("|")
                         .append(udf6 != null ? udf6 : "").append("|")
                         .append(udf5 != null ? udf5 : "").append("|")
                         .append(udf4 != null ? udf4 : "").append("|")
                         .append(udf3 != null ? udf3 : "").append("|")
                         .append(udf2 != null ? udf2 : "").append("|")
                         .append(udf1 != null ? udf1 : "").append("|")
                         .append(email != null ? email : "").append("|")
                         .append(firstname != null ? firstname : "").append("|")
                         .append(productinfo != null ? productinfo : "").append("|")
                         .append(amount != null ? amount : "").append("|")
                         .append(txnid != null ? txnid : "").append("|")
                         .append(key != null ? key : "");
        
        return generateSHA512(reverseHashString.toString());
    }

    /**
     * Generate reverse hash for callback verification (backward compatible - UDF1-UDF5 only)
     * Deprecated: Use generateReverseHashWithUDF() for full UDF1-UDF10 support
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
     * @deprecated Use generateReverseHashWithUDF() for full UDF1-UDF10 support
     */
    @Deprecated(since = "1.1", forRemoval = true)
    public static String generateReverseHash(
            String salt,
            String status,
            String email,
            String firstname,
            String productinfo,
            String amount,
            String txnid,
            String key) {
        
        // Build reverse hash string using old format (for backward compatibility)
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
