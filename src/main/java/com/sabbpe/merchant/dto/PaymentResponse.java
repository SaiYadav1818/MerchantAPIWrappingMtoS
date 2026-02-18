package com.sabbpe.merchant.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * DTO for payment response
 * Contains transaction details returned from payment processing
 * 
 * Used in:
 * - POST /api/payment/process responses
 * - Payment status queries
 * - Transaction history APIs
 * - Payment redirect/success/failure pages
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public class PaymentResponse {

    @JsonProperty("transaction_id")
    private String transactionId;

    @JsonProperty("status")
    private String status;

    @JsonProperty("message")
    private String message;

    @JsonProperty("amount")
    private BigDecimal amount;

    @JsonProperty("timestamp")
    private LocalDateTime timestamp;

    @JsonProperty("merchant_id")
    private String merchantId;

    @JsonProperty("order_id")
    private String orderId;

    @JsonProperty("internal_token")
    private String internalToken;

    @JsonProperty("gateway_reference")
    private String gatewayReference;

    @JsonProperty("payment_method")
    private String paymentMethod;

    @JsonProperty("error_code")
    private String errorCode;

    @JsonProperty("error_description")
    private String errorDescription;

    /**
     * Static factory method for successful payment response
     * 
     * @param transactionId Transaction ID
     * @param amount Payment amount
     * @param message Success message
     * @return PaymentResponse with SUCCESS status
     */
    public static PaymentResponse success(String transactionId, BigDecimal amount, String message) {
        return PaymentResponse.builder()
                .transactionId(transactionId)
                .status("SUCCESS")
                .message(message)
                .amount(amount)
                .timestamp(LocalDateTime.now())
                .build();
    }

    /**
     * Static factory method for failed payment response
     * 
     * @param transactionId Transaction ID
     * @param message Error message
     * @param errorCode Error code from gateway
     * @return PaymentResponse with FAILURE status
     */
    public static PaymentResponse failure(String transactionId, String message, String errorCode) {
        return PaymentResponse.builder()
                .transactionId(transactionId)
                .status("FAILURE")
                .message(message)
                .errorCode(errorCode)
                .timestamp(LocalDateTime.now())
                .build();
    }

    /**
     * Static factory method for pending payment response
     * 
     * @param transactionId Transaction ID
     * @param message Pending message
     * @return PaymentResponse with PENDING status
     */
    public static PaymentResponse pending(String transactionId, String message) {
        return PaymentResponse.builder()
                .transactionId(transactionId)
                .status("PENDING")
                .message(message)
                .timestamp(LocalDateTime.now())
                .build();
    }

    /**
     * Check if payment was successful
     * 
     * @return true if status is SUCCESS
     */
    public boolean isSuccessful() {
        return "SUCCESS".equalsIgnoreCase(this.status);
    }

    /**
     * Check if payment failed
     * 
     * @return true if status is FAILURE
     */
    public boolean isFailed() {
        return "FAILURE".equalsIgnoreCase(this.status);
    }

    /**
     * Check if payment is pending
     * 
     * @return true if status is PENDING
     */
    public boolean isPending() {
        return "PENDING".equalsIgnoreCase(this.status);
    }
}
