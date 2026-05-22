package com.twinl.dto.response;

import com.twinl.entity.ShipmentStatus;
import com.twinl.entity.ShippingProvider;
import java.math.BigDecimal;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class ShipmentResponse {
	private Long id;
	private Long orderId;
	private ShippingProvider provider;
	private String trackingCode;
	private BigDecimal shippingFee;
	private ShipmentStatus status;
}
