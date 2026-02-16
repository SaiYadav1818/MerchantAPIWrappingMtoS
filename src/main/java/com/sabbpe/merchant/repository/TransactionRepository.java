package com.sabbpe.merchant.repository;

import com.sabbpe.merchant.entity.Transaction;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface TransactionRepository extends JpaRepository<Transaction, Long> {
    Optional<Transaction> findByOrderId(String orderId);
    Optional<Transaction> findByInternalToken(String internalToken);
}
