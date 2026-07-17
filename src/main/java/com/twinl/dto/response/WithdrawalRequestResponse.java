package com.twinl.dto.response;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class WithdrawalRequestResponse {
    private Long id;
    private String sellerName;
    private String sellerEmail;
    private BigDecimal amount;
    private String bankName;
    private String bankAccountNumber;
    private String bankAccountName;
    private LocalDateTime createdAt;
    private String status;
}
