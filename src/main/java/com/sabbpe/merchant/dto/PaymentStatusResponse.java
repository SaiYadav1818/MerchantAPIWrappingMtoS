package com.sabbpe.merchant.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Payment Status Response DTO
 * 
 * Used for REST API responses when querying payment transaction status.
 * Includes all relevant transaction details, UDF fields, bank details, and raw response.
 * 
 * Supports both new (with full details) and legacy (with status/message/errorCode) responses.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public class PaymentStatusResponse {

    // ========================================
    // LEGACY FIELDS (for backward compatibility)
    // ========================================
    private String txnId;
    private BigDecimal amount;
    private String status;
    private String gatewayStatus;
    private String message;
    private String errorCode;

    // ========================================
    // COMPREHENSIVE FIELDS
    // ========================================
    @JsonProperty("txnid")
    private String txnid;

    @JsonProperty("bank_ref_num")
    private String bankRefNum;

    @JsonProperty("easepayid")
    private String easepayid;

    @JsonProperty("bankcode")
    private String bankcode;

    @JsonProperty("mode")
    private String mode;

    @JsonProperty("email")
    private String email;

    @JsonProperty("phone")
    private String phone;

    @JsonProperty("firstname")
    private String firstname;

    @JsonProperty("bank_name")
    private String bankName;

    @JsonProperty("issuing_bank")
    private String issuingBank;

    @JsonProperty("card_type")
    private String cardType;

    @JsonProperty("hash_verified")
    private Boolean hashVerified;

    @JsonProperty("error_message")
    private String errorMessage;

    // ========================================
    // UDF FIELDS
    // ========================================
    @JsonProperty("udf1")
    private String udf1;

    @JsonProperty("udf2")
    private String udf2;

    @JsonProperty("udf3")
    private String udf3;

    @JsonProperty("udf4")
    private String udf4;

    @JsonProperty("udf5")
    private String udf5;

    @JsonProperty("udf6")
    private String udf6;

    @JsonProperty("udf7")
    private String udf7;

    @JsonProperty("udf8")
    private String udf8;

    @JsonProperty("udf9")
    private String udf9;

    @JsonProperty("udf10")
    private String udf10;

    // ========================================
    // RAW RESPONSE AND TIMESTAMPS
    // ========================================
    @JsonProperty("raw_response")
    private String rawResponse;

    @JsonProperty("created_at")
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime createdAt;

    @JsonProperty("updated_at")
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime updatedAt;

    // ========================================
    // LEGACY CONSTRUCTORS
    // ========================================

    /**
     * Constructor with status, message, and errorCode (for error responses)
     */
    public PaymentStatusResponse(String status, String message, String errorCode) {
        this.status = status;
        this.message = message;
        this.errorCode = errorCode;
    }

    /**
     * Constructor with txnId, amount, status, and gatewayStatus
     */
    public PaymentStatusResponse(String txnId, BigDecimal amount, String status, String gatewayStatus) {
        this.txnId = txnId;
        this.amount = amount;
        this.status = status;
        this.gatewayStatus = gatewayStatus;
    }
}
