package com.sabbpe.merchant.auth;

import com.sabbpe.merchant.entity.Merchant;
import com.sabbpe.merchant.entity.MerchantStatus;
import com.sabbpe.merchant.exception.MerchantNotFoundException;
import com.sabbpe.merchant.repository.MerchantRepository;
import com.sabbpe.merchant.security.JwtUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.Date;

/**
 * Authentication Service
 * Handles merchant login and JWT token generation
 * Validates merchant credentials against database records
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AuthService {

    private final MerchantRepository merchantRepository;
    private final JwtUtil jwtUtil;

    /**
     * Authenticate merchant with credentials and generate JWT token
     * @param loginRequest merchant credentials
     * @return LoginResponse with JWT token
     * @throws MerchantNotFoundException if merchant not found or credentials invalid
     */
    public LoginResponse login(LoginRequest loginRequest) {
        log.info("Login attempt for merchantId: {}", loginRequest.getMerchantId());

        // Fetch merchant from database
        Merchant merchant = merchantRepository
                .findByMerchantId(loginRequest.getMerchantId())
                .orElseThrow(() -> {
                    log.warn("Merchant not found during login: {}", loginRequest.getMerchantId());
                    return new MerchantNotFoundException(
                            "Merchant not found for ID: " + loginRequest.getMerchantId()
                    );
                });

        // Verify merchant is active
        if (!MerchantStatus.ACTIVE.equals(merchant.getStatus())) {
            log.warn("Inactive merchant login attempt: {} with status: {}", 
                    loginRequest.getMerchantId(), merchant.getStatus());
            throw new MerchantNotFoundException(
                    "Merchant is not active. Current status: " + merchant.getStatus()
            );
        }

        // Validate secret key (saltKey in database)
        if (!merchant.getSaltKey().equals(loginRequest.getSecretKey())) {
            log.warn("Invalid secret key provided for merchant: {}", loginRequest.getMerchantId());
            throw new MerchantNotFoundException(
                    "Invalid credentials: Secret key does not match"
            );
        }

        // Generate JWT token using singleton JwtUtil instance
        String token = jwtUtil.generateToken(merchant.getMerchantId());
        Date expiresAt = jwtUtil.getExpirationDateFromToken(token);
        long expiresIn = expiresAt.getTime() - System.currentTimeMillis();

        log.info("Successful login for merchantId: {} - JWT token generated (JwtUtil hash: {})", 
                loginRequest.getMerchantId(), System.identityHashCode(jwtUtil));

        return LoginResponse.builder()
                .token(token)
                .tokenType("Bearer")
                .merchantId(merchant.getMerchantId())
                .expiresIn(expiresIn)
                .expiresAt(expiresAt)
                .success(true)
                .message("Authentication successful")
                .build();
    }

    /**
     * Validate merchant identity from JWT token
     * Ensures token merchant and request merchant match
     * @param tokenMerchantId merchant ID from JWT token
     * @param requestMerchantId merchant ID from request payload
     * @throws IllegalArgumentException if merchant IDs don't match
     */
    public void validateMerchantOwnership(String tokenMerchantId, String requestMerchantId) {
        if (!tokenMerchantId.equals(requestMerchantId)) {
            log.warn("Merchant ownership validation failed. Token merchant: {}, Request merchant: {}",
                    tokenMerchantId, requestMerchantId);
            throw new IllegalArgumentException(
                    "Access denied: Merchant ID in token does not match request"
            );
        }
        log.debug("Merchant ownership validation successful for merchantId: {}", tokenMerchantId);
    }
}
