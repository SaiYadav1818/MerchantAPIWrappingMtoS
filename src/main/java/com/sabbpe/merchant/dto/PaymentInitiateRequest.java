package com.sabbpe.merchant.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

/**
 * DTO for payment initiation request from merchant
 * Contains merchant details, order info, amount and hash for verification
 * Supports snake_case JSON field names via @JsonProperty
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PaymentInitiateRequest {

    @NotBlank(message = "Merchant ID cannot be blank")
    @JsonProperty("merchant_id")
    private String merchantId;

    @NotBlank(message = "Order ID cannot be blank")
    @JsonProperty("order_id")
    private String orderId;

    @NotNull(message = "Amount cannot be null")
    @DecimalMin(value = "0.01", message = "Amount must be greater than 0")
    private BigDecimal amount;

    @NotBlank(message = "Hash cannot be blank")
    private String hash;
}
