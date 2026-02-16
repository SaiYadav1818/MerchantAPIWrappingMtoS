package com.sabbpe.merchant.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO for payment URL response
 * Contains the payment URL from Easebuzz gateway and transaction details
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public class PaymentUrlResponse {

    private String status;
    private String paymentUrl;
    private String txnId;
    private String orderId;
    private String message;
    private String errorCode;
    private Long timestamp;
}
