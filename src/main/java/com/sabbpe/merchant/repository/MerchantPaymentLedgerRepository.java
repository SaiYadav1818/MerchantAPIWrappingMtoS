package com.sabbpe.merchant.repository;

import com.sabbpe.merchant.entity.MerchantPaymentLedger;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * Repository for MerchantPaymentLedger entity
 * 
 * Provides database operations for merchant payment ledger records.
 * Used for:
 * - Recording payment results at merchant level
 * - Tracking settlement status
 * - Reconciliation and reporting
 */
@Repository
public interface MerchantPaymentLedgerRepository extends JpaRepository<MerchantPaymentLedger, Long> {

    /**
     * Find ledger entry by transaction ID
     */
    Optional<MerchantPaymentLedger> findByTxnid(String txnid);

    /**
     * Find ledger entry by merchant ID and transaction ID
     */
    Optional<MerchantPaymentLedger> findByMerchantIdAndTxnid(String merchantId, String txnid);

    /**
     * Find all ledger entries for a merchant ordered by creation date (newest first)
     */
    List<MerchantPaymentLedger> findByMerchantIdOrderByCreatedAtDesc(String merchantId);

    /**
     * Find all ledger entries for a merchant with specific status
     */
    List<MerchantPaymentLedger> findByMerchantIdAndStatusOrderByCreatedAtDesc(
            String merchantId, String status);

    /**
     * Find all ledger entries for a merchant with specific settlement status
     */
    List<MerchantPaymentLedger> findByMerchantIdAndSettlementStatusOrderByCreatedAtDesc(
            String merchantId, String settlementStatus);

    /**
     * Find all successful ledger entries pending settlement for a merchant
     */
    @Query("SELECT mpl FROM MerchantPaymentLedger mpl " +
           "WHERE mpl.merchantId = :merchantId " +
           "AND mpl.status = 'SUCCESS' " +
           "AND mpl.settlementStatus = 'PENDING' " +
           "ORDER BY mpl.createdAt DESC")
    List<MerchantPaymentLedger> findPendingSettlementsByMerchant(@Param("merchantId") String merchantId);

    /**
     * Find all ledger entries created between two dates
     */
    List<MerchantPaymentLedger> findByCreatedAtBetweenOrderByCreatedAtDesc(
            LocalDateTime startDate, LocalDateTime endDate);

    /**
     * Find all successful ledger entries for a merchant between two dates
     */
    List<MerchantPaymentLedger> findByMerchantIdAndStatusAndCreatedAtBetweenOrderByCreatedAtDesc(
            String merchantId, String status, LocalDateTime startDate, LocalDateTime endDate);

    /**
     * Find ledger entry by order ID
     */
    Optional<MerchantPaymentLedger> findByOrderId(String orderId);

    /**
     * Count total transactions for a merchant
     */
    long countByMerchantId(String merchantId);

    /**
     * Count successful transactions for a merchant
     */
    long countByMerchantIdAndStatus(String merchantId, String status);
}
