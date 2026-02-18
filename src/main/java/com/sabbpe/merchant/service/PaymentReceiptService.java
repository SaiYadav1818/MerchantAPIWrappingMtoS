package com.sabbpe.merchant.service;

import com.sabbpe.merchant.dto.PaymentReceiptResponse;
import com.sabbpe.merchant.entity.Merchant;
import com.sabbpe.merchant.entity.PaymentTransaction;
import com.sabbpe.merchant.exception.MerchantNotActiveException;
import com.sabbpe.merchant.exception.TransactionNotFoundException;
import com.sabbpe.merchant.repository.MerchantRepository;
import com.sabbpe.merchant.repository.PaymentTransactionRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * PaymentReceiptService
 * 
 * Generates payment receipt data for display/download.
 * 
 * Responsibilities:
 * 1. Fetch transaction by txnid
 * 2. Fetch merchant details
 * 3. Format receipt response
 * 4. Handle errors
 */
@Slf4j
@Service
@Transactional(readOnly = true)
public class PaymentReceiptService {

    private final PaymentTransactionRepository paymentTransactionRepository;
    private final MerchantRepository merchantRepository;

    @Autowired
    public PaymentReceiptService(
            PaymentTransactionRepository paymentTransactionRepository,
            MerchantRepository merchantRepository) {
        this.paymentTransactionRepository = paymentTransactionRepository;
        this.merchantRepository = merchantRepository;
    }

    /**
     * Generate payment receipt for a transaction
     * 
     * @param txnid Transaction ID
     * @return PaymentReceiptResponse with receipt data
     * @throws TransactionNotFoundException if transaction not found
     * @throws MerchantNotActiveException if merchant is not active
     */
    public PaymentReceiptResponse getPaymentReceipt(String txnid) {
        log.info("Generating payment receipt for txnid: {}", txnid);

        if (txnid == null || txnid.trim().isEmpty()) {
            log.warn("Invalid txnid provided for receipt: {}", txnid);
            throw new TransactionNotFoundException("No transaction ID provided");
        }

        // Fetch transaction
        PaymentTransaction transaction = paymentTransactionRepository.findByTxnid(txnid)
                .orElseThrow(() -> {
                    log.warn("Transaction not found for receipt generation - txnid: {}", txnid);
                    return new TransactionNotFoundException("Transaction not found with txnid: " + txnid);
                });

        log.info("Transaction found for receipt - Merchant: {}, Amount: {}",
                transaction.getUdf1(), transaction.getAmount());

        // Extract merchant ID from UDF1
        String merchantId = transaction.getUdf1();
        String merchantName = "Unknown Merchant";

        // Fetch merchant details if merchant ID exists
        if (merchantId != null && !merchantId.isEmpty()) {
            try {
                Merchant merchant = merchantRepository.findByMerchantId(merchantId)
                        .orElse(null);

                if (merchant != null) {
                    // Verify merchant is active
                    if (!isActiveMerchant(merchant)) {
                        log.warn("Merchant is not active - merchantId: {}", merchantId);
                        throw new MerchantNotActiveException(merchantId);
                    }
                    merchantName = merchant.getMerchantName();
                    log.debug("Merchant found and active - Name: {}", merchantName);
                } else {
                    log.warn("Merchant not found in database - merchantId: {}", merchantId);
                    // Continue with unknown merchant name
                }
            } catch (Exception e) {
                log.warn("Error fetching merchant details for receipt - merchantId: {}, error: {}",
                        merchantId, e.getMessage());
                // Continue with receipt generation (merchant name remains "Unknown Merchant")
            }
        }

        // Build receipt response
        PaymentReceiptResponse receipt = PaymentReceiptResponse.fromPaymentTransaction(
                transaction, merchantName);

        log.info("Payment receipt generated successfully for txnid: {}", txnid);
        return receipt;
    }

    /**
     * Check if merchant is active
     */
    private boolean isActiveMerchant(Merchant merchant) {
        if (merchant == null) {
            return false;
        }
        // Check if merchant status is ACTIVE
        return "ACTIVE".equalsIgnoreCase(merchant.getStatus().toString());
    }

    /**
     * Get receipt in formatted text format (for display/printing)
     */
    public String getFormattedReceipt(String txnid) {
        log.info("Generating formatted receipt text for txnid: {}", txnid);
        PaymentReceiptResponse receipt = getPaymentReceipt(txnid);
        return receipt.getFormattedReceipt();
    }
}
