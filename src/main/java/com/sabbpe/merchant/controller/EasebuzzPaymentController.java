package com.sabbpe.merchant.controller;

import com.sabbpe.merchant.dto.EasebuzzInitiateRequest;
import com.sabbpe.merchant.dto.EasebuzzPaymentResponse;
import com.sabbpe.merchant.service.EasebuzzService;
import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * REST Controller for Easebuzz payment gateway integration
 * Handles payment initiation requests and communicates with Easebuzz
 */
@Slf4j
@RestController
@RequestMapping("/api/payment")
public class EasebuzzPaymentController {

    private final EasebuzzService easebuzzService;

    @Value("${easebuzz.key}")
    private String easebuzzKey;

    @Value("${easebuzz.surl}")
    private String successUrl;

    @Value("${easebuzz.furl}")
    private String failureUrl;

    @Autowired
    public EasebuzzPaymentController(EasebuzzService easebuzzService) {
        this.easebuzzService = easebuzzService;
    }

    /**
     * Initiate payment with Easebuzz gateway
     *
     * @param initiateRequest Request body containing payment details (txnid, amount, productinfo, firstname, phone, email)
     * @return EasebuzzPaymentResponse with payment URL for successful initiation
     */
    @PostMapping("/easebuzz/initiate")
    public ResponseEntity<EasebuzzPaymentResponse> initiatePayment(
            @Valid @RequestBody EasebuzzInitiateRequest initiateRequest) {

        try {
            log.info("Payment initiation request received for transaction: {}", initiateRequest.getTxnid());

            // Auto-fill Easebuzz key
            initiateRequest.setKey(easebuzzKey);

            // Set success and failure URLs
            if (initiateRequest.getSurl() == null || initiateRequest.getSurl().isEmpty()) {
                initiateRequest.setSurl(successUrl);
            }
            if (initiateRequest.getFurl() == null || initiateRequest.getFurl().isEmpty()) {
                initiateRequest.setFurl(failureUrl);
            }

            // Set placeholder hash (will be generated in service)
            initiateRequest.setHash("");

            log.debug("Calling EasebuzzService for transaction: {}", initiateRequest.getTxnid());

            // Call service to initiate payment
            EasebuzzPaymentResponse response = easebuzzService.initiatePayment(initiateRequest);

            log.info("Payment initiated successfully for transaction: {}", initiateRequest.getTxnid());

            return ResponseEntity.ok(response);

        } catch (IllegalArgumentException e) {
            log.error("Invalid request parameter for transaction initiation: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(new EasebuzzPaymentResponse(0, null));

        } catch (Exception e) {
            log.error("Error initiating payment: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new EasebuzzPaymentResponse(0, null));
        }
    }
}
