package com.sabbpe.merchant.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.sabbpe.merchant.entity.Merchant;
import com.sabbpe.merchant.entity.MerchantPaymentLedger;
import com.sabbpe.merchant.entity.PaymentTransaction;
import com.sabbpe.merchant.exception.MerchantNotActiveException;
import com.sabbpe.merchant.repository.MerchantPaymentLedgerRepository;
import com.sabbpe.merchant.repository.MerchantRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

/**
 * MerchantRoutingService
 * 
 * Routes payment results to merchant ledger.
 * 
 * Responsibilities:
 * 1. Extract merchant ID from UDF1
 * 2. Verify merchant is active
 * 3. Create/update merchant payment ledger entry
 * 4. Handle idempotent callbacks (no duplicates)
 * 5. Log routing events
 */
@Slf4j
@Service
@Transactional
public class MerchantRoutingService {

    private final MerchantRepository merchantRepository;
    private final MerchantPaymentLedgerRepository ledgerRepository;
    private final ObjectMapper objectMapper;

    @Autowired
    public MerchantRoutingService(
            MerchantRepository merchantRepository,
            MerchantPaymentLedgerRepository ledgerRepository,
            ObjectMapper objectMapper) {
        this.merchantRepository = merchantRepository;
        this.ledgerRepository = ledgerRepository;
        this.objectMapper = objectMapper;
    }

    /**
     * Route payment result to merchant ledger
     * 
     * Creates or updates a ledger entry for successful payments.
     * Implements idempotent callback handling.
     * 
     * @param transaction Payment transaction entity
     * @return true if routing successful, false otherwise
     */
    @Transactional
    public boolean routePaymentToMerchant(PaymentTransaction transaction) {
        log.info("========== MERCHANT ROUTING START ==========");
        log.info("Transaction ID: {}, Status: {}, Amount: {}",
                transaction.getTxnid(), transaction.getStatus(), transaction.getAmount());

        try {
            // Extract merchant ID from UDF1
            String merchantId = transaction.getUdf1();
            if (merchantId == null || merchantId.trim().isEmpty()) {
                log.warn("Merchant ID (UDF1) is missing! Cannot route payment - txnid: {}",
                        transaction.getTxnid());
                return false;
            }

            log.info("Routing to merchant: {}", merchantId);

            // Verify merchant exists and is active
            Merchant merchant = merchantRepository.findByMerchantId(merchantId)
                    .orElseThrow(() -> new RuntimeException("Merchant not found: " + merchantId));

            if (!isActiveMerchant(merchant)) {
                log.error("Merchant is not active - merchantId: {}", merchantId);
                throw new MerchantNotActiveException(merchantId);
            }

            log.info("Merchant verified and active - merchantName: {}", merchant.getMerchantName());

            // Check if ledger entry already exists (idempotent protection)
            Optional<MerchantPaymentLedger> existingLedger = 
                    ledgerRepository.findByMerchantIdAndTxnid(merchantId, transaction.getTxnid());

            if (existingLedger.isPresent()) {
                // Duplicate callback detection
                MerchantPaymentLedger ledger = existingLedger.get();
                log.warn("DUPLICATE CALLBACK DETECTED - Ledger already exists");
                log.warn("  Existing Status: {}, New Status: {}",
                        ledger.getStatus(), transaction.getStatus());

                // Only update if status changed
                if (!ledger.getStatus().equals(transaction.getStatus())) {
                    log.info("Status change detected - Updating existing ledger entry");
                    ledger.setStatus(transaction.getStatus());
                    ledger.setUpdatedAt(java.time.LocalDateTime.now());
                    ledger.setNotes("Updated via duplicate callback");
                    ledgerRepository.save(ledger);
                    log.info("Ledger entry updated");
                } else {
                    log.warn("Status unchanged - Skipping ledger update");
                }

                log.warn("========== MERCHANT ROUTING END (DUPLICATE) ==========\n");
                return true; // Still return true as ledger is in correct state
            }

            // Create new ledger entry
            MerchantPaymentLedger ledger = MerchantPaymentLedger.builder()
                    .merchantId(merchantId)
                    .txnid(transaction.getTxnid())
                    .orderId(transaction.getUdf2() != null ? transaction.getUdf2() : "")
                    .amount(transaction.getAmount())
                    .status(transaction.getStatus())
                    .paymentMode(transaction.getMode())
                    .bankRefNum(transaction.getBankRefNum())
                    .gatewayId(transaction.getEasepayid())
                    .settlementStatus("PENDING")
                    .notes("Routed from payment transaction")
                    .build();

            ledgerRepository.save(ledger);

            log.info("Merchant ledger entry created - Ledger ID: {}, Status: {}, Amount: {}",
                    ledger.getId(), ledger.getStatus(), ledger.getAmount());

            // Log merchant routing event
            if (transaction.isSuccessful()) {
                log.info("SUCCESS PAYMENT ROUTED - Merchant: {}, Amount: {}, Reference: {}",
                        merchantId, transaction.getAmount(), transaction.getBankRefNum());
            } else {
                log.warn("FAILED PAYMENT ROUTED - Merchant: {}, Status: {}",
                        merchantId, transaction.getStatus());
            }

            log.info("========== MERCHANT ROUTING SUCCESS ==========\n");
            return true;

        } catch (MerchantNotActiveException e) {
            log.error("Merchant routing failed - Merchant not active", e);
            return false;
        } catch (Exception e) {
            log.error("ERROR routing payment to merchant - txnid: {}, error: {}",
                    transaction.getTxnid(), e.getMessage(), e);
            return false;
        }
    }

    /**
     * Check if merchant is active
     */
    private boolean isActiveMerchant(Merchant merchant) {
        if (merchant == null) {
            return false;
        }
        return "ACTIVE".equalsIgnoreCase(merchant.getStatus().toString());
    }

    /**
     * Get merchant ledger entry for a transaction
     * Used for order status display
     */
    public Optional<MerchantPaymentLedger> getMerchantLedger(String merchantId, String txnid) {
        log.debug("Fetching merchant ledger - Merchant: {}, Txnid: {}", merchantId, txnid);
        return ledgerRepository.findByMerchantIdAndTxnid(merchantId, txnid);
    }

    /**
     * Manually create ledger entry (for manual reconciliation scenarios)
     */
    @Transactional
    public MerchantPaymentLedger createLedgerEntry(
            String merchantId,
            String txnid,
            String orderId,
            java.math.BigDecimal amount,
            String status) {

        log.info("Creating manual ledger entry - Merchant: {}, Txnid: {}", merchantId, txnid);

        // Verify merchant is active
        Merchant merchant = merchantRepository.findByMerchantId(merchantId)
                .orElseThrow(() -> new RuntimeException("Merchant not found: " + merchantId));

        if (!isActiveMerchant(merchant)) {
            throw new MerchantNotActiveException(merchantId);
        }

        MerchantPaymentLedger ledger = MerchantPaymentLedger.builder()
                .merchantId(merchantId)
                .txnid(txnid)
                .orderId(orderId)
                .amount(amount)
                .status(status)
                .settlementStatus("PENDING")
                .notes("Manual ledger entry created")
                .build();

        ledgerRepository.save(ledger);
        log.info("Manual ledger entry created - Ledger ID: {}", ledger.getId());

        return ledger;
    }
}
