package com.twinl.dto.response;

import com.twinl.entity.OrderStatus;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class OrderSummaryResponse {
	private Long id;
	private String code;
	private String customerName;
	private BigDecimal totalAmount;
	private OrderStatus status;
	private LocalDateTime createdAt;
}
