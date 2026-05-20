package com.twinl.dto.response;

import java.math.BigDecimal;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class OrderItemResponse {
	private Long productId;
	private String productName;
	private Integer quantity;
	private BigDecimal unitPrice;
	private BigDecimal lineTotal;
}
