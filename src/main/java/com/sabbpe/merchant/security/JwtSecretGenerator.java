package com.sabbpe.merchant.security;

import java.util.Base64;
import java.security.SecureRandom;

/**
 * JWT Secret Generator Utility
 * 
 * Use this class to generate secure Base64-encoded JWT secrets.
 * HS512 requires at least 512 bits (64 bytes).
 * 
 * Run main() method to generate a new secret for your environment.
 * Copy the output and set JWT_SECRET environment variable or update application.yml
 * 
 * SECURITY NOTES:
 * - Generate a new secret per environment (DEV, UAT, PROD)
 * - Store securely using environment variables, not in code
 * - Rotate secrets periodically
 * - Never commit secrets to version control
 */
public class JwtSecretGenerator {

    private static final int MINIMUM_KEY_SIZE_BYTES = 64; // 512 bits for HS512

    /**
     * Generate a secure Base64-encoded JWT secret
     * Produces 64 random bytes (512 bits) encoded in Base64
     * 
     * @return Base64-encoded secure JWT secret
     */
    public static String generateSecureSecret() {
        byte[] secretBytes = new byte[MINIMUM_KEY_SIZE_BYTES];
        new SecureRandom().nextBytes(secretBytes);
        return Base64.getEncoder().encodeToString(secretBytes);
    }

    /**
     * Generate multiple secrets for different environments
     * Each environment should have its own secret
     * 
     * @param count number of secrets to generate
     */
    public static void generateMultipleSecrets(int count) {
        System.out.println("=== JWT SECRET GENERATION (Base64-encoded, 64 bytes each) ===\n");
        for (int i = 1; i <= count; i++) {
            String secret = generateSecureSecret();
            System.out.println("Secret #" + i + ":");
            System.out.println(secret);
            System.out.println();
        }
    }

    /**
     * Main method - Run to generate secrets
     * Usage: java com.sabbpe.merchant.security.JwtSecretGenerator
     */
    public static void main(String[] args) {
        System.out.println("\n╔════════════════════════════════════════════════════════════════╗");
        System.out.println("║        JWT SECRET GENERATOR (HS512 Compliant)                   ║");
        System.out.println("╚════════════════════════════════════════════════════════════════╝\n");

        System.out.println("Generated Secrets (Base64-encoded, minimum 512 bits):\n");

        // Generate 3 secrets (DEV, UAT, PROD)
        generateMultipleSecrets(3);

        System.out.println("\n╔════════════════════════════════════════════════════════════════╗");
        System.out.println("║                    USAGE INSTRUCTIONS                           ║");
        System.out.println("╚════════════════════════════════════════════════════════════════╝\n");

        System.out.println("1. Copy one of the secrets above");
        System.out.println("\n2. Set environment variable:");
        System.out.println("   export JWT_SECRET='<paste-secret-here>'");
        System.out.println("\n3. Or update application.yml:");
        System.out.println("   jwt:");
        System.out.println("     secret: ${JWT_SECRET:<paste-secret-here>}");
        System.out.println("\n4. Restart application");
        System.out.println("\n5. Test by calling /api/auth/login\n");

        System.out.println("⚠️  SECURITY WARNINGS:");
        System.out.println("   - Never commit secrets to Git");
        System.out.println("   - Use environment variables in production");
        System.out.println("   - Rotate secrets periodically");
        System.out.println("   - Each environment needs its own secret");
        System.out.println("   - Store in secure vault (e.g., HashiCorp Vault, AWS Secrets Manager)\n");
    }
}
