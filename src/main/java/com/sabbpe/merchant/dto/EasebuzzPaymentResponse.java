package com.sabbpe.merchant.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO for Easebuzz payment initiation response
 * Contains payment status, message, error code, payment URL, and transaction ID
 * Uses Lombok for automatic getter/setter/builder generation
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public class EasebuzzPaymentResponse {

    @JsonProperty("status")
    private String status;

    @JsonProperty("message")
    private String message;

    @JsonProperty("error_code")
    private String errorCode;

    @JsonProperty("payment_url")
    private String paymentUrl;

    @JsonProperty("txn_id")
    private String txnId;

    /**
     * Static factory method for successful payment response
     */
    public static EasebuzzPaymentResponse success(String txnId, String paymentUrl) {
        return EasebuzzPaymentResponse.builder()
            .status("SUCCESS")
            .message("Payment initiated successfully")
            .paymentUrl(paymentUrl)
            .txnId(txnId)
            .build();
    }

    /**
     * Static factory method for failure response
     */
    public static EasebuzzPaymentResponse failure(String message, String errorCode) {
        return EasebuzzPaymentResponse.builder()
            .status("FAILURE")
            .message(message)
            .errorCode(errorCode)
            .build();
    }

    /**
     * Static factory method for error response
     */
    public static EasebuzzPaymentResponse error(String status, String message, String errorCode) {
        return EasebuzzPaymentResponse.builder()
            .status(status)
            .message(message)
            .errorCode(errorCode)
            .build();
    }
}
