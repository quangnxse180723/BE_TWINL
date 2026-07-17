package com.twinl.dto.response;

import java.math.BigDecimal;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class UserStatsResponse {
    private BigDecimal walletBalance;
    private long totalOrdersPurchased;
    private long totalOrdersSold;
}
