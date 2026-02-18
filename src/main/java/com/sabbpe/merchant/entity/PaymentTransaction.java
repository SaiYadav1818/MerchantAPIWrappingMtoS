package com.sabbpe.merchant.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * PaymentTransaction Entity
 * 
 * Stores complete payment gateway response data for audit, reconciliation,
 * and merchant routing purposes.
 * 
 * Each payment redirect creates or updates a record with:
 * - All gateway response fields (status, amount, bank details, etc.)
 * - UDF fields for merchant-specific data (UDF1-UDF10)
 * - Raw JSON response for complete audit trail
 * - Hash verification status
 * 
 * Uniqueness: Transaction ID (txnid) from payment gateway
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Entity
@Table(name = "payment_transactions", indexes = {
    @Index(name = "idx_txnid", columnList = "txnid", unique = true),
    @Index(name = "idx_status", columnList = "status"),
    @Index(name = "idx_created_at", columnList = "created_at")
})
public class PaymentTransaction {

    // ========================================
    // PRIMARY KEY
    // ========================================
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // ========================================
    // GATEWAY TRANSACTION FIELDS
    // ========================================
    @Column(name = "txnid", nullable = false, unique = true, length = 50)
    private String txnid;

    @Column(name = "status", nullable = false, length = 50)
    private String status;

    @Column(name = "amount", nullable = false, precision = 10, scale = 2)
    private BigDecimal amount;

    @Column(name = "easepayid", length = 100)
    private String easepayid;

    @Column(name = "bank_ref_num", length = 100)
    private String bankRefNum;

    @Column(name = "bankcode", length = 50)
    private String bankcode;

    @Column(name = "mode", length = 50)
    private String mode;

    // ========================================
    // CUSTOMER INFORMATION
    // ========================================
    @Column(name = "email", length = 100)
    private String email;

    @Column(name = "phone", length = 20)
    private String phone;

    @Column(name = "firstname", length = 100)
    private String firstname;

    // ========================================
    // PAYMENT DETAILS
    // ========================================
    @Column(name = "hash", length = 500)
    private String hash;

    @Column(name = "hash_verified", nullable = false)
    @Builder.Default
    private Boolean hashVerified = false;

    @Column(name = "payment_source", length = 100)
    private String paymentSource;

    @Column(name = "productinfo", length = 500)
    private String productinfo;

    @Column(name = "bank_name", length = 100)
    private String bankName;

    @Column(name = "issuing_bank", length = 100)
    private String issuingBank;

    @Column(name = "card_type", length = 50)
    private String cardType;

    @Column(name = "auth_code", length = 100)
    private String authCode;

    @Column(name = "error_message", length = 500)
    private String errorMessage;

    // ========================================
    // USER-DEFINED FIELDS (UDF1-UDF10)
    // ========================================
    @Column(name = "udf1", length = 300)
    private String udf1;

    @Column(name = "udf2", length = 300)
    private String udf2;

    @Column(name = "udf3", length = 300)
    private String udf3;

    @Column(name = "udf4", length = 300)
    private String udf4;

    @Column(name = "udf5", length = 300)
    private String udf5;

    @Column(name = "udf6", length = 300)
    private String udf6;

    @Column(name = "udf7", length = 300)
    private String udf7;

    @Column(name = "udf8", length = 300)
    private String udf8;

    @Column(name = "udf9", length = 300)
    private String udf9;

    @Column(name = "udf10", length = 300)
    private String udf10;

    // ========================================
    // RAW RESPONSE STORAGE
    // ========================================
    @Column(name = "raw_response", columnDefinition = "CLOB")
    private String rawResponse;

    // ========================================
    // AUDIT FIELDS
    // ========================================
    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    // ========================================
    // UTILITY METHODS
    // ========================================

    /**
     * Check if payment is successful
     */
    public boolean isSuccessful() {
        return "SUCCESS".equalsIgnoreCase(this.status);
    }

    /**
     * Check if payment failed
     */
    public boolean isFailed() {
        return "FAILED".equalsIgnoreCase(this.status) ||
               "FAILURE".equalsIgnoreCase(this.status);
    }

    /**
     * Check if hash verification passed
     */
    public boolean isHashValid() {
        return this.hashVerified != null && this.hashVerified;
    }

    /**
     * Get UDF value by index (1-10)
     */
    public String getUDFValue(int index) {
        return switch (index) {
            case 1 -> udf1;
            case 2 -> udf2;
            case 3 -> udf3;
            case 4 -> udf4;
            case 5 -> udf5;
            case 6 -> udf6;
            case 7 -> udf7;
            case 8 -> udf8;
            case 9 -> udf9;
            case 10 -> udf10;
            default -> null;
        };
    }

    /**
     * Set UDF value by index (1-10)
     */
    public void setUDFValue(int index, String value) {
        switch (index) {
            case 1 -> this.udf1 = value;
            case 2 -> this.udf2 = value;
            case 3 -> this.udf3 = value;
            case 4 -> this.udf4 = value;
            case 5 -> this.udf5 = value;
            case 6 -> this.udf6 = value;
            case 7 -> this.udf7 = value;
            case 8 -> this.udf8 = value;
            case 9 -> this.udf9 = value;
            case 10 -> this.udf10 = value;
        }
    }
}
