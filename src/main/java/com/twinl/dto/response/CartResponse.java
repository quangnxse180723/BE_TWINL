package com.twinl.dto.response;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Set;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class CartResponse {
	private Long id;
	private Long userId;
	private Set<CartItemResponse> items;
	private Integer totalQuantity;
	private BigDecimal subtotal;
	private LocalDateTime createdAt;
	private LocalDateTime updatedAt;
}
