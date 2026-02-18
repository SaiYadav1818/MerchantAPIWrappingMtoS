package com.sabbpe.merchant.schedule;

import com.sabbpe.merchant.entity.PaymentTransaction;
import com.sabbpe.merchant.repository.PaymentTransactionRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

/**
 * TransactionReconciliationScheduler
 * 
 * Scheduled job for transaction reconciliation.
 * 
 * Runs every 1 hour.
 * 
 * Responsibilities:
 * 1. Find transactions in INITIATED or PROCESSING status
 * 2. Older than 15 minutes
 * 3. Mark as FAILED
 * 4. Log reconciliation events
 */
@Slf4j
@Component
public class TransactionReconciliationScheduler {

    private final PaymentTransactionRepository paymentTransactionRepository;

    // Configuration
    private static final int STALE_TRANSACTION_MINUTES = 15;
    private static final String INITIATED_STATUS = "INITIATED";
    private static final String PROCESSING_STATUS = "PROCESSING";

    @Autowired
    public TransactionReconciliationScheduler(PaymentTransactionRepository paymentTransactionRepository) {
        this.paymentTransactionRepository = paymentTransactionRepository;
    }

    /**
     * Run reconciliation job every 1 hour
     * 
     * Scheduled using cron expression:
     * 0 0 * * * * = every hour at minute 0
     */
    @Scheduled(cron = "0 0 * * * *")
    @Transactional
    public void reconcileStaleTransactions() {
        log.info("========== TRANSACTION RECONCILIATION JOB STARTED ==========");

        try {
            LocalDateTime staleThreshold = LocalDateTime.now().minusMinutes(STALE_TRANSACTION_MINUTES);
            
            log.info("Searching for stale transactions created before: {}", staleThreshold);

            List<String> statuses = List.of(INITIATED_STATUS, PROCESSING_STATUS);

            // Find stale transactions
            List<PaymentTransaction> staleTransactions = 
                    paymentTransactionRepository.findOldTransactionsByStatus(statuses, staleThreshold);

            log.info("Found {} stale transactions", staleTransactions.size());

            if (staleTransactions.isEmpty()) {
                log.info("No stale transactions found");
                log.info("========== TRANSACTION RECONCILIATION JOB COMPLETED ==========\n");
                return;
            }

            // Process each stale transaction
            int successCount = 0;
            for (PaymentTransaction transaction : staleTransactions) {
                try {
                    markTransactionAsFailed(transaction);
                    successCount++;
                } catch (Exception e) {
                    log.error("Error processing stale transaction - txnid: {}, error: {}",
                            transaction.getTxnid(), e.getMessage(), e);
                }
            }

            log.info("Reconciliation completed - {} transactions marked as FAILED", successCount);
            log.info("========== TRANSACTION RECONCILIATION JOB COMPLETED ==========\n");

        } catch (Exception e) {
            log.error("ERROR in transaction reconciliation job", e);
            log.info("========== TRANSACTION RECONCILIATION JOB FAILED ==========\n");
        }
    }

    /**
     * Mark a transaction as FAILED due to no gateway confirmation
     */
    private void markTransactionAsFailed(PaymentTransaction transaction) {
        log.warn("Marking stale transaction as FAILED - txnid: {}, current_status: {}, created_at: {}",
                transaction.getTxnid(), transaction.getStatus(), transaction.getCreatedAt());

        transaction.setStatus("FAILED");
        transaction.setErrorMessage("No gateway confirmation received. Marked as failed by reconciliation job.");
        transaction.setUpdatedAt(LocalDateTime.now());

        paymentTransactionRepository.save(transaction);

        log.info("Transaction marked as FAILED - txnid: {}, merchant: {}",
                transaction.getTxnid(), transaction.getUdf1());
    }

    /**
     * Optional: Manual trigger for reconciliation (for testing or emergency scenarios)
     */
    @Transactional
    public void triggerManualReconciliation() {
        log.info("MANUAL RECONCILIATION TRIGGERED");
        reconcileStaleTransactions();
    }

    /**
     * Get statistics about pending transactions
     */
    public ReconciliationStats getReconciliationStats() {
        log.debug("Calculating reconciliation statistics");

        long initiatedCount = paymentTransactionRepository.countByStatus(INITIATED_STATUS);
        long processingCount = paymentTransactionRepository.countByStatus(PROCESSING_STATUS);
        long successCount = paymentTransactionRepository.countByStatus("SUCCESS");
        long failedCount = paymentTransactionRepository.countByStatus("FAILED");

        return ReconciliationStats.builder()
                .initiatedTransactions(initiatedCount)
                .processingTransactions(processingCount)
                .successTransactions(successCount)
                .failedTransactions(failedCount)
                .totalTransactions(initiatedCount + processingCount + successCount + failedCount)
                .timestamp(LocalDateTime.now())
                .build();
    }

    /**
     * DTO for reconciliation statistics
     */
    @lombok.Data
    @lombok.Builder
    public static class ReconciliationStats {
        private long initiatedTransactions;
        private long processingTransactions;
        private long successTransactions;
        private long failedTransactions;
        private long totalTransactions;
        private LocalDateTime timestamp;
    }
}
