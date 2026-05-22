package com.twinl.dto.response;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class PaymentCreateResponse {
	private Long orderId;
	private String orderCode;
	private String paymentUrl;
}
