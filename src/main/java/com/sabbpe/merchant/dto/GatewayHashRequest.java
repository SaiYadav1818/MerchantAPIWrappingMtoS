package com.sabbpe.merchant.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

/**
 * Request DTO for Easebuzz payload hash generation
 * 
 * Contains the payment details required to generate the Easebuzz payment gateway hash
 * Supports snake_case JSON field names via @JsonProperty
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class GatewayHashRequest {

    @NotBlank(message = "Transaction ID is required")
    @Size(min = 1, max = 100, message = "Transaction ID must be between 1 and 100 characters")
    @JsonProperty("txn_id")
    private String txnId;

    @NotNull(message = "Amount is required")
    @DecimalMin(value = "0.01", message = "Amount must be greater than 0")
    @DecimalMax(value = "999999.99", message = "Amount must not exceed 999999.99")
    private BigDecimal amount;

    @NotBlank(message = "Product info is required")
    @Size(min = 1, max = 255, message = "Product info must be between 1 and 255 characters")
    private String productInfo;

    @NotBlank(message = "First name is required")
    @Size(min = 1, max = 100, message = "First name must be between 1 and 100 characters")
    private String firstName;

    @NotBlank(message = "Email is required")
    @Email(message = "Email must be valid")
    private String email;
}
