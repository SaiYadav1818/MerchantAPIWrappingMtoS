package com.sabbpe.merchant.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

/**
 * DTO for Payment Status Response
 * Contains transaction and payment status information
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public class PaymentStatusResponse {

    private String txnId;
    private BigDecimal amount;
    private String status;
    private String gatewayStatus;
    private String message;
    private String errorCode;

    // Constructor with status, message, and errorCode (for error responses)
    public PaymentStatusResponse(String status, String message, String errorCode) {
        this.status = status;
        this.message = message;
        this.errorCode = errorCode;
    }

    // Constructor with txnId, amount, status, and gatewayStatus
    public PaymentStatusResponse(String txnId, BigDecimal amount, String status, String gatewayStatus) {
        this.txnId = txnId;
        this.amount = amount;
        this.status = status;
        this.gatewayStatus = gatewayStatus;
    }
}
