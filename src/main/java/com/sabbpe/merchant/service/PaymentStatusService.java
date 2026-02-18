package com.sabbpe.merchant.service;

import com.sabbpe.merchant.dto.PaymentStatusResponse;
import com.sabbpe.merchant.entity.PaymentTransaction;
import com.sabbpe.merchant.exception.TransactionNotFoundException;
import com.sabbpe.merchant.repository.PaymentTransactionRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * PaymentStatusService
 * 
 * Provides transaction status lookup functionality.
 * 
 * Responsibilities:
 * 1. Query payment transaction by txnid
 * 2. Convert entity to DTO with all merchant routing info
 * 3. Handle not found scenarios
 */
@Slf4j
@Service
@Transactional(readOnly = true)
public class PaymentStatusService {

    private final PaymentTransactionRepository paymentTransactionRepository;

    @Autowired
    public PaymentStatusService(PaymentTransactionRepository paymentTransactionRepository) {
        this.paymentTransactionRepository = paymentTransactionRepository;
    }

    /**
     * Get payment status by transaction ID
     * 
     * @param txnid Transaction ID from payment gateway
     * @return PaymentStatusResponse with complete transaction details
     * @throws TransactionNotFoundException if transaction not found
     */
    public PaymentStatusResponse getPaymentStatus(String txnid) {
        log.info("Fetching payment status for txnid: {}", txnid);

        if (txnid == null || txnid.trim().isEmpty()) {
            log.warn("Invalid txnid provided: {}", txnid);
            throw new TransactionNotFoundException("No transaction ID provided");
        }

        PaymentTransaction transaction = paymentTransactionRepository.findByTxnid(txnid)
                .orElseThrow(() -> {
                    log.warn("Transaction not found for txnid: {}", txnid);
                    return new TransactionNotFoundException("Transaction not found with txnid: " + txnid);
                });

        log.info("Transaction found - Status: {}, Amount: {}, Merchant: {}",
                transaction.getStatus(), transaction.getAmount(), transaction.getUdf1());

        // Convert entity to DTO
        PaymentStatusResponse response = convertToResponse(transaction);

        log.debug("Payment status response prepared for txnid: {}", txnid);
        return response;
    }

    /**
     * Convert PaymentTransaction entity to PaymentStatusResponse DTO
     */
    private PaymentStatusResponse convertToResponse(PaymentTransaction transaction) {
        return PaymentStatusResponse.builder()
                .txnid(transaction.getTxnid())
                .status(transaction.getStatus())
                .amount(transaction.getAmount())
                .errorMessage(transaction.getErrorMessage())
                // Bank Details
                .bankRefNum(transaction.getBankRefNum())
                .bankName(transaction.getBankName())
                .mode(transaction.getMode())
                .easepayid(transaction.getEasepayid())
                .bankcode(transaction.getBankcode())
                // UDF Fields
                .udf1(transaction.getUdf1())
                .udf2(transaction.getUdf2())
                .udf3(transaction.getUdf3())
                .udf4(transaction.getUdf4())
                .udf5(transaction.getUdf5())
                .udf6(transaction.getUdf6())
                .udf7(transaction.getUdf7())
                .udf8(transaction.getUdf8())
                .udf9(transaction.getUdf9())
                .udf10(transaction.getUdf10())
                // Customer Information
                .firstname(transaction.getFirstname())
                .email(transaction.getEmail())
                .phone(transaction.getPhone())
                // Additional Details
                .bankName(transaction.getBankName())
                .issuingBank(transaction.getIssuingBank())
                .cardType(transaction.getCardType())
                .hashVerified(transaction.getHashVerified())
                // Timestamps
                .createdAt(transaction.getCreatedAt())
                .updatedAt(transaction.getUpdatedAt())
                // Raw Response
                .rawResponse(transaction.getRawResponse())
                .build();
    }

    /**
     * Get payment transaction entity by txnid
     * 
     * Used internally by other services for further processing.
     * 
     * @param txnid Transaction ID
     * @return PaymentTransaction entity
     * @throws TransactionNotFoundException if not found
     */
    public PaymentTransaction getTransaction(String txnid) {
        log.debug("Fetching transaction entity for txnid: {}", txnid);
        return paymentTransactionRepository.findByTxnid(txnid)
                .orElseThrow(() -> new TransactionNotFoundException(
                        "Transaction not found with txnid: " + txnid));
    }

    /**
     * Check if transaction exists
     */
    public boolean transactionExists(String txnid) {
        return paymentTransactionRepository.existsByTxnid(txnid);
    }
}
