package com.twinl.dto.response;

import com.twinl.entity.OrderStatus;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Set;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class OrderResponse {
	private Long id;
	private String code;
	private String customerName;
	private String customerEmail;
	private String customerPhone;
	private String shippingAddress;
	private OrderStatus status;
	private BigDecimal totalAmount;
	private Set<OrderItemResponse> items;
	private LocalDateTime createdAt;
	private LocalDateTime updatedAt;
}
