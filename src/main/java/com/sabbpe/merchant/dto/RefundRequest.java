package com.sabbpe.merchant.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

/**
 * DTO for Refund Request
 * Contains transaction ID and refund amount
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RefundRequest {

    @NotBlank(message = "Transaction ID cannot be blank")
    private String txnId;

    @NotNull(message = "Refund amount cannot be null")
    @Positive(message = "Refund amount must be positive")
    private BigDecimal refundAmount;
}
