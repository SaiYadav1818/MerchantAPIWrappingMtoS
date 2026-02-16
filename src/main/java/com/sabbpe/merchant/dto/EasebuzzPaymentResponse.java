package com.sabbpe.merchant.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO for Easebuzz Payment Response
 * Contains payment status, transaction ID, payment URL and error details
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public class EasebuzzPaymentResponse {

    private String status;
    private String txnId;
    private String paymentUrl;
    private String message;
    private String errorCode;

    // Factory method for SUCCESS case
    public static EasebuzzPaymentResponse success(String status, String txnId, String paymentUrl) {
        return EasebuzzPaymentResponse.builder()
                .status(status)
                .txnId(txnId)
                .paymentUrl(paymentUrl)
                .build();
    }

    // Factory method for ERROR case
    public static EasebuzzPaymentResponse error(String status, String message, String errorCode) {
        return EasebuzzPaymentResponse.builder()
                .status(status)
                .message(message)
                .errorCode(errorCode)
                .build();
    }
}
