package com.sabbpe.merchant.service;

import com.sabbpe.merchant.dto.EasebuzzInitiateRequest;
import com.sabbpe.merchant.dto.EasebuzzPaymentResponse;
import com.sabbpe.merchant.dto.ErrorResponse;
import com.sabbpe.merchant.exception.GatewayException;
import com.sabbpe.merchant.util.HashGeneratorUtil;
import com.sabbpe.merchant.util.EasebuzzErrorHandler;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import java.util.Map;

@Slf4j
@Service
public class EasebuzzService {

    private final RestTemplate restTemplate;
    private final HashGeneratorUtil hashGeneratorUtil;
    private final EasebuzzErrorHandler errorHandler;

    @Value("${easebuzz.key}")
    private String easebuzzKey;

    @Value("${easebuzz.salt}")
    private String easebuzzSalt;

    @Value("${easebuzz.url.initiate}")
    private String initiateUrl;

    @Autowired
    public EasebuzzService(RestTemplate restTemplate,
                           HashGeneratorUtil hashGeneratorUtil,
                           EasebuzzErrorHandler errorHandler) {
        this.restTemplate = restTemplate;
        this.hashGeneratorUtil = hashGeneratorUtil;
        this.errorHandler = errorHandler;
    }

    // ======================================================
    // PAYMENT INITIATION
    // ======================================================

    public EasebuzzPaymentResponse initiatePayment(EasebuzzInitiateRequest request) {

        try {
            log.info("Initiating payment with Easebuzz. TxnId={}", request.getTxnid());

            // --------------------------------------------------
            // Generate hash (udf1 → udf10)
            // --------------------------------------------------
            String hash = hashGeneratorUtil.generateHash(
                    easebuzzKey,
                    request.getTxnid(),
                    request.getAmount(),
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

            // --------------------------------------------------
            // Build request body
            // --------------------------------------------------
            MultiValueMap<String, String> body = buildRequestBody(request, hash);

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);

            HttpEntity<MultiValueMap<String, String>> httpEntity =
                    new HttpEntity<>(body, headers);

            // --------------------------------------------------
            // Call Easebuzz API
            // --------------------------------------------------
            ResponseEntity<Map> response =
                    restTemplate.postForEntity(initiateUrl, httpEntity, Map.class);

            log.info("Easebuzz RAW response: {}", response.getBody());

            if (response.getBody() == null) {
                throw new GatewayException("Empty response from Easebuzz gateway");
            }

            // Parse response with transaction ID for better error tracking
            return parseEasebuzzResponse(response.getBody(), request.getTxnid());

        } catch (RestClientException ex) {
            log.error("Easebuzz communication error for TxnId: {}", request.getTxnid(), ex);
            throw new GatewayException("Gateway communication failed", ex);
        }
    }

    // ======================================================
    // BUILD REQUEST BODY
    // ======================================================

    private MultiValueMap<String, String> buildRequestBody(
            EasebuzzInitiateRequest request,
            String hash) {

        MultiValueMap<String, String> body = new LinkedMultiValueMap<>();

        body.add("key", easebuzzKey);
        body.add("txnid", request.getTxnid());
        body.add("amount", request.getAmount().toString());
        body.add("productinfo", request.getProductinfo());
        body.add("firstname", request.getFirstname());
        body.add("phone", request.getPhone());
        body.add("email", request.getEmail());
        body.add("surl", request.getSurl());
        body.add("furl", request.getFurl());
        body.add("hash", hash);

        // UDF fields (1 → 10)
        addIfPresent(body, "udf1", request.getUdf1());
        addIfPresent(body, "udf2", request.getUdf2());
        addIfPresent(body, "udf3", request.getUdf3());
        addIfPresent(body, "udf4", request.getUdf4());
        addIfPresent(body, "udf5", request.getUdf5());
        addIfPresent(body, "udf6", request.getUdf6());
        addIfPresent(body, "udf7", request.getUdf7());
        addIfPresent(body, "udf8", request.getUdf8());
        addIfPresent(body, "udf9", request.getUdf9());
        addIfPresent(body, "udf10", request.getUdf10());

        return body;
    }

    private void addIfPresent(MultiValueMap<String, String> body,
                              String key,
                              String value) {
        if (value != null && !value.isBlank()) {
            body.add(key, value);
        }
    }

    // ======================================================
    // RESPONSE PARSER
    // ======================================================

    /**
     * Parse Easebuzz response and handle errors gracefully.
     * For errors (including duplicates), throws GatewayException with error details.
     * The controller should catch this and return appropriate error response.
     */
    private EasebuzzPaymentResponse parseEasebuzzResponse(
            Map<String, Object> responseBody,
            String transactionId) {

        int status = errorHandler.extractStatus(responseBody.get("status"));

        // ---------------- SUCCESS ----------------
        if (status == 1) {
            String data = (String) responseBody.get("data");

            if (data == null || data.isBlank()) {
                String errorMsg = "Payment initiated but no reference received";
                log.error("Easebuzz error - TxnId: {}, Error: {}", transactionId, errorMsg);
                throw new GatewayException(errorMsg);
            }

            log.info("Easebuzz payment initiated successfully. TxnId={}, Reference={}", 
                    transactionId, data);
            return new EasebuzzPaymentResponse(status, data);
        }

        // ---------------- FAILURE ----------------
        // Use error handler to parse and categorize the error
        ErrorResponse errorResponse = errorHandler.handleEasebuzzError(responseBody, transactionId);
        
        // Log detailed error information with transaction ID for traceability
        log.error("Easebuzz payment initiation failed. TxnId: {}, ErrorType: {}, Message: {}, ErrorCode: {}, Details: {}",
                transactionId,
                errorResponse.getErrorType(),
                errorResponse.getMessage(),
                errorResponse.getErrorCode(),
                errorResponse.getDetails());

        // Throw exception with error response so controller can handle it
        GatewayException ex = new GatewayException(errorResponse.getMessage());
        ex.setErrorResponse(errorResponse);  // Attach error response for controller
        throw ex;
    }

    private int extractStatus(Object statusObj) {

        if (statusObj instanceof Integer) {
            return (Integer) statusObj;
        }

        if (statusObj instanceof String) {
            try {
                return Integer.parseInt((String) statusObj);
            } catch (NumberFormatException ignored) {}
        }

        return 0;
    }
}
