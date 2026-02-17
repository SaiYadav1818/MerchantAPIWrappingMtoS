package com.sabbpe.merchant.controller;

import com.sabbpe.merchant.entity.Transaction;
import com.sabbpe.merchant.entity.TransactionStatus;
import com.sabbpe.merchant.repository.TransactionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Optional;

@Slf4j
@RestController
@RequestMapping("/api/easebuzz")
@RequiredArgsConstructor
public class EasebuzzCallbackController {

    private final TransactionRepository transactionRepository;

    @PostMapping("/callback")
    public ResponseEntity<String> handleCallback(
            @RequestParam("txnid") String txnid,
            @RequestParam("status") String status,
            @RequestParam("amount") String amount,
            @RequestParam("easepayid") String easepayid,
            @RequestParam("hash") String hash,
            @RequestParam("firstname") String firstname,
            @RequestParam("email") String email,
            @RequestParam("productinfo") String productinfo
    ) {
        // Convert txnid to Long for compatibility
        Long txnIdLong;
        try {
            txnIdLong = Long.valueOf(txnid);
        } catch (NumberFormatException e) {
            log.error("Invalid txnid format: {}", txnid, e);
            return ResponseEntity.badRequest().body("Invalid transaction ID format");
        }

        // Fetch the corresponding transaction
        Optional<Transaction> transactionOptional = transactionRepository.findById(txnIdLong);

        if (transactionOptional.isPresent()) {
            Transaction transaction = transactionOptional.get();

            // Update transaction status based on the callback status
            if ("success".equalsIgnoreCase(status)) {
                transaction.setStatus(TransactionStatus.COMPLETED);
            } else {
                transaction.setStatus(TransactionStatus.FAILED);
            }

            // Save the updated transaction
            transactionRepository.save(transaction);

            log.info("Transaction with txnid={} updated to status={}", txnid, transaction.getStatus());
        } else {
            log.warn("Transaction with txnid={} not found", txnid);
        }

        // Return a simple success response
        return ResponseEntity.ok("Callback processed successfully");
    }
}