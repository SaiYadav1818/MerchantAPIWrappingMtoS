package com.sabbpe.merchant.entity;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "transactions")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@EqualsAndHashCode(exclude = {"createdAt", "updatedAt"})
@ToString
public class Transaction {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "txn_id", unique = true, nullable = false)
    private String txnId;

    @Column(name = "order_id", nullable = false)
    private String orderId;

    @Column(name = "merchant_id", nullable = false)
    private String merchantId;

    @Column(name = "amount", nullable = false, precision = 15, scale = 2)
    private BigDecimal amount;

    @Column(name = "status", nullable = false)
    @Enumerated(EnumType.STRING)
    private TransactionStatus status;

    @Column(name = "internal_token", unique = true)
    private String internalToken;

    @Column(name = "product_info")
    private String productinfo;

    @Column(name = "customer_email")
    private String customerEmail;

    @Column(name = "customer_name")
    private String customerName;

    @Column(name = "customer_phone")
    private String customerPhone;

    @Column(name = "hash_generated")
    @Builder.Default
    private Boolean hashGenerated = false;

    @Column(name = "hash_validated")
    @Builder.Default
    private Boolean hashValidated = false;

    // ========================================
    // UDF FIELDS (UDF1 - UDF10)
    // ========================================
    
    @Column(name = "udf1")
    private String udf1;

    @Column(name = "udf2")
    private String udf2;

    @Column(name = "udf3")
    private String udf3;

    @Column(name = "udf4")
    private String udf4;

    @Column(name = "udf5")
    private String udf5;

    @Column(name = "udf6")
    private String udf6;

    @Column(name = "udf7")
    private String udf7;

    @Column(name = "udf8")
    private String udf8;

    @Column(name = "udf9")
    private String udf9;

    @Column(name = "udf10")
    private String udf10;

    // ========================================
    // AUDIT FIELDS
    // ========================================

    @Column(name = "initiated_at")
    private LocalDateTime initiatedAt;

    @Column(name = "completed_at")
    private LocalDateTime completedAt;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
        if (this.initiatedAt == null) {
            this.initiatedAt = LocalDateTime.now();
        }
    }

    @PreUpdate
    protected void onUpdate() {
        this.updatedAt = LocalDateTime.now();
    }
}
