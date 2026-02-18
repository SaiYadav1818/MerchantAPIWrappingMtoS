package com.sabbpe.merchant.controller;

import com.sabbpe.merchant.dto.PaymentReceiptResponse;
import com.sabbpe.merchant.service.PaymentReceiptService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * PaymentReceiptController
 * 
 * REST API endpoints for payment receipt generation.
 * 
 * Endpoints:
 * GET /api/payment/receipt/{txnid} - Get payment receipt as JSON
 * GET /api/payment/receipt/{txnid}/text - Get payment receipt as formatted text
 */
@Slf4j
@RestController
@RequestMapping("/api/payment")
@CrossOrigin(origins = "*", maxAge = 3600)
public class PaymentReceiptController {

    private final PaymentReceiptService paymentReceiptService;

    @Autowired
    public PaymentReceiptController(PaymentReceiptService paymentReceiptService) {
        this.paymentReceiptService = paymentReceiptService;
    }

    /**
     * GET /api/payment/receipt/{txnid}
     * 
     * Generate and return payment receipt as JSON
     * 
     * @param txnid Transaction ID from payment gateway
     * @return PaymentReceiptResponse with receipt details
     * 
     * Example Response:
     * {
     *   "receipt_number": "RCP-12345-1705326600000",
     *   "merchant_name": "ACME Corporation",
     *   "merchant_id": "MERCH001",
     *   "txnid": "12345",
     *   "order_id": "ORDER001",
     *   "status": "SUCCESS",
     *   "amount": 999.99,
     *   "currency": "INR",
     *   "payment_mode": "NETBANKING",
     *   "bank_name": "HDFC",
     *   "bank_reference": "BANK123",
     *   "gateway_reference": "EASE456",
     *   "customer_name": "John Doe",
     *   "customer_email": "john@example.com",
     *   "customer_phone": "9876543210",
     *   "transaction_date": "15-Jan-2024",
     *   "transaction_time": "10:30:00",
     *   "transaction_datetime": "15-Jan-2024 10:30:00"
     * }
     * 
     * Status Codes:
     * 200 - OK (receipt generated)
     * 404 - NOT FOUND (transaction not found)
     * 500 - INTERNAL SERVER ERROR
     */
    @GetMapping("/receipt/{txnid}")
    public ResponseEntity<PaymentReceiptResponse> getPaymentReceipt(
            @PathVariable String txnid) {

        log.info("API Request: GET /api/payment/receipt/{}", txnid);

        try {
            if (txnid == null || txnid.trim().isEmpty()) {
                log.warn("Invalid txnid parameter");
                return ResponseEntity.badRequest().build();
            }

            PaymentReceiptResponse receipt = paymentReceiptService.getPaymentReceipt(txnid);

            log.info("Payment receipt generated successfully - Merchant: {}, Amount: {}",
                    receipt.getMerchantId(), receipt.getAmount());

            return ResponseEntity.ok(receipt);

        } catch (com.sabbpe.merchant.exception.TransactionNotFoundException e) {
            log.warn("Transaction not found for receipt - txnid: {}", txnid);
            return ResponseEntity.notFound().build();

        } catch (com.sabbpe.merchant.exception.MerchantNotActiveException e) {
            log.warn("Merchant not active - error: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();

        } catch (Exception e) {
            log.error("Error generating payment receipt - txnid: {}, error: {}",
                    txnid, e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    /**
     * GET /api/payment/receipt/{txnid}/text
     * 
     * Generate and return payment receipt as formatted text
     * Suitable for display, print, or email
     * 
     * @param txnid Transaction ID
     * @return Formatted receipt text
     */
    @GetMapping("/receipt/{txnid}/text")
    public ResponseEntity<String> getPaymentReceiptText(
            @PathVariable String txnid) {

        log.info("API Request: GET /api/payment/receipt/{}/text", txnid);

        try {
            String receipt = paymentReceiptService.getFormattedReceipt(txnid);

            log.info("Payment receipt text generated successfully - txnid: {}", txnid);

            return ResponseEntity.ok()
                    .header("Content-Type", "text/plain; charset=UTF-8")
                    .body(receipt);

        } catch (com.sabbpe.merchant.exception.TransactionNotFoundException e) {
            log.warn("Transaction not found for receipt text - txnid: {}", txnid);
            return ResponseEntity.notFound().build();

        } catch (Exception e) {
            log.error("Error generating receipt text - txnid: {}, error: {}",
                    txnid, e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }
}
