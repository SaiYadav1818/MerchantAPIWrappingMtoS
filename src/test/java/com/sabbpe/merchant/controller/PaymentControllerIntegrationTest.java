package com.sabbpe.merchant.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.sabbpe.merchant.dto.PaymentInitiateRequest;
import com.sabbpe.merchant.entity.Merchant;
import com.sabbpe.merchant.entity.MerchantStatus;
import com.sabbpe.merchant.repository.MerchantRepository;
import com.sabbpe.merchant.util.HashUtil;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.context.ActiveProfiles;

import java.math.BigDecimal;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
public class PaymentControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private MerchantRepository merchantRepository;

    @Autowired
    private ObjectMapper objectMapper;

    private Merchant testMerchant;
    private static final String MERCHANT_ID = "TEST_M001";
    private static final String ORDER_ID = "TEST_ORD001";
    private static final BigDecimal AMOUNT = new BigDecimal("5000.00");

    @BeforeEach
    public void setUp() {
        // Clean up before each test
        merchantRepository.deleteAll();

        // Create test merchant
        testMerchant = Merchant.builder()
            .merchantId(MERCHANT_ID)
            .merchantName("Test Merchant")
            .saltKey("test_salt_key_12345")
            .status(MerchantStatus.ACTIVE)
            .build();

        merchantRepository.save(testMerchant);
    }

    @Test
    public void testPaymentInitiationSuccess() throws Exception {
        // Generate correct hash
        String hashInput = MERCHANT_ID + ORDER_ID + AMOUNT + testMerchant.getSaltKey();
        String correctHash = HashUtil.generateSHA256(hashInput);

        PaymentInitiateRequest request = PaymentInitiateRequest.builder()
            .merchantId(MERCHANT_ID)
            .orderId(ORDER_ID)
            .amount(AMOUNT)
            .hash(correctHash)
            .build();

        mockMvc.perform(post("/api/payment/initiate")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.status").value("SUCCESS"))
            .andExpect(jsonPath("$.orderId").value(ORDER_ID))
            .andExpect(jsonPath("$.internalToken").isNotEmpty());
    }

    @Test
    public void testPaymentInitiationHashMismatch() throws Exception {
        PaymentInitiateRequest request = PaymentInitiateRequest.builder()
            .merchantId(MERCHANT_ID)
            .orderId(ORDER_ID)
            .amount(AMOUNT)
            .hash("incorrect_hash_value")
            .build();

        mockMvc.perform(post("/api/payment/initiate")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.status").value("FAILURE"))
            .andExpect(jsonPath("$.errorCode").value("HASH_MISMATCH"));
    }

    @Test
    public void testPaymentInitiationMerchantNotFound() throws Exception {
        PaymentInitiateRequest request = PaymentInitiateRequest.builder()
            .merchantId("NONEXISTENT")
            .orderId(ORDER_ID)
            .amount(AMOUNT)
            .hash("any_hash")
            .build();

        mockMvc.perform(post("/api/payment/initiate")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isUnauthorized())
            .andExpect(jsonPath("$.status").value("FAILURE"))
            .andExpect(jsonPath("$.errorCode").value("MERCHANT_NOT_FOUND"));
    }

    @Test
    public void testPaymentInitiationMerchantInactive() throws Exception {
        // Create inactive merchant
        Merchant inactiveMerchant = Merchant.builder()
            .merchantId("INACTIVE_M001")
            .merchantName("Inactive Merchant")
            .saltKey("inactive_salt_key")
            .status(MerchantStatus.INACTIVE)
            .build();
        merchantRepository.save(inactiveMerchant);

        PaymentInitiateRequest request = PaymentInitiateRequest.builder()
            .merchantId("INACTIVE_M001")
            .orderId(ORDER_ID)
            .amount(AMOUNT)
            .hash("any_hash")
            .build();

        mockMvc.perform(post("/api/payment/initiate")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isUnauthorized())
            .andExpect(jsonPath("$.status").value("FAILURE"))
            .andExpect(jsonPath("$.errorCode").value("MERCHANT_NOT_FOUND"));
    }

    @Test
    public void testPaymentInitiationValidationErrors() throws Exception {
        PaymentInitiateRequest request = PaymentInitiateRequest.builder()
            .merchantId("")  // Empty
            .orderId(ORDER_ID)
            .amount(AMOUNT)
            .hash("hash")
            .build();

        mockMvc.perform(post("/api/payment/initiate")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.status").value("FAILURE"))
            .andExpect(jsonPath("$.errorCode").value("VALIDATION_FAILED"));
    }
}
