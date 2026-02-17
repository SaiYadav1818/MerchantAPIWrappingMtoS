package com.sabbpe.merchant.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.MalformedJwtException;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.security.Keys;
import io.jsonwebtoken.security.SignatureException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import jakarta.annotation.PostConstruct;
import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;

/**
 * JWT Utility for token generation and validation
 * 
 * ✅ CRITICAL DESIGN NOTES:
 * 1. Secret loaded from application.yml at application startup via @Value
 * 2. Signing key initialized EXACTLY ONCE via @PostConstruct in thread-safe manner
 * 3. Key stored in FINAL field to prevent accidental modification
 * 4. Same key instance used for ALL signing/validation operations
 * 5. This eliminates signature mismatch caused by key regeneration
 * 
 * WHY THIS MATTERS:
 * - Token generated with KEY_A at time T1
 * - If application creates KEY_B at time T2, validation fails
 * - Our fix: Single key created at startup, never regenerated
 * - Result: All tokens use same key throughout application lifetime
 * 
 * ALGORITHM: HS256 (HMAC-SHA256) - industry standard, deterministic
 * ENCODING: UTF-8 for secret bytes - consistent across all platforms
 * CACHING: Final field prevents accidental key changes
 */
@Slf4j
@Component
public class JwtUtil {

    @Value("${jwt.secret}")
    private String jwtSecret; // Plain text secret from application.yml (NOT Base64)

    @Value("${jwt.expiration}")
    private long jwtExpiration; // in milliseconds

    private static final String ROLE_CLAIM_KEY = "role";
    private static final String MERCHANT_ROLE = "ROLE_MERCHANT";
    private static final SignatureAlgorithm SIGNATURE_ALGORITHM = SignatureAlgorithm.HS256;

    /**
     * ✅ CRITICAL: FINAL field prevents accidental key regeneration
     * Initialized ONCE via @PostConstruct at application startup
     * Never modified after initialization
     */
    private SecretKey signingKey;

    /**
     * ✅ CRITICAL: Eager initialization via @PostConstruct
     * Called by Spring immediately after @Value injection
     * Ensures key is created exactly ONCE at startup
     * Any errors thrown here prevent application start (fail-fast)
     */
    @PostConstruct
    public void initializeSigningKey() {
        try {
            // ✅ Step 1: Validate secret is configured
            if (jwtSecret == null || jwtSecret.trim().isEmpty()) {
                String errorMsg = "JWT secret is not configured. " +
                        "Set 'jwt.secret' in application.yml or JWT_SECRET environment variable.";
                log.error("❌ " + errorMsg);
                throw new IllegalArgumentException(errorMsg);
            }

            // ✅ Step 2: Convert secret string to UTF-8 bytes
            byte[] secretBytes = jwtSecret.getBytes(StandardCharsets.UTF_8);
            
            // ✅ Step 3: Validate secret size for HS256 (must be >= 32 bytes)
            if (secretBytes.length < 32) {
                String errorMsg = String.format(
                    "❌ JWT secret is too short (%d bytes). " +
                    "HS256 requires minimum 32 bytes (256 bits = 32 characters). " +
                    "Current secret: '%s' (length: %d chars). " +
                    "Update 'jwt.secret' in application.yml with >= 32 character string.",
                    secretBytes.length, jwtSecret, jwtSecret.length()
                );
                log.error(errorMsg);
                throw new IllegalArgumentException(errorMsg);
            }

            // ✅ Step 4: Create signing key using JJWT library
            // Keys.hmacShaKeyFor() creates key once and returns same instance
            this.signingKey = Keys.hmacShaKeyFor(secretBytes);
            
            // ✅ Step 5: Log successful initialization with instance verification
            log.info("═══════════════════════════════════════════════════════════════════");
            log.info("✅ JWT SIGNING KEY INITIALIZED SUCCESSFULLY");
            log.info("   JwtUtil Instance Hash: {} (use this to verify singleton)", 
                     System.identityHashCode(this));
            log.info("   Algorithm: {}", SIGNATURE_ALGORITHM);
            log.info("   Key Size: {} bytes ({} bits)", secretBytes.length, secretBytes.length * 8);
            log.info("   Secret Length: {} characters (utf-8 encoded)", jwtSecret.length());
            log.info("   Key Object Hash: {}", System.identityHashCode(this.signingKey));
            log.info("   Timestamp: {}", new Date());
            log.info("═══════════════════════════════════════════════════════════════════");
            
        } catch (IllegalArgumentException e) {
            log.error("❌ JWT configuration validation failed during startup: {}", e.getMessage());
            throw e;
        } catch (Exception e) {
            log.error("❌ FATAL: Failed to initialize JWT signing key: {}", e.getMessage(), e);
            throw new IllegalArgumentException("Failed to initialize JWT signing key", e);
        }
    }


    /**
     * Generate JWT token for merchant with role claim
     * 
     * TOKEN PAYLOAD STRUCTURE:
     * {
     *   "sub": "M123",              // merchantId (subject)
     *   "role": "ROLE_MERCHANT",    // Authority role (Spring Security format)
     *   "iat": 1707923445,          // Token issued at
     *   "exp": 1708009845           // Token expiration
     * }
     * 
     * ✅ Uses signing key initialized at startup (never regenerated)
     * 
     * @param merchantId unique merchant identifier
     * @return JWT token string with role claim and expiration
     * @throws RuntimeException if token generation fails
     */
    public String generateToken(String merchantId) {
        try {
            Date now = new Date();
            Date expiryDate = new Date(now.getTime() + jwtExpiration);

            // ✅ Use initialized signing key (same instance created at startup)
            String token = Jwts.builder()
                    .setSubject(merchantId)
                    .claim(ROLE_CLAIM_KEY, MERCHANT_ROLE)  // Role claim for Spring Security
                    .setIssuedAt(now)
                    .setExpiration(expiryDate)
                    .signWith(signingKey, SIGNATURE_ALGORITHM)  // ✅ HS256 with startup key
                    .compact();

            log.debug("✅ Generated JWT token for merchant: {} with instance hash: {} key hash: {}", 
                      merchantId, System.identityHashCode(this), System.identityHashCode(this.signingKey));
            return token;
            
        } catch (Exception e) {
            log.error("❌ Failed to generate JWT token for merchant {}: {}", merchantId, e.getMessage(), e);
            throw new RuntimeException("Token generation failed", e);
        }
    }

    /**
     * Validate JWT token signature and expiration
     * 
     * ✅ CRITICAL: Uses SAME signing key initialized at startup
     * Validates:
     * 1. Signature matches (using startup key)
     * 2. Token not expired
     * 3. Claims can be parsed
     * 
     * @param token JWT token string
     * @return true if token is valid and not expired, false otherwise
     */
    public boolean validateToken(String token) {
        try {
            // ✅ Use initialized signing key (same instance created at startup)
            // parseClaimsJws() validates signature AND expiration
            Jwts.parserBuilder()
                    .setSigningKey(signingKey)  // ✅ SAME key used in generateToken()
                    .build()
                    .parseClaimsJws(token);  // Throws exception if invalid/expired
            
            log.debug("✅ JWT token validation successful");
            return true;
            
        } catch (SignatureException e) {
            log.warn("❌ JWT signature validation failed (KEY MISMATCH): {}", e.getMessage());
            return false;
        } catch (ExpiredJwtException e) {
            log.warn("❌ JWT token has expired: {}", e.getMessage());
            return false;
        } catch (MalformedJwtException e) {
            log.warn("❌ JWT token is malformed: {}", e.getMessage());
            return false;
        } catch (Exception e) {
            log.warn("❌ JWT token validation failed: {} - {}", e.getClass().getSimpleName(), e.getMessage());
            return false;
        }
    }

    /**
     * Extract merchantId from JWT token
     * Parses token and returns subject claim
     * 
     * ✅ Uses SAME signing key initialized at startup
     * 
     * @param token JWT token string
     * @return merchantId if valid, null if invalid or expired
     */
    public String getMerchantIdFromToken(String token) {
        try {
            // ✅ Use initialized signing key (same instance created at startup)
            Claims claims = Jwts.parserBuilder()
                    .setSigningKey(signingKey)  // ✅ SAME key
                    .build()
                    .parseClaimsJws(token)
                    .getBody();

            String merchantId = claims.getSubject();
            log.debug("✅ Extracted merchantId from token: {}", merchantId);
            return merchantId;
            
        } catch (Exception e) {
            log.warn("❌ JWT claims extraction failed: {}", e.getMessage());
            return null;
        }
    }

    /**
     * Extract role from JWT token
     * Parses token and returns role claim
     * 
     * ✅ Uses SAME signing key initialized at startup
     * 
     * @param token JWT token string
     * @return role claim if valid (e.g., "ROLE_MERCHANT"), null if invalid or expired
     */
    public String getRoleFromToken(String token) {
        try {
            // ✅ Use initialized signing key (same instance created at startup)
            Claims claims = Jwts.parserBuilder()
                    .setSigningKey(signingKey)  // ✅ SAME key
                    .build()
                    .parseClaimsJws(token)
                    .getBody();

            String role = claims.get(ROLE_CLAIM_KEY, String.class);
            log.debug("✅ Extracted role from token: {}", role);
            return role;
            
        } catch (Exception e) {
            log.warn("❌ Failed to extract role from token: {}", e.getMessage());
            return null;
        }
    }

    /**
     * Extract expiration date from JWT token
     * 
     * ✅ Uses SAME signing key initialized at startup
     * 
     * @param token JWT token string
     * @return expiration date if valid, null otherwise
     */
    public Date getExpirationDateFromToken(String token) {
        try {
            // ✅ Use initialized signing key (same instance created at startup)
            Claims claims = Jwts.parserBuilder()
                    .setSigningKey(signingKey)  // ✅ SAME key
                    .build()
                    .parseClaimsJws(token)
                    .getBody();
            
            return claims.getExpiration();
            
        } catch (Exception e) {
            log.warn("❌ Failed to extract expiration date from token: {}", e.getMessage());
            return null;
        }
    }

    /**
     * Check if token has expired
     * 
     * @param token JWT token string
     * @return true if token is expired, false otherwise
     */
    public boolean isTokenExpired(String token) {
        Date expirationDate = getExpirationDateFromToken(token);
        return expirationDate != null && expirationDate.before(new Date());
    }
}
