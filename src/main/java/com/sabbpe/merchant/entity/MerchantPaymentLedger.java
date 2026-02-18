package com.sabbpe.merchant.entity;

import jakarta.persistence.*;
import lombok.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * MerchantPaymentLedger Entity
 * 
 * Tracks payment results at merchant level.
 * 
 * Created when payment succeeds for a merchant.
 * Used for:
 * - Merchant transaction history
 * - Settlement calculation
 * - Reconciliation
 * - Revenue reports
 * 
 * One payment transaction can be routed to multiple merchants via ledger entries.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Entity
@Table(name = "merchant_payment_ledger", indexes = {
    @Index(name = "idx_merchant_id", columnList = "merchant_id"),
    @Index(name = "idx_txnid", columnList = "txnid"),
    @Index(name = "idx_order_id", columnList = "order_id"),
    @Index(name = "idx_status", columnList = "status"),
    @Index(name = "idx_created_at", columnList = "created_at")
})
public class MerchantPaymentLedger {

    // ========================================
    // PRIMARY KEY
    // ========================================
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // ========================================
    // MERCHANT INFORMATION
    // ========================================
    @Column(name = "merchant_id", nullable = false, length = 100)
    private String merchantId;

    // ========================================
    // TRANSACTION INFORMATION
    // ========================================
    @Column(name = "txnid", nullable = false, length = 50)
    private String txnid;

    @Column(name = "order_id", nullable = false, length = 100)
    private String orderId;

    @Column(name = "amount", nullable = false, precision = 10, scale = 2)
    private BigDecimal amount;

    @Column(name = "status", nullable = false, length = 50)
    private String status;

    // ========================================
    // PAYMENT DETAILS
    // ========================================
    @Column(name = "payment_mode", length = 50)
    private String paymentMode;

    @Column(name = "bank_ref_num", length = 100)
    private String bankRefNum;

    @Column(name = "gateway_id", length = 100)
    private String gatewayId;

    // ========================================
    // LEDGER STATUS
    // ========================================
    @Column(name = "settlement_status", nullable = false, length = 50)
    @Builder.Default
    private String settlementStatus = "PENDING"; // PENDING, SETTLED, FAILED, REVERSED

    @Column(name = "settlement_date")
    private LocalDateTime settlementDate;

    @Column(name = "settled_amount", precision = 10, scale = 2)
    private BigDecimal settledAmount;

    // ========================================
    // AUDIT FIELDS
    // ========================================
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @Column(name = "notes", length = 500)
    private String notes;

    // ========================================
    // LIFECYCLE HOOKS
    // ========================================
    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
        if (this.settlementStatus == null) {
            this.settlementStatus = "PENDING";
        }
    }

    @PreUpdate
    protected void onUpdate() {
        this.updatedAt = LocalDateTime.now();
    }

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
     * Check if ledger is settled
     */
    public boolean isSettled() {
        return "SETTLED".equalsIgnoreCase(this.settlementStatus);
    }

    /**
     * Check if ledger is pending settlement
     */
    public boolean isPendingSettlement() {
        return "PENDING".equalsIgnoreCase(this.settlementStatus);
    }
}
