package com.sabbpe.merchant.auth;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotBlank;
import lombok.*;

/**
 * Login Request DTO
 * Contains merchant credentials for authentication
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@ToString(exclude = {"secretKey"}) // Never log the secret
public class LoginRequest {

    @NotBlank(message = "Merchant ID is required")
    @JsonProperty("merchant_id")
    private String merchantId;

    @NotBlank(message = "Secret key is required")
    @JsonProperty("secret_key")
    private String secretKey;
}
