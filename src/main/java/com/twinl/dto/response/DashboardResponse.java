package com.twinl.dto.response;

import java.math.BigDecimal;
import java.util.List;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class DashboardResponse {
	private BigDecimal totalRevenue;
	private long totalProducts;
	private long totalUsers;
	private long totalOrders;
	private List<TopProductResponse> topProducts;
	private List<OrderSummaryResponse> recentOrders;
}
