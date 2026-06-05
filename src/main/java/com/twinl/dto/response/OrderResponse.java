package com.twinl.dto.response;

import com.twinl.entity.OrderStatus;
import com.twinl.entity.PaymentMethod;
import com.twinl.entity.PaymentStatus;
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
	private String shippingWardCode;
	private Integer shippingDistrictId;
	private Integer shippingProvinceId;
	private OrderStatus status;
	private BigDecimal totalAmount;
	private PaymentMethod paymentMethod;
	private PaymentStatus paymentStatus;

	// In-house Shipper info
	private Long shipperId;
	private String shipperName;

	// Giao hàng thành công — trigger Escrow 48h
	private LocalDateTime deliveredAt;

	// Ghi chú Shipper
	private String note;

	// Split Payment & Escrow Info
	private BigDecimal platformFee;
	private BigDecimal sellerAmount;
	private String escrowStatus;

	private Set<OrderItemResponse> items;
	private LocalDateTime createdAt;
	private LocalDateTime updatedAt;
}
