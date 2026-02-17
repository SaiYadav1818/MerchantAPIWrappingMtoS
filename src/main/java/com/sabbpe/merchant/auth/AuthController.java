package com.sabbpe.merchant.auth;

import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * Authentication Controller
 * Provides merchant login endpoint for JWT token generation
 * Entry point for authentication flow
 */
@Slf4j
@RestController
@RequestMapping("/api/auth")
public class AuthController {

    @Autowired
    private AuthService authService;

    /**
     * Merchant login endpoint
     * Validates credentials and returns JWT token
     * Expected request body:
     * {
     *   "merchant_id": "MERCHANT_ID",
     *   "secret_key": "SECRET_KEY"
     * }
     * @param loginRequest merchant credentials
     * @return JWT token response with authentication details
     */
    @PostMapping("/login")
    public ResponseEntity<LoginResponse> login(@Valid @RequestBody LoginRequest loginRequest) {
        log.info("Login request received for merchantId: {}", loginRequest.getMerchantId());

        try {
            LoginResponse response = authService.login(loginRequest);
            log.info("Login successful for merchantId: {}", loginRequest.getMerchantId());
            return ResponseEntity.ok(response);

        } catch (IllegalArgumentException e) {
            log.error("Login validation error: {}", e.getMessage());
            LoginResponse errorResponse = LoginResponse.builder()
                    .success(false)
                    .message(e.getMessage())
                    .build();
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(errorResponse);

        } catch (Exception e) {
            log.error("Login failed for merchantId: {} - Error: {}", 
                    loginRequest.getMerchantId(), e.getMessage());
            LoginResponse errorResponse = LoginResponse.builder()
                    .success(false)
                    .message("Authentication failed: " + e.getMessage())
                    .build();
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(errorResponse);
        }
    }

    /**
     * Health check endpoint for auth service
     * @return success response
     */
    @GetMapping("/health")
    public ResponseEntity<String> health() {
        return ResponseEntity.ok("Auth service is running");
    }
}
