package com.sabbpe.merchant.util;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

public class HashUtil {

    private static final String SHA_256 = "SHA-256";
    private static final String SHA_512 = "SHA-512";
    private static final String HEX_DIGITS = "0123456789abcdef";

    /**
     * Generates SHA-256 hash for the input string
     * 
     * @param input The input string to hash
     * @return SHA-256 hash as hexadecimal string
     * @throws RuntimeException if SHA-256 algorithm is not available
     */
    public static String generateSHA256(String input) {
        try {
            MessageDigest digest = MessageDigest.getInstance(SHA_256);
            byte[] encodedhash = digest.digest(input.getBytes());
            return bytesToHexString(encodedhash);
        } catch (NoSuchAlgorithmException e) {
            System.err.println("SHA-256 algorithm not found: " + e.getMessage());
            throw new RuntimeException("SHA-256 algorithm not available", e);
        }
    }

    /**
     * Generates SHA-512 hash for the input string (used for Easebuzz)
     * 
     * @param input The input string to hash
     * @return SHA-512 hash as hexadecimal string
     * @throws RuntimeException if SHA-512 algorithm is not available
     */
    public static String generateSHA512(String input) {
        try {
            MessageDigest digest = MessageDigest.getInstance(SHA_512);
            byte[] encodedhash = digest.digest(input.getBytes());
            return bytesToHexString(encodedhash);
        } catch (NoSuchAlgorithmException e) {
            System.err.println("SHA-512 algorithm not found: " + e.getMessage());
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
