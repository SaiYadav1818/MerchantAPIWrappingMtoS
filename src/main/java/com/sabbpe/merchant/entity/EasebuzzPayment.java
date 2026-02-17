package com.sabbpe.merchant.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * EasebuzzPayment Entity
 * Represents payment records from Easebuzz gateway integration
 * Uses Lombok for automatic generation of getters, setters, constructors, toString, equals, hashCode
 */
@Entity
@Table(name = "easebuzz_payments")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@EqualsAndHashCode(exclude = {"createdAt", "updatedAt"})
@ToString(exclude = {"rawResponse"})
public class EasebuzzPayment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "txn_id", unique = true, nullable = false)
    private String txnId;

    @Column(name = "merchant_id", nullable = false)
    private String merchantId;

    @Column(name = "amount", nullable = false, precision = 15, scale = 2)
    private BigDecimal amount;

    @Column(name = "gateway_status", nullable = false)
    private String gatewayStatus;

    @Column(name = "normalized_status", nullable = false)
    private String normalizedStatus;

    @Column(name = "hash")
    private String hash;

    @Column(name = "hash_validated", nullable = false)
    private Boolean hashValidated = false;

    @Column(name = "raw_response", columnDefinition = "TEXT")
    private String rawResponse;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
}
