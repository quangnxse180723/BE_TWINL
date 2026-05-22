package com.twinl.dto.response;

import com.twinl.entity.OrderStatus;
import com.twinl.entity.PaymentMethod;
import com.twinl.entity.PaymentStatus;
import com.twinl.entity.ShipmentStatus;
import com.twinl.entity.ShippingProvider;
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
	private PaymentMethod paymentMethod;
	private PaymentStatus paymentStatus;
	private ShippingProvider shippingProvider;
	private ShipmentStatus shippingStatus;
	private String trackingCode;
	private BigDecimal shippingFee;
	private Set<OrderItemResponse> items;
	private LocalDateTime createdAt;
	private LocalDateTime updatedAt;
}
