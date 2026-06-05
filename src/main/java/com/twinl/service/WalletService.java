package com.twinl.service;

import com.twinl.entity.Order;

public interface WalletService {
    void holdEscrow(Order order);
    void releaseEscrow(Order order);
    void processCommission(Order order);
    
    com.twinl.dto.response.WalletResponse getMyWallet(String username);
    void updateBankAccount(String username, com.twinl.dto.request.BankUpdateRequest request);
    com.twinl.dto.response.SellerStatisticsResponse getSellerStatistics(String username);
}
