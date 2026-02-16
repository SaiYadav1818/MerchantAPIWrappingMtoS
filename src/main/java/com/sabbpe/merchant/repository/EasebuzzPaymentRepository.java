package com.sabbpe.merchant.repository;

import com.sabbpe.merchant.entity.EasebuzzPayment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface EasebuzzPaymentRepository extends JpaRepository<EasebuzzPayment, Long> {
    Optional<EasebuzzPayment> findByTxnId(String txnId);
    Optional<EasebuzzPayment> findByMerchantIdAndTxnId(String merchantId, String txnId);
}
