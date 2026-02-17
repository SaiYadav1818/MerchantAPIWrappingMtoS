package com.sabbpe.merchant.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
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
 * Supports snake_case JSON field names via @JsonProperty
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RefundRequest {

    @NotBlank(message = "Transaction ID cannot be blank")
    @JsonProperty("txn_id")
    private String txnId;

    @NotNull(message = "Refund amount cannot be null")
    @Positive(message = "Refund amount must be positive")
    @JsonProperty("refund_amount")
    private BigDecimal refundAmount;
}
