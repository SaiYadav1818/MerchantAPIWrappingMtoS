package com.sabbpe.merchant.service;

import com.sabbpe.merchant.dto.PaymentInitiateRequest;
import com.sabbpe.merchant.dto.PaymentResponse;
import com.sabbpe.merchant.entity.Merchant;
import com.sabbpe.merchant.entity.MerchantStatus;
import com.sabbpe.merchant.entity.Transaction;
import com.sabbpe.merchant.exception.HashMismatchException;
import com.sabbpe.merchant.exception.MerchantNotFoundException;
import com.sabbpe.merchant.repository.MerchantRepository;
import com.sabbpe.merchant.repository.TransactionRepository;
import com.sabbpe.merchant.util.HashUtil;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class PaymentServiceTest {

    @Mock
    private MerchantRepository merchantRepository;

    @Mock
    private TransactionRepository transactionRepository;

    @InjectMocks
    private PaymentService paymentService;

    private Merchant testMerchant;
    private PaymentInitiateRequest testRequest;
    private static final String MERCHANT_ID = "M123";
    private static final String ORDER_ID = "ORD1001";
    private static final BigDecimal AMOUNT = new BigDecimal("1000.00");
    private static final String SALT_KEY = "secret_salt_key_123";

    @BeforeEach
    public void setUp() {
        testMerchant = Merchant.builder()
            .id(1L)
            .merchantId(MERCHANT_ID)
            .merchantName("Test Merchant")
            .saltKey(SALT_KEY)
            .status(MerchantStatus.ACTIVE)
            .build();

        String hashInput = MERCHANT_ID + ORDER_ID + AMOUNT + SALT_KEY;
        String correctHash = HashUtil.generateSHA256(hashInput);

        testRequest = PaymentInitiateRequest.builder()
            .merchantId(MERCHANT_ID)
            .orderId(ORDER_ID)
            .amount(AMOUNT)
            .hash(correctHash)
            .build();
    }

    @Test
    public void testInitiatePaymentSuccess() {
        // Arrange
        when(merchantRepository.findByMerchantId(MERCHANT_ID))
            .thenReturn(Optional.of(testMerchant));
        when(transactionRepository.save(any(Transaction.class)))
            .thenAnswer(invocation -> invocation.getArgument(0));

        // Act
        PaymentResponse response = paymentService.initiatePayment(testRequest);

        // Assert
        assertNotNull(response);
        assertEquals("SUCCESS", response.getStatus());
        assertEquals(ORDER_ID, response.getOrderId());
        assertNotNull(response.getInternalToken());

        // Verify interactions
        verify(merchantRepository, times(1)).findByMerchantId(MERCHANT_ID);
        verify(transactionRepository, times(1)).save(any(Transaction.class));

        // Verify transaction was saved with correct data
        ArgumentCaptor<Transaction> transactionCaptor = ArgumentCaptor.forClass(Transaction.class);
        verify(transactionRepository).save(transactionCaptor.capture());
        Transaction savedTransaction = transactionCaptor.getValue();
        assertEquals(ORDER_ID, savedTransaction.getOrderId());
        assertEquals(MERCHANT_ID, savedTransaction.getMerchantId());
        assertEquals(AMOUNT, savedTransaction.getAmount());
    }

    @Test
    public void testInitiatePaymentMerchantNotFound() {
        // Arrange
        when(merchantRepository.findByMerchantId(MERCHANT_ID))
            .thenReturn(Optional.empty());

        // Act & Assert
        assertThrows(MerchantNotFoundException.class, () -> {
            paymentService.initiatePayment(testRequest);
        });

        verify(merchantRepository, times(1)).findByMerchantId(MERCHANT_ID);
        verify(transactionRepository, never()).save(any());
    }

    @Test
    public void testInitiatePaymentMerchantInactive() {
        // Arrange
        testMerchant.setStatus(MerchantStatus.INACTIVE);
        when(merchantRepository.findByMerchantId(MERCHANT_ID))
            .thenReturn(Optional.of(testMerchant));

        // Act & Assert
        assertThrows(MerchantNotFoundException.class, () -> {
            paymentService.initiatePayment(testRequest);
        });

        verify(merchantRepository, times(1)).findByMerchantId(MERCHANT_ID);
        verify(transactionRepository, never()).save(any());
    }

    @Test
    public void testInitiatePaymentHashMismatch() {
        // Arrange
        testRequest.setHash("incorrect_hash");
        when(merchantRepository.findByMerchantId(MERCHANT_ID))
            .thenReturn(Optional.of(testMerchant));

        // Act & Assert
        assertThrows(HashMismatchException.class, () -> {
            paymentService.initiatePayment(testRequest);
        });

        verify(merchantRepository, times(1)).findByMerchantId(MERCHANT_ID);
        verify(transactionRepository, never()).save(any());
    }

    @Test
    public void testHashUtilGenerateSHA256() {
        // Test SHA-256 hash generation
        String input = "M123ORD10011000.00secret_salt_key_123";
        String hash = HashUtil.generateSHA256(input);

        assertNotNull(hash);
        assertTrue(hash.length() == 64); // SHA-256 produces 64 character hex string
        assertTrue(hash.matches("[a-f0-9]{64}")); // All lowercase hex chars
    }
}
