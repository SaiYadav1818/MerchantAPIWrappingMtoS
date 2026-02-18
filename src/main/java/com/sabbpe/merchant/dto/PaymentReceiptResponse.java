package com.sabbpe.merchant.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * PaymentReceiptResponse DTO
 * 
 * Response for GET /api/payment/receipt/{txnid}
 * 
 * Contains payment receipt data for display/download:
 * - Merchant details
 * - Transaction info
 * - Amount and payment mode
 * - Bank reference
 * - Timestamp in readable format
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public class PaymentReceiptResponse {

    @JsonProperty("receipt_number")
    private String receiptNumber;

    // ========================================
    // MERCHANT INFORMATION
    // ========================================
    @JsonProperty("merchant_name")
    private String merchantName;

    @JsonProperty("merchant_id")
    private String merchantId;

    // ========================================
    // TRANSACTION INFORMATION
    // ========================================
    @JsonProperty("txnid")
    private String txnid;

    @JsonProperty("order_id")
    private String orderId;

    @JsonProperty("status")
    private String status;

    // ========================================
    // PAYMENT DETAILS
    // ========================================
    @JsonProperty("amount")
    private BigDecimal amount;

    @JsonProperty("currency")
    @Builder.Default
    private String currency = "INR";

    @JsonProperty("payment_mode")
    private String paymentMode;

    @JsonProperty("bank_name")
    private String bankName;

    @JsonProperty("bank_reference")
    private String bankReference;

    @JsonProperty("gateway_reference")
    private String gatewayReference;

    // ========================================
    // CUSTOMER INFORMATION
    // ========================================
    @JsonProperty("customer_name")
    private String customerName;

    @JsonProperty("customer_email")
    private String customerEmail;

    @JsonProperty("customer_phone")
    private String customerPhone;

    // ========================================
    // TIMESTAMPS
    // ========================================
    @JsonProperty("transaction_date")
    private String transactionDate;

    @JsonProperty("transaction_time")
    private String transactionTime;

    @JsonProperty("transaction_datetime")
    private String transactionDateTime;

    // ========================================
    // UTILITY METHODS
    // ========================================

    public static PaymentReceiptResponse fromPaymentTransaction(
            com.sabbpe.merchant.entity.PaymentTransaction transaction,
            String merchantName) {

        if (transaction == null) {
            return null;
        }

        DateTimeFormatter dateFormatter = DateTimeFormatter.ofPattern("dd-MMM-yyyy");
        DateTimeFormatter timeFormatter = DateTimeFormatter.ofPattern("HH:mm:ss");
        DateTimeFormatter dateTimeFormatter = DateTimeFormatter.ofPattern("dd-MMM-yyyy HH:mm:ss");

        LocalDateTime txnDateTime = transaction.getCreatedAt();
        if (txnDateTime == null) {
            txnDateTime = LocalDateTime.now();
        }

        return PaymentReceiptResponse.builder()
                .receiptNumber("RCP-" + transaction.getTxnid() + "-" + System.currentTimeMillis())
                .merchantName(merchantName)
                .merchantId(transaction.getUdf1())
                .txnid(transaction.getTxnid())
                .orderId(transaction.getUdf2())
                .status(transaction.getStatus())
                .amount(transaction.getAmount())
                .currency("INR")
                .paymentMode(transaction.getMode())
                .bankName(transaction.getBankName())
                .bankReference(transaction.getBankRefNum())
                .gatewayReference(transaction.getEasepayid())
                .customerName(transaction.getFirstname())
                .customerEmail(transaction.getEmail())
                .customerPhone(transaction.getPhone())
                .transactionDate(txnDateTime.format(dateFormatter))
                .transactionTime(txnDateTime.format(timeFormatter))
                .transactionDateTime(txnDateTime.format(dateTimeFormatter))
                .build();
    }

    /**
     * Get formatted receipt display string
     */
    public String getFormattedReceipt() {
        StringBuilder sb = new StringBuilder();
        sb.append("=====================================\n");
        sb.append("        PAYMENT RECEIPT\n");
        sb.append("=====================================\n\n");
        sb.append("Receipt Number: ").append(receiptNumber).append("\n");
        sb.append("Transaction ID: ").append(txnid).append("\n");
        sb.append("Order ID: ").append(orderId).append("\n\n");
        sb.append("Merchant: ").append(merchantName).append("\n\n");
        sb.append("Customer Name: ").append(customerName).append("\n");
        sb.append("Customer Email: ").append(customerEmail).append("\n");
        sb.append("Customer Phone: ").append(customerPhone).append("\n\n");
        sb.append("Amount: ₹").append(amount).append(" ").append(currency).append("\n");
        sb.append("Payment Mode: ").append(paymentMode).append("\n");
        sb.append("Bank: ").append(bankName).append("\n");
        sb.append("Bank Reference: ").append(bankReference).append("\n");
        sb.append("Gateway Reference: ").append(gatewayReference).append("\n\n");
        sb.append("Status: ").append(status).append("\n");
        sb.append("Date & Time: ").append(transactionDateTime).append("\n\n");
        sb.append("=====================================\n");
        return sb.toString();
    }
}
