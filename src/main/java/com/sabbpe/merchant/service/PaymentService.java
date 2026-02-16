package com.sabbpe.merchant.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sabbpe.merchant.config.EasebuzzConfig;
import com.sabbpe.merchant.dto.*;
import com.sabbpe.merchant.entity.EasebuzzPayment;
import com.sabbpe.merchant.entity.Merchant;
import com.sabbpe.merchant.entity.MerchantStatus;
import com.sabbpe.merchant.entity.Transaction;
import com.sabbpe.merchant.entity.TransactionStatus;
import com.sabbpe.merchant.exception.GatewayException;
import com.sabbpe.merchant.exception.HashMismatchException;
import com.sabbpe.merchant.exception.MerchantNotFoundException;
import com.sabbpe.merchant.exception.ValidationException;
import com.sabbpe.merchant.repository.EasebuzzPaymentRepository;
import com.sabbpe.merchant.repository.MerchantRepository;
import com.sabbpe.merchant.repository.TransactionRepository;
import com.sabbpe.merchant.util.HashUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestTemplate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@Service
public class PaymentService {

    private static final Logger logger = LoggerFactory.getLogger(PaymentService.class);

    @Autowired
    private MerchantRepository merchantRepository;

    @Autowired
    private TransactionRepository transactionRepository;

    @Autowired
    private EasebuzzPaymentRepository easebuzzPaymentRepository;

    @Autowired
    private RestTemplate restTemplate;

    @Autowired
    private EasebuzzConfig easebuzzConfig;

    @Autowired
    private ObjectMapper objectMapper;

    @Transactional
    public PaymentResponse initiatePayment(PaymentInitiateRequest request) {
        System.out.println("[DEBUG] Initiating payment verification for merchant: " + request.getMerchantId() + ", orderId: " + request.getOrderId());

        Merchant merchant = merchantRepository
            .findByMerchantId(request.getMerchantId())
            .orElseThrow(() -> {
                System.out.println("[WARN] Merchant not found: " + request.getMerchantId());
                return new MerchantNotFoundException(
                    "Merchant not found for ID: " + request.getMerchantId());
            });

        if (!merchant.getStatus().equals(MerchantStatus.ACTIVE)) {
            System.out.println("[WARN] Merchant inactive: " + request.getMerchantId() + " with status: " + merchant.getStatus());
            throw new MerchantNotFoundException(
                "Merchant is not active for ID: " + request.getMerchantId());
        }

        String hashInput = request.getMerchantId() + request.getOrderId() + 
                          request.getAmount() + merchant.getSaltKey();
        String generatedHash = HashUtil.generateSHA256(hashInput);
        
        System.out.println("[DEBUG] Hash verification for orderId: " + request.getOrderId());
        System.out.println("[DEBUG] Generated hash input: " + hashInput);

        if (!generatedHash.equals(request.getHash())) {
            System.out.println("[WARN] Hash mismatch for orderId: " + request.getOrderId() + ". Expected: " + generatedHash + ", Received: " + request.getHash());
            throw new HashMismatchException(
                "Hash verification failed for order: " + request.getOrderId());
        }

        String internalToken = UUID.randomUUID().toString();
        
        Transaction transaction = Transaction.builder()
            .orderId(request.getOrderId())
            .merchantId(request.getMerchantId())
            .amount(request.getAmount())
            .status(TransactionStatus.INITIATED)
            .internalToken(internalToken)
            .build();

        transactionRepository.save(transaction);
        
        System.out.println("[INFO] Payment initiated successfully for orderId: " + request.getOrderId() + " with token: " + internalToken);

        return PaymentResponse.builder()
            .status("SUCCESS")
            .internalToken(internalToken)
            .orderId(request.getOrderId())
            .build();
    }

    /**
     * Generates SHA-256 hash for payment request details
     * 
     * @param request The hash generation request containing merchantId, orderId, and amount
     * @return HashResponse containing the generated hash
     * @throws MerchantNotFoundException if merchant not found or inactive
     */
    @Transactional(readOnly = true)
    public HashResponse generateHash(HashGenerateRequest request) {
        logger.info("Generating hash for merchantId: {}, orderId: {}", request.getMerchantId(), request.getOrderId());

        // Fetch merchant from database
        Merchant merchant = merchantRepository
            .findByMerchantId(request.getMerchantId())
            .orElseThrow(() -> {
                logger.warn("Merchant not found: {}", request.getMerchantId());
                return new MerchantNotFoundException(
                    "Merchant not found for ID: " + request.getMerchantId());
            });

        // Check if merchant is active
        if (!merchant.getStatus().equals(MerchantStatus.ACTIVE)) {
            logger.warn("Merchant is not active: {} with status: {}", request.getMerchantId(), merchant.getStatus());
            throw new MerchantNotFoundException(
                "Merchant is not active for ID: " + request.getMerchantId());
        }

        // Generate hash using formula: SHA-256(merchantId + orderId + amount + saltKey)
        String hashInput = request.getMerchantId() + request.getOrderId() + 
                          request.getAmount() + merchant.getSaltKey();
        String generatedHash = HashUtil.generateSHA256(hashInput);
        
        logger.debug("Hash generated successfully. OrderId: {}, Hash: {}", request.getOrderId(), generatedHash);
        logger.debug("Hash input formula: merchantId + orderId + amount + saltKey");

        return new HashResponse(generatedHash, request.getOrderId());
    }

    /**
     * Initiates Easebuzz payment
     * 
     * @param request EasebuzzPaymentInitiateRequest with amount, productInfo, firstname, email, phone
     * @param merchantId The merchant ID extracted from JWT token
     * @return EasebuzzPaymentResponse with payment URL
     * @throws MerchantNotFoundException if merchant not found or not active
     * @throws GatewayException if Easebuzz API call fails
     */
    @Transactional
    public EasebuzzPaymentResponse initiateEasebuzzPayment(EasebuzzPaymentInitiateRequest request, String merchantId) {
        logger.info("Initiating Easebuzz payment for merchant: {}, amount: {}", merchantId, request.getAmount());

        // Step 1: Fetch and validate merchant
        Merchant merchant = merchantRepository
            .findByMerchantId(merchantId)
            .orElseThrow(() -> {
                logger.warn("Merchant not found: {}", merchantId);
                return new MerchantNotFoundException("Merchant not found for ID: " + merchantId);
            });

        if (!merchant.getStatus().equals(MerchantStatus.ACTIVE)) {
            logger.warn("Merchant is not active: {} with status: {}", merchantId, merchant.getStatus());
            throw new MerchantNotFoundException("Merchant is not active for ID: " + merchantId);
        }

        // Step 2: Generate transaction ID
        String txnId = "TXN" + System.currentTimeMillis();
        logger.debug("Generated txnId: {}", txnId);

        // Step 3: Build hash string for Easebuzz
        // Hash = SHA512(key|txnid|amount|productinfo|firstname|email|||||||||salt)
        String hashString = merchant.getMerchantId() + "|" + txnId + "|" + request.getAmount() + "|" +
                           request.getProductInfo() + "|" + request.getFirstName() + "|" + request.getEmail() +
                           "|||||||" + merchant.getSaltKey();
        String hash = HashUtil.generateSHA512(hashString);
        logger.debug("Generated hash for txnId: {}", txnId);

        // Step 4: Prepare form data for Easebuzz API
        MultiValueMap<String, String> formData = new LinkedMultiValueMap<>();
        formData.add("key", merchant.getMerchantId());
        formData.add("txnid", txnId);
        formData.add("amount", request.getAmount().toString());
        formData.add("productinfo", request.getProductInfo());
        formData.add("firstname", request.getFirstName());
        formData.add("email", request.getEmail());
        formData.add("phone", request.getPhone());
        formData.add("surl", easebuzzConfig.getSurl());
        formData.add("furl", easebuzzConfig.getFurl());
        formData.add("hash", hash);

        // Step 5: Call Easebuzz initiate API
        String initiateUrl = easebuzzConfig.getUrl().getInitiate();
        logger.info("Calling Easebuzz initiate API: {}", initiateUrl);

        try {
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);
            HttpEntity<MultiValueMap<String, String>> requestEntity = new HttpEntity<>(formData, headers);

            ResponseEntity<Map> response = restTemplate.postForEntity(initiateUrl, requestEntity, Map.class);
            Map<String, Object> responseBody = response.getBody();

            logger.info("Easebuzz API response status: {}, body: {}", response.getStatusCode(), responseBody);

            // Step 6: Validate response
            if (responseBody == null || responseBody.get("status") == null) {
                logger.error("Invalid Easebuzz response: missing status");
                throw new GatewayException("Invalid Easebuzz response format", 502);
            }

            Object statusObj = responseBody.get("status");
            int status = statusObj instanceof Integer ? (Integer) statusObj : 
                        Integer.parseInt(statusObj.toString());

            if (status != 1) {
                logger.error("Easebuzz API failed with status: {}", status);
                throw new GatewayException("Easebuzz payment initiation failed with status: " + status, 502);
            }

            String accessKey = responseBody.get("data") != null ? responseBody.get("data").toString() : null;
            if (accessKey == null) {
                logger.error("Easebuzz response missing access key");
                throw new GatewayException("Easebuzz response missing access key", 502);
            }

            // Step 7: Save transaction in database
            String rawResponse = objectMapper.writeValueAsString(responseBody);
            EasebuzzPayment payment = new EasebuzzPayment();
            payment.setTxnId(txnId);
            payment.setMerchantId(merchantId);
            payment.setAmount(request.getAmount());
            payment.setGatewayStatus("INITIATED");
            payment.setNormalizedStatus("INITIATED");
            payment.setHash(hash);
            payment.setHashValidated(false);
            payment.setRawResponse(rawResponse);

            easebuzzPaymentRepository.save(payment);
            logger.info("Payment transaction saved in database with txnId: {}", txnId);

            // Step 8: Return payment URL
            String paymentUrl = easebuzzConfig.getUrl().getPayment() + accessKey;
            logger.info("Payment URL generated: {}", paymentUrl);

           return EasebuzzPaymentResponse.error("FAILED", "Invalid hash", "HASH_ERROR");


        } catch (GatewayException e) {
            logger.error("Gateway exception: {}", e.getMessage(), e);
            throw e;
        } catch (JsonProcessingException e) {
            logger.error("JSON processing error: {}", e.getMessage(), e);
            throw new GatewayException("Error processing Easebuzz response", 502, e);
        } catch (Exception e) {
            logger.error("Unexpected error during payment initiation: {}", e.getMessage(), e);
            throw new GatewayException("Payment initiation failed: " + e.getMessage(), 502, e);
        }
    }

    /**
     * Processes callback from Easebuzz payment gateway
     * 
     * @param callbackParams Map of all callback parameters from Easebuzz
     * @return true if processing was successful
     */
    @Transactional
    public boolean processEasebuzzCallback(Map<String, String> callbackParams) {
        logger.info("Processing Easebuzz callback");

        try {
            // Extract callback parameters
            String txnId = callbackParams.get("txnid");
            String status = callbackParams.get("status");
            String hash = callbackParams.get("hash");
            String email = callbackParams.get("email");
            String firstname = callbackParams.get("firstname");
            String amount = callbackParams.get("amount");
            String productinfo = callbackParams.get("productinfo");
            String key = callbackParams.get("key");

            logger.debug("Callback params - txnId: {}, status: {}, key: {}", txnId, status, key);

            if (txnId == null || key == null) {
                logger.warn("Missing required callback parameters");
                return false;
            }

            // Fetch merchant using key
            Merchant merchant = merchantRepository
                .findByMerchantId(key)
                .orElseThrow(() -> new MerchantNotFoundException("Merchant not found for key: " + key));

            // Fetch or create payment record
            EasebuzzPayment payment = easebuzzPaymentRepository.findByTxnId(txnId)
                .orElseGet(() -> {
                    logger.info("Creating new payment record for txnId: {}", txnId);
                    EasebuzzPayment newPayment = new EasebuzzPayment();
                    newPayment.setTxnId(txnId);
                    newPayment.setMerchantId(key);
                    newPayment.setAmount(amount != null ? new BigDecimal(amount) : BigDecimal.ZERO);
                    return newPayment;
                });

            // Build reverse hash for verification
            // reverse_hash = SHA512(salt|status||||||||email|firstname|productinfo|amount|txnid|key)
            String reverseHashString = merchant.getSaltKey() + "|" + status + "||||||||" +
                                      email + "|" + firstname + "|" + productinfo + "|" +
                                      amount + "|" + txnId + "|" + merchant.getMerchantId();
            String calculatedHash = HashUtil.generateSHA512(reverseHashString);

            logger.debug("Hash validation - received: {}, calculated: {}", hash, calculatedHash);

            boolean hashValid = hash != null && hash.equals(calculatedHash);
            payment.setHashValidated(hashValid);
            payment.setHash(hash);

            // Map gateway status to normalized status
            String normalizedStatus;
            if (!hashValid) {
                normalizedStatus = "HASH_MISMATCH";
                logger.warn("Hash mismatch for txnId: {}", txnId);
            } else {
                normalizedStatus = mapGatewayStatusToNormalized(status);
            }

            payment.setGatewayStatus(status != null ? status : "UNKNOWN");
            payment.setNormalizedStatus(normalizedStatus);

            // Store raw response
            String rawResponse = objectMapper.writeValueAsString(callbackParams);
            payment.setRawResponse(rawResponse);

            easebuzzPaymentRepository.save(payment);
            logger.info("Callback processed and saved for txnId: {}, normalized status: {}", txnId, normalizedStatus);

            return true;

        } catch (JsonProcessingException e) {
            logger.error("Error processing callback data: {}", e.getMessage(), e);
            return false;
        } catch (Exception e) {
            logger.error("Unexpected error processing callback: {}", e.getMessage(), e);
            return false;
        }
    }

    /**
     * Maps Easebuzz gateway status to normalized status
     */
    private String mapGatewayStatusToNormalized(String gatewayStatus) {
        if (gatewayStatus == null) {
            return "UNKNOWN";
        }

        return switch (gatewayStatus.toLowerCase()) {
            case "success" -> "SUCCESS";
            case "failure" -> "FAILED";
            case "usercancelled", "cancel" -> "CANCELLED";
            case "timeout" -> "TIMEOUT";
            case "pending" -> "PENDING";
            default -> "UNKNOWN";
        };
    }

    /**
     * Retrieves payment status for a given transaction ID
     * 
     * @param txnId Transaction ID
     * @return PaymentStatusResponse with current payment status
     * @throws ValidationException if transaction not found
     */
    @Transactional(readOnly = true)
    public PaymentStatusResponse getPaymentStatus(String txnId) {
        logger.info("Fetching payment status for txnId: {}", txnId);

        EasebuzzPayment payment = easebuzzPaymentRepository.findByTxnId(txnId)
            .orElseThrow(() -> {
                logger.warn("Payment not found for txnId: {}", txnId);
                return new ValidationException("Payment not found for txnId: " + txnId);
            });

        return new PaymentStatusResponse(
            payment.getTxnId(),
            payment.getAmount(),
            payment.getNormalizedStatus(),
            payment.getGatewayStatus()
        );
    }

    /**
     * Processes refund request (optional implementation)
     * 
     * @param txnId Transaction ID to refund
     * @param refundAmount Amount to refund
     * @return true if refund was processed successfully
     */
    @Transactional
    public boolean processRefund(String txnId, BigDecimal refundAmount) {
        logger.info("Processing refund for txnId: {}, amount: {}", txnId, refundAmount);

        EasebuzzPayment payment = easebuzzPaymentRepository.findByTxnId(txnId)
            .orElseThrow(() -> new ValidationException("Payment not found for txnId: " + txnId));

        if (!payment.getNormalizedStatus().equals("SUCCESS")) {
            logger.warn("Cannot refund non-successful payment. Status: {}", payment.getNormalizedStatus());
            throw new ValidationException("Cannot refund payment with status: " + payment.getNormalizedStatus());
        }

        // Here you would typically call the Easebuzz refund API
        // For now, we just mark it as refunded in the database
        payment.setNormalizedStatus("REFUNDED");
        payment.setGatewayStatus("REFUNDED");
        easebuzzPaymentRepository.save(payment);

        logger.info("Refund processed for txnId: {}", txnId);
        return true;
    }
}

