package com.sabbpe.merchant.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

/**
 * DTO for hash generation request
 * Contains merchant details required to generate SHA-256 hash
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class HashGenerateRequest {

    @NotBlank(message = "Merchant ID cannot be blank")
    private String merchantId;

    @NotBlank(message = "Order ID cannot be blank")
    private String orderId;

    @NotNull(message = "Amount cannot be null")
    @DecimalMin(value = "0.01", message = "Amount must be greater than 0")
    private BigDecimal amount;
}
