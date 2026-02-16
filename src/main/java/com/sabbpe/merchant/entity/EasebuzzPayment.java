package com.sabbpe.merchant.entity;

import jakarta.persistence.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "easebuzz_payments")
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

    public EasebuzzPayment() {
    }

    public EasebuzzPayment(String txnId, String merchantId, BigDecimal amount,
                           String gatewayStatus, String normalizedStatus,
                           Boolean hashValidated, String rawResponse) {
        this.txnId = txnId;
        this.merchantId = merchantId;
        this.amount = amount;
        this.gatewayStatus = gatewayStatus;
        this.normalizedStatus = normalizedStatus;
        this.hashValidated = hashValidated;
        this.rawResponse = rawResponse;
    }

    // Getters and Setters
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getTxnId() {
        return txnId;
    }

    public void setTxnId(String txnId) {
        this.txnId = txnId;
    }

    public String getMerchantId() {
        return merchantId;
    }

    public void setMerchantId(String merchantId) {
        this.merchantId = merchantId;
    }

    public BigDecimal getAmount() {
        return amount;
    }

    public void setAmount(BigDecimal amount) {
        this.amount = amount;
    }

    public String getGatewayStatus() {
        return gatewayStatus;
    }

    public void setGatewayStatus(String gatewayStatus) {
        this.gatewayStatus = gatewayStatus;
    }

    public String getNormalizedStatus() {
        return normalizedStatus;
    }

    public void setNormalizedStatus(String normalizedStatus) {
        this.normalizedStatus = normalizedStatus;
    }

    public String getHash() {
        return hash;
    }

    public void setHash(String hash) {
        this.hash = hash;
    }

    public Boolean getHashValidated() {
        return hashValidated;
    }

    public void setHashValidated(Boolean hashValidated) {
        this.hashValidated = hashValidated;
    }

    public String getRawResponse() {
        return rawResponse;
    }

    public void setRawResponse(String rawResponse) {
        this.rawResponse = rawResponse;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }
}
