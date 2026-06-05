package com.twinl.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class WalletResponse {
    private BigDecimal balance;
    private BigDecimal escrowBalance;
    private BigDecimal totalCommission;
    private String bankName;
    private String bankAccountNumber;
    private String bankAccountName;
    private List<WalletTransactionResponse> transactions;
}
