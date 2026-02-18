package com.sabbpe.merchant.repository;

import com.sabbpe.merchant.entity.PaymentTransaction;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * Repository for PaymentTransaction entity
 * 
 * Provides database access methods for:
 * - Finding transactions by transaction ID
 * - Querying by status, amount, date ranges
 * - Audit trails and reconciliation
 */
@Repository
public interface PaymentTransactionRepository extends JpaRepository<PaymentTransaction, Long> {

    /**
     * Find payment transaction by unique transaction ID
     * @param txnid Transaction ID from payment gateway
     * @return Optional containing transaction if found
     */
    Optional<PaymentTransaction> findByTxnid(String txnid);

    /**
     * Find all transactions by status
     * @param status Payment status (SUCCESS, FAILED, HASH_MISMATCH)
     * @return List of transactions with given status
     */
    List<PaymentTransaction> findByStatus(String status);

    /**
     * Find transactions by UDF1 (e.g., merchant ID)
     * @param udf1 UDF1 value (typically merchant ID)
     * @return List of transactions
     */
    List<PaymentTransaction> findByUdf1(String udf1);

    /**
     * Find transactions by UDF2 (e.g., order ID)
     * @param udf2 UDF2 value (typically order ID)
     * @return List of transactions
     */
    List<PaymentTransaction> findByUdf2(String udf2);

    /**
     * Find transactions by email
     * @param email Customer email
     * @return List of transactions
     */
    List<PaymentTransaction> findByEmail(String email);

    /**
     * Find transactions by phone
     * @param phone Customer phone
     * @return List of transactions
     */
    List<PaymentTransaction> findByPhone(String phone);

    /**
     * Find transactions by hash verification status
     * @param hashVerified Hash verification status (true/false)
     * @return List of transactions
     */
    List<PaymentTransaction> findByHashVerified(Boolean hashVerified);

    /**
     * Count transactions by status
     * @param status Payment status
     * @return Count of transactions
     */
    long countByStatus(String status);

    /**
     * Check if transaction exists by txnid
     * @param txnid Transaction ID
     * @return true if exists, false otherwise
     */
    boolean existsByTxnid(String txnid);

    /**
     * Custom query: Find successful transactions for a merchant
     * @param merchantId Merchant ID (UDF1)
     * @param status Payment status
     * @return List of matching transactions
     */
    @Query("SELECT p FROM PaymentTransaction p WHERE p.udf1 = :merchantId AND p.status = :status")
    List<PaymentTransaction> findSuccessfulTransactionsByMerchant(
        @Param("merchantId") String merchantId,
        @Param("status") String status);

    /**
     * Custom query: Find transactions with hash verification failures
     * @return List of transactions with hash_verified = false
     */
    @Query("SELECT p FROM PaymentTransaction p WHERE p.hashVerified = false ORDER BY p.createdAt DESC")
    List<PaymentTransaction> findHashVerificationFailures();

    /**
     * Find pending transactions older than specified time (for reconciliation)
     * Used to mark stale INITIATED/PROCESSING transactions as FAILED
     * @param status The status to search for (INITIATED, PROCESSING)
     * @param beforeTime LocalDateTime threshold
     * @return List of old pending transactions
     */
    @Query("SELECT p FROM PaymentTransaction p WHERE p.status = :status AND p.createdAt < :beforeTime ORDER BY p.createdAt ASC")
    List<PaymentTransaction> findOldPendingTransactions(
        @Param("status") String status,
        @Param("beforeTime") java.time.LocalDateTime beforeTime);

    /**
     * Find all transactions with status in list and created before time
     */
    @Query("SELECT p FROM PaymentTransaction p WHERE p.status IN :statuses AND p.createdAt < :beforeTime ORDER BY p.createdAt ASC")
    List<PaymentTransaction> findOldTransactionsByStatus(
        @Param("statuses") List<String> statuses,
        @Param("beforeTime") java.time.LocalDateTime beforeTime);
}
