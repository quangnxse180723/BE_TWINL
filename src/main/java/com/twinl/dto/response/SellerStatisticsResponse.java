package com.twinl.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SellerStatisticsResponse {
    private long totalProducts;
    private long totalOrders;
    private BigDecimal totalRevenue;
    private BigDecimal pendingEscrow;
}
