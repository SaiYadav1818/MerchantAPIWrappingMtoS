package com.sabbpe.merchant.service.payment;

import com.sabbpe.merchant.dto.EasebuzzInitiateRequest;
import com.sabbpe.merchant.dto.EasebuzzPaymentResponse;
import com.sabbpe.merchant.dto.PaymentResponse;
import com.sabbpe.merchant.entity.Transaction;
import com.sabbpe.merchant.entity.TransactionStatus;
import com.sabbpe.merchant.repository.TransactionRepository;
import com.sabbpe.merchant.service.EasebuzzService;
import com.sabbpe.merchant.util.EasebuzzHashUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Service for payment initiation with full UDF1-UDF10 support
 * 
 * Responsibilities:
 * - Generate payment hash with all UDF fields
 * - Validate payment request
 * - Create transaction record
 * - Call Easebuzz gateway
 * - Handle payment response
 * - Store payment details for audit
 */
@Slf4j
@Service
public class PaymentInitiationService {

    private final EasebuzzService easebuzzService;
    private final TransactionRepository transactionRepository;
    private final EasebuzzHashUtil hashUtil;

    @Value("${easebuzz.key}")
    private String easebuzzKey;

    @Value("${easebuzz.salt}")
    private String easebuzzSalt;

    @Value("${easebuzz.surl}")
    private String successUrl;

    @Value("${easebuzz.furl}")
    private String failureUrl;

    @Autowired
    public PaymentInitiationService(
            EasebuzzService easebuzzService,
            TransactionRepository transactionRepository,
            EasebuzzHashUtil hashUtil) {
        this.easebuzzService = easebuzzService;
        this.transactionRepository = transactionRepository;
        this.hashUtil = hashUtil;
    }

    /**
     * Initiate payment with full UDF1-UDF10 support
     * 
     * Flow:
     * 1. Validate request
     * 2. Generate SHA-512 hash with all UDF fields
     * 3. Create transaction record in database
     * 4. Call Easebuzz payment gateway
     * 5. Handle and return response
     * 
     * @param request Payment initiation request with UDF fields
     * @return PaymentResponse with payment URL or error
     */
    public PaymentResponse initiatePaymentWithUDF(EasebuzzInitiateRequest request) {
        try {
            log.info("Starting payment initiation with UDF. txnid: {}, amount: {}, merchant(udf1): {}",
                    request.getTxnid(), request.getAmount(), request.getUdf1());

            // Step 1: Validate request
            validatePaymentRequest(request);

            // Step 2: Generate hash with all UDF fields
            String hash = generatePaymentHash(request);
            request.setHash(hash);
            request.setKey(easebuzzKey);
            request.setSurl(successUrl);
            request.setFurl(failureUrl);

            log.info("Payment hash generated for txnid: {}", request.getTxnid());

            // Step 3: Create transaction record
            Transaction transaction = createTransactionRecord(request);
            transactionRepository.save(transaction);

            log.info("Transaction record created. txnid: {}, id: {}", 
                    request.getTxnid(), transaction.getId());

            // Step 4: Call Easebuzz gateway
            log.info("Calling Easebuzz payment gateway...");
            EasebuzzPaymentResponse gatewayResponse = easebuzzService.initiatePayment(request);

            log.info("Easebuzz response received. status: {}", gatewayResponse.getStatus());

            // Step 5: Build and return response
            if ("SUCCESS".equalsIgnoreCase(gatewayResponse.getStatus())) {
                log.info("Payment initiation successful. paymentUrl: {}", 
                        gatewayResponse.getPaymentUrl());

                return PaymentResponse.builder()
                        .transactionId(request.getTxnid())
                        .status("SUCCESS")
                        .message(gatewayResponse.getMessage())
                        .gatewayReference(gatewayResponse.getPaymentUrl())
                        .merchantId(request.getUdf1())
                        .orderId(request.getUdf2())
                        .timestamp(LocalDateTime.now())
                        .build();
            } else {
                log.error("Payment initiation failed. status: {}, message: {}",
                        gatewayResponse.getStatus(), gatewayResponse.getMessage());

                return PaymentResponse.builder()
                        .transactionId(request.getTxnid())
                        .status("FAILURE")
                        .message(gatewayResponse.getMessage())
                        .errorCode(gatewayResponse.getErrorCode())
                        .timestamp(LocalDateTime.now())
                        .build();
            }

        } catch (Exception e) {
            log.error("Error during payment initiation. txnid: {}, error: {}",
                    request.getTxnid(), e.getMessage(), e);

            return PaymentResponse.builder()
                    .transactionId(request.getTxnid())
                    .status("FAILURE")
                    .message("Payment initiation error: " + e.getMessage())
                    .errorCode("INIT_ERROR")
                    .timestamp(LocalDateTime.now())
                    .build();
        }
    }

    /**
     * Generate SHA-512 hash with all UDF1-UDF10 fields
     * 
     * Hash Format:
     * key|txnid|amount|productinfo|firstname|email|udf1|udf2|udf3|udf4|udf5|udf6|udf7|udf8|udf9|udf10|salt
     * 
     * @param request Payment request
     * @return SHA-512 hash as hexadecimal string
     */
    public String generatePaymentHash(EasebuzzInitiateRequest request) {
        log.debug("Generating payment hash with UDF fields for txnid: {}", request.getTxnid());

        return EasebuzzHashUtil.generateHashWithUDF(
                easebuzzKey,
                request.getTxnid(),
                request.getAmount().toString(),
                request.getProductinfo(),
                request.getFirstname(),
                request.getEmail(),
                request.getUdf1(),
                request.getUdf2(),
                request.getUdf3(),
                request.getUdf4(),
                request.getUdf5(),
                request.getUdf6(),
                request.getUdf7(),
                request.getUdf8(),
                request.getUdf9(),
                request.getUdf10(),
                easebuzzSalt
        );
    }

    /**
     * Create transaction record for payment initiation
     * 
     * @param request Payment request
     * @return Transaction entity
     */
    private Transaction createTransactionRecord(EasebuzzInitiateRequest request) {
        Transaction transaction = new Transaction();
        transaction.setTxnId(request.getTxnid());
        transaction.setAmount(request.getAmount());
        transaction.setProductinfo(request.getProductinfo());
        transaction.setCustomerEmail(request.getEmail());
        transaction.setCustomerName(request.getFirstname());
        transaction.setCustomerPhone(request.getPhone());
        transaction.setStatus(TransactionStatus.INITIATED);
        transaction.setHashGenerated(true);
        transaction.setInitiatedAt(LocalDateTime.now());
        transaction.setMerchantId(request.getUdf1()); // Use UDF1 as merchant ID
        transaction.setOrderId(request.getUdf2()); // Use UDF2 as order ID

        // Store all UDF fields for reference
        transaction.setUdf1(request.getUdf1());
        transaction.setUdf2(request.getUdf2());
        transaction.setUdf3(request.getUdf3());
        transaction.setUdf4(request.getUdf4());
        transaction.setUdf5(request.getUdf5());
        transaction.setUdf6(request.getUdf6());
        transaction.setUdf7(request.getUdf7());
        transaction.setUdf8(request.getUdf8());
        transaction.setUdf9(request.getUdf9());
        transaction.setUdf10(request.getUdf10());

        return transaction;
    }

    /**
     * Validate payment request before initiating
     * 
     * @param request Payment request
     * @throws IllegalArgumentException if validation fails
     */
    private void validatePaymentRequest(EasebuzzInitiateRequest request) {
        if (request.getTxnid() == null || request.getTxnid().isEmpty()) {
            throw new IllegalArgumentException("Transaction ID is required");
        }

        if (request.getAmount() == null || request.getAmount().compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Amount must be greater than 0");
        }

        if (request.getEmail() == null || request.getEmail().isEmpty()) {
            throw new IllegalArgumentException("Email is required");
        }

        if (request.getFirstname() == null || request.getFirstname().isEmpty()) {
            throw new IllegalArgumentException("Customer name is required");
        }

        // UDF1 (merchant ID) is recommended but not mandatory for basic initiation
        if (request.getUdf1() == null || request.getUdf1().isEmpty()) {
            log.warn("Warning: UDF1 (Merchant ID) is not provided. Multi-merchant routing may not work.");
        }

        log.debug("Payment request validation passed for txnid: {}", request.getTxnid());
    }

    /**
     * Get payment status by transaction ID
     * 
     * @param txnid Transaction ID
     * @return PaymentResponse with current status
     */
    public PaymentResponse getPaymentStatus(String txnid) {
        try {
            log.debug("Fetching payment status for txnid: {}", txnid);

            Transaction transaction = transactionRepository.findByTxnId(txnid)
                    .orElse(null);
            if (transaction == null) {
                log.warn("Transaction not found. txnid: {}", txnid);
                return null;
            }

            return PaymentResponse.builder()
                    .transactionId(txnid)
                    .status(transaction.getStatus().name())
                    .amount(transaction.getAmount())
                    .merchantId(transaction.getUdf1())
                    .orderId(transaction.getUdf2())
                    .timestamp(transaction.getInitiatedAt())
                    .build();

        } catch (Exception e) {
            log.error("Error fetching payment status for txnid: {}, error: {}",
                    txnid, e.getMessage(), e);
            return null;
        }
    }
}
