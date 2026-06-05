package com.twinl.repository;

import com.twinl.entity.WalletTransaction;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface WalletTransactionRepository extends JpaRepository<WalletTransaction, Long> {
    List<WalletTransaction> findAllByWalletId(Long walletId);
    List<WalletTransaction> findByWalletIdOrderByCreatedAtDesc(Long walletId);
}
