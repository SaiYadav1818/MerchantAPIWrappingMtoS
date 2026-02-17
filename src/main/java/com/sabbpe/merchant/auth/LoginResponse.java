package com.sabbpe.merchant.auth;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Date;

/**
 * Login Response DTO
 * Contains JWT token and related information
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class LoginResponse {

    @JsonProperty("token")
    private String token;

    @JsonProperty("token_type")
    private String tokenType = "Bearer";

    @JsonProperty("merchant_id")
    private String merchantId;

    @JsonProperty("expires_in")
    private Long expiresIn; // milliseconds

    @JsonProperty("expires_at")
    private Date expiresAt;

    @JsonProperty("success")
    private Boolean success = true;

    @JsonProperty("message")
    private String message;
}
