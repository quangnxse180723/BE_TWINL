package com.twinl.dto.response;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class VnpayReturnResponse {
	private String orderCode;
	private String paymentStatus;
	private String message;
}
